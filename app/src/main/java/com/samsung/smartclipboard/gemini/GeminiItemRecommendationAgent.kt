package com.samsung.smartclipboard.gemini

import com.samsung.smartclipboard.domain.model.CandidateItem
import com.samsung.smartclipboard.domain.model.ItemRecommendationResult
import com.samsung.smartclipboard.domain.model.RetrievalPlan
import com.samsung.smartclipboard.gemini.GeminiUtils.contentPreview
import com.samsung.smartclipboard.gemini.GeminiUtils.escapeJson
import com.samsung.smartclipboard.gemini.GeminiUtils.formatDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GeminiItemRecommendationAgent(
    private val geminiManager: GeminiManager
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun recommend(
        topicQuery: String,
        plan: RetrievalPlan,
        candidates: List<CandidateItem>
    ): Result<ItemRecommendationResult> {
        if (topicQuery.isBlank()) return Result.failure(IllegalArgumentException("주제가 비어 있습니다"))
        if (candidates.isEmpty()) return Result.failure(IllegalArgumentException("아이템이 비어 있습니다"))

        val targetCandidates = candidates.take(20)

        return runCatching {
            // 1. 프롬프트 생성 및 Gemini 실행, 파싱
            val prompt = buildPrompt(topicQuery, plan, targetCandidates)
            val result = parseItemRecommendation(geminiManager.run(prompt), targetCandidates)

            // 2. 검증 로직 (실패 시 예외를 던져 자동으로 폴백 유도)
            val validIds = targetCandidates.map { it.item.id }.toSet()
            val recommendedIds = result.recommendedItems.map { it.item.id }.toSet()

            require(result.recommendedItems.size <= 10 && result.recommendedItems.all { it.item.id in validIds }) { "추천 아이템 검증 실패" }
            require(result.selectedItemIds.size <= 5 && result.selectedItemIds.all { it in recommendedIds }) { "선택 아이템 검증 실패" }
            require(result.recommendedItems.all { it.relevanceScore in 0.0f..1.0f }) { "점수 범위 검증 실패" }
            require(result.recommendationReason.isNotBlank()) { "추천 이유 누락" }

            result
        }
    }

    private fun buildPrompt(topicQuery: String, plan: RetrievalPlan, candidates: List<CandidateItem>): String = """
        너는 Android 로컬 데이터 기반 아이템 추천 비서다.
        사용자의 주제와 검색 계획, 후보 아이템 목록을 보고 추천할 아이템을 선택해라.

        ## 반드시 지킬 규칙
        - 응답은 반드시 JSON object 하나만 출력한다.
        - markdown 코드 펜스(```), 설명문, 주석을 절대 포함하지 마라.
        - JSON object 외에 다른 텍스트를 출력하지 마라.
        - 아래 후보에 없는 itemId를 절대 반환하지 마라.
        - 새 itemId를 생성하지 마라.
        - item content를 그대로 복사하지 말고 추천 이유만 요약해라.
        - 개인정보, 연락처, 주소, URL 등 민감할 수 있는 값을 불필요하게 재출력하지 마라.
        - itemId는 절대 번역하지 마라.
        - 추천 이유는 한국어로 작성해라.

        ## 사용 가능한 itemId 목록
        ${candidates.joinToString(", ") { it.item.id.toString() }}

        ## 출력 JSON schema
        {
          "selectedItemIds": [1, 2, 3],
          "itemReasons": [
            {
              "itemId": 1,
              "score": 0.92,
              "reason": "주제의 핵심 키워드와 가장 직접적으로 관련됩니다."
            }
          ],
          "recommendationReason": "최근 데이터와 주제 관련도를 기준으로 3개를 우선 선택했습니다.",
          "suggestedQueries": ["추가 검색어1"]
        }

        ## 필드 규칙
        - selectedItemIds: 위 목록에 존재하는 itemId만 사용, Long 숫자 배열, 0~5개
        - itemReasons: 추천한 각 아이템 별 점수와 이유, 존재하는 itemId만 사용
          - score: 0.0~1.0
          - reason: 1~2문장의 한국어 추천 이유
          - 최대 10개
        - recommendationReason: 전체 추천에 대한 한국어 설명 1~3문장
        - suggestedQueries: 추가 검색어 제안, 0~5개, 빈 문자열 금지

        ## 사용자 주제
        $topicQuery

        ## 검색 계획
        키워드: ${plan.keywords.joinToString(", ").ifBlank { "없음" }}
        타입 필터: ${plan.typeFilters.joinToString(", ") { it.name }.ifBlank { "없음" }}
        날짜 범위: ${plan.dateRangeDays?.let { "${it}일" } ?: "없음"}
        최대 결과: ${plan.maxResults}

        ## 후보 아이템 목록 (${candidates.size}개)
        ${candidates.joinToString(",\n", "[\n", "\n]") { c ->
        """
              {
                "id": ${c.item.id},
                "type": "${c.item.type.name}",
                "title": ${c.item.title?.let { "\"${escapeJson(it)}\"" }},
                "source": ${c.item.source?.let { "\"${escapeJson(it)}\"" }},
                "mimeType": ${c.item.mimeType?.let { "\"${escapeJson(it)}\"" }},
                "contentPreview": "${escapeJson(contentPreview(c.item, 1000)
            )
        }",
                "createdAt": "${formatDate(c.item.createdAt)}",
                "localScore": ${c.relevanceScore},
                "localReason": "${escapeJson(c.relevanceReason)}"
              }
            """.trimIndent()
    }}
    """.trimIndent()

    private fun parseItemRecommendation(raw: String, candidates: List<CandidateItem>): ItemRecommendationResult {
        val jsonText = GeminiUtils.extractJsonObject(raw) ?: throw IllegalArgumentException("유효한 JSON을 찾을 수 없습니다.")
        val obj = json.parseToJsonElement(jsonText).jsonObject
        val candidateById = candidates.associateBy { it.item.id }

        fun parseId(el: JsonElement?) = el?.jsonPrimitive?.content?.let { it.toLongOrNull() ?: it.toDoubleOrNull()?.toLong() }

        // 1. 추천 아이템 사유 파싱 및 업데이트
        val parsedItemReasons = (obj["itemReasons"] as? JsonArray)?.mapNotNull { el ->
            val itemObj = el.jsonObject
            val id = parseId(itemObj["itemId"]) ?: return@mapNotNull null
            val candidate = candidateById[id] ?: return@mapNotNull null

            val score = itemObj["score"]?.jsonPrimitive?.content?.toFloatOrNull() ?: candidate.relevanceScore
            val reason = itemObj["reason"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() } ?: candidate.relevanceReason

            candidate.copy(relevanceScore = score.coerceIn(0f, 1f), relevanceReason = reason)
        }?.take(10) ?: emptyList()

        // 2. 추천 아이템 목록 완성 (파싱된 것 우선 + 부족하면 점수 높은 순 채우기)
        val parsedIds = parsedItemReasons.map { it.item.id }.toSet()
        val recommendedItems = (parsedItemReasons + candidates.filter { it.item.id !in parsedIds }.sortedByDescending { it.relevanceScore }).take(10)
        val recommendedIdsSet = recommendedItems.map { it.item.id }.toSet()

        // 3. 선택된 아이템 파싱
        val parsedSelectedIds = (obj["selectedItemIds"] as? JsonArray)?.mapNotNull { parseId(it) } ?: emptyList()
        val finalSelectedIds = (if (parsedSelectedIds.isNotEmpty()) parsedSelectedIds.filter { it in recommendedIdsSet }
        else recommendedItems.take(3).map { it.item.id }).take(5).toSet()

        // 4. 나머지 필드 파싱
        val recommendationReason = obj["recommendationReason"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }
            ?: "주제와 후보 아이템의 관련도를 기준으로 추천했습니다."

        val suggestedQueries = (obj["suggestedQueries"] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.content.takeIf { c -> c != "null" && c.isNotBlank() }?.trim() }
            ?.distinct()?.take(5) ?: emptyList()

        return ItemRecommendationResult(
            recommendedItems = recommendedItems,
            selectedItemIds = finalSelectedIds,
            recommendationReason = recommendationReason,
            suggestedQueries = suggestedQueries
        )
    }
}