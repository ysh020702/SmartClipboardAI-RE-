package com.samsung.smartclipboard.gemini

import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.gemini.GeminiUtils.contentPreview
import com.samsung.smartclipboard.gemini.GeminiUtils.escapeJson
import com.samsung.smartclipboard.gemini.GeminiUtils.extractJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GeminiPurposeAnalyzer(
    private val geminiManager: GeminiManager
) {

    companion object {
        private const val TAG = "GeminiPurposeAnalyzer"
        private const val BATCH_SIZE = 20
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun analyze(items: List<DataItem>): Result<List<AnalyzedPurpose>> {
        if (items.isEmpty()) return Result.success(emptyList())

        // chunked로 나눈 배치를 flatMap으로 순회하며 성공한 결과만 하나의 리스트로 병합합니다.
        val allResults = items.chunked(BATCH_SIZE).flatMap { batch ->
            analyzeBatch(batch).getOrDefault(emptyList())
        }

        return if (allResults.isEmpty()) {
            Result.failure(IllegalStateException("모든 purpose 분석이 실패했습니다"))
        } else {
            Result.success(allResults)
        }
    }

    private suspend fun analyzeBatch(batch: List<DataItem>): Result<List<AnalyzedPurpose>> {
        val validIds = batch.map { it.id }.toSet()

        return runCatching {
            // 1. Gemini 실행 및 파싱 시도
            val rawResponse = geminiManager.run(buildPrompt(batch))
            val parsedResult = parsePurposes(rawResponse, validIds).getOrThrow()

            // 2. 검증 로직 (실패 시 예외 발생 -> 자동 폴백 유도)
            require(parsedResult.all { it.itemId in validIds && it.purpose.isNotBlank() && it.purposeKeyword.isNotBlank() }) {
                "Purpose 검증 실패"
            }
            parsedResult

        }
    }

    private fun buildPrompt(items: List<DataItem>): String = """
        너는 Android 앱에서 사용자의 수집 데이터를 분석하는 비서다.
        각 데이터 항목에 대해, 사용자가 이 정보를 왜 찾았을지 그 '목적'을 추론해라.

        ## 목적(purpose)의 정의
        - 글쓴이나 정보가 만들어진 목적이 아니다.
        - 이 정보를 찾은 사람(사용자)이 왜 이 정보를 찾았을까를 추론하는 것이다.
        - 예: '회의 준비를 위해 관련 자료를 조사함', '여행 일정 계획을 위해 항공권 정보를 찾음',
          '프로젝트 참고용으로 기술 문서를 수집함', '쇼핑 비교를 위해 가격 정보를 확인함'

        ## 반드시 지킬 규칙
        - 응답은 반드시 JSON object 하나만 출력한다.
        - markdown 코드 펜스, 설명문, 주석을 절대 포함하지 마라.
        - 새 id를 생성하지 마라. 입력된 id만 사용.
        - 한국어로 생성해라.
        - 개인정보, URL, 주소, 연락처 등 민감한 값을 불필요하게 그대로 재출력하지 마라.

        ## 출력 JSON schema
        {
          "items": [
            {
              "id": 1,
              "purpose": "회의 준비를 위해 관련 자료를 조사함",
              "purposeKeyword": "회의,준비,자료,조사"
            }
          ]
        }

        ## 필드 규칙
        - id: 입력된 아이템 id 중 하나만 사용
        - purpose: 한국어 문장 형태, 10~80자, '함/임/위함' 등으로 끝나는 목적 서술
        - purposeKeyword: purpose에서 추출한 핵심 키워드 3~7개, 콤마로 구분
          - 목적을 대표하는 명사/동명사 위주
          - 예: '회의,준비,자료조사', '여행,일정,계획,항공권', '쇼핑,가격비교'

        ## 데이터 항목 목록
        ${buildItemsJson(items)}
    """.trimIndent()

    private fun buildItemsJson(items: List<DataItem>): String {
        return items.joinToString(",\n", "[\n", "\n]") { item ->
            """
              {
                "id": ${item.id},
                "type": "${item.type.name}",
                "title": ${item.title?.let { "\"${escapeJson(it)}\"" }},
                "source": ${item.source?.let { "\"${escapeJson(it)}\"" }},
                "contentPreview": "${escapeJson(contentPreview(item, 1000))}",
                "createdAt": ${item.createdAt}
              }
            """.trimIndent()
        }
    }

    fun parsePurposes(raw: String, validIds: Set<Long>): Result<List<AnalyzedPurpose>> = runCatching {
        val jsonText = extractJsonObject(raw) ?: throw IllegalArgumentException("유효한 JSON을 찾을 수 없습니다.")
        val obj = json.parseToJsonElement(jsonText).jsonObject

        val arr = obj["items"] as? JsonArray ?: throw IllegalArgumentException("items 배열이 없습니다")
        val result = arr.mapNotNull { parseItem(it.jsonObject, validIds) }.distinctBy { it.itemId }

        require(result.isNotEmpty()) { "유효한 purpose 분석 결과가 없습니다" }
        result
    }

    private fun parseItem(obj: JsonObject, validIds: Set<Long>): AnalyzedPurpose? {
        val id = obj["id"]?.jsonPrimitive?.content?.toLongOrNull()?.takeIf { it in validIds } ?: return null
        val purpose = obj["purpose"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val purposeKeyword = obj["purposeKeyword"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }
            ?: extractKeywordsFromPurpose(purpose)

        return AnalyzedPurpose(
            itemId = id,
            purpose = purpose.take(200),
            purposeKeyword = purposeKeyword.take(200)
        )
    }

    private fun extractKeywordsFromPurpose(purpose: String): String {
        return purpose
            .replace(Regex("[은는이가을를에의과와도에서로하]$"), "")
            .split(Regex("[\\s,·]+"))
            .filter { it.length >= 2 }
            .take(7)
            .joinToString(",")
    }

    data class AnalyzedPurpose(
        val itemId: Long,
        val purpose: String,
        val purposeKeyword: String
    )
}