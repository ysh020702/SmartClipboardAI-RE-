package com.samsung.smartclipboard.gemini

import android.util.Log
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.gemini.GeminiUtils.escapeJson
import com.samsung.smartclipboard.gemini.GeminiUtils.extractJsonObject
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자가 입력한 주제와 데이터 항목들의 purpose를 분석하여
 * 해당 주제에 관련된 데이터 ID 목록을 반환하는 Gemini 에이전트.
 */
@Singleton
class GeminiFindData @Inject constructor(
    private val geminiManager: GeminiManager,
) {
    companion object {
        private const val TAG = "GeminiFindData"
        private const val MAX_ITEMS = 50
    }

    /**
     * 주제와 데이터 항목들을 분석하여 관련성 있는 항목의 ID 목록을 반환합니다.
     *
     * @param topicTitle 사용자가 입력한 주제
     * @param items 분석할 데이터 항목 목록
     * @return 주제와 관련된 데이터 항목의 ID 집합. 실패 시 빈 Set 반환.
     */
    suspend fun findRelevantItems(topicTitle: String, items: List<DataItem>): Set<Long> {
        if (items.isEmpty()) return emptySet()

        val limited = items.take(MAX_ITEMS)

        return try {
            val prompt = buildPrompt(topicTitle, limited)
            val raw = geminiManager.run(prompt)

            val result = parse(raw)
            if (result == null) {
                Log.w(TAG, "Gemini 응답 파싱 실패, 추천 항목 없음")
                return emptySet()
            }

            // 유효한 ID만 필터링
            val validIds = limited.map { it.id }.toSet()
            result.itemIds.intersect(validIds)
        } catch (e: Exception) {
            Log.e(TAG, "GeminiFindData 실행 실패", e)
            emptySet()
        }
    }

    private fun buildPrompt(topicTitle: String, items: List<DataItem>): String {
        return """
            당신은 데이터 분석 전문가입니다.
            사용자가 입력한 주제와 아래 데이터 항목들을 분석하여, 해당 주제와 관련성이 높은 데이터를 찾아주세요.

            ## 핵심 기준
            1. 각 데이터 항목의 purpose(목적)와 내용을 종합적으로 고려하세요.
            2. 주제와 직접적으로 관련된 항목뿐만 아니라, 간접적으로도 도움이 될 수 있는 항목도 포함하세요.
            3. 관련성이 낮거나 무관한 항목은 제외하세요.
            4. 주제가 포괄적인 경우(예: "여행 계획") 관련 범주의 항목들을 넓게 포함하세요.
            5. 주제가 구체적인 경우(예: "제주도 숙소 예약") 정확히 일치하는 항목만 포함하세요.

            ## 일반 규칙
            - 응답은 반드시 JSON object 하나만 출력한다.
            - markdown 코드 펜스, 설명문, 주석을 절대 포함하지 마라.
            - 관련 항목이 없으면 빈 배열을 반환한다.

            ## 출력 JSON schema
            {
              "relevant_ids": [1, 3, 5, ...]
            }

            ## 필드 규칙
            - relevant_ids: 주제와 관련된 데이터 항목의 id 배열. 입력 데이터의 id 필드와 정확히 일치해야 한다.

            ## 사용자 주제
            "${escapeJson(topicTitle)}"

            ## 입력 데이터 (${items.size}개)
            ${buildItemsString(items)}
        """.trimIndent()
    }

    private fun buildItemsString(items: List<DataItem>): String {
        return items.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { item ->
            """
              {
                "id": ${item.id},
                "type": "${item.type.name}",
                "title": ${item.title?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "purpose": ${item.purpose?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "content_preview": "${escapeJson(item.effectiveContent.take(200))}"
              }
            """.trimIndent()
        }
    }

    private data class FindDataResult(
        val itemIds: Set<Long>,
    )

    private fun parse(raw: String): FindDataResult? {
        return try {
            val jsonText = extractJsonObject(raw) ?: return null
            val json = JSONObject(jsonText)

            val idsArray = json.optJSONArray("relevant_ids") ?: return FindDataResult(emptySet())
            val ids = mutableSetOf<Long>()
            for (i in 0 until idsArray.length()) {
                ids.add(idsArray.getLong(i))
            }

            FindDataResult(itemIds = ids)
        } catch (e: Exception) {
            Log.e(TAG, "파싱 실패", e)
            null
        }
    }
}