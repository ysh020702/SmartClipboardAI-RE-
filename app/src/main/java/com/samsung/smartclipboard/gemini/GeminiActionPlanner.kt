package com.samsung.smartclipboard.gemini

import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.CandidateItem
import com.samsung.smartclipboard.domain.model.RetrievalPlan
import com.samsung.smartclipboard.domain.model.TopicActionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GeminiActionPlanner(
    private val geminiManager: GeminiManager
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun planActions(
        topicQuery: String,
        plan: RetrievalPlan,
        selectedItems: List<CandidateItem>
    ): Result<List<AgentActionDraft>> {
        if (topicQuery.isBlank() || selectedItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("주제나 선택된 아이템이 비어 있습니다"))
        }

        val targetItems = selectedItems.take(10)

        return runCatching {
            // 1. 프롬프트 생성 및 Gemini 실행, 파싱
            val prompt = buildPrompt(topicQuery, plan, targetItems)
            val actions = parseActions(geminiManager.run(prompt), targetItems).getOrThrow()

            // 2. 결과 검증 (중복 선언되어 있던 validateActions 로직을 require로 통합)
            val selectedIdSet = targetItems.map { it.item.id }.toSet()
            require(actions.size in 1..5) { "Action 갯수 범위 오류" }
            require(actions.all { action ->
                action.confidence in 0.0f..1.0f &&
                        action.title.isNotBlank() &&
                        action.body.isNotBlank() &&
                        action.sourceItemIds.isNotEmpty() &&
                        action.sourceItemIds.all { it in selectedIdSet }
            }) { "Action 필드 검증 실패" }

            actions
        }
    }

    private fun buildPrompt(topicQuery: String, plan: RetrievalPlan, items: List<CandidateItem>): String = """
        너는 Android 앱의 작업 계획 비서다.
        사용자가 선택한 DataItem 목록을 보고 가능한 작업 후보(ActionDraft)를 생성해라.

        ## 반드시 지킬 규칙
        - 응답은 반드시 JSON object 하나만 출력한다.
        - markdown 코드 펜스(```), 설명문, 주석을 절대 포함하지 마라.
        - JSON object 외에 다른 텍스트를 출력하지 마라.
        - 새 DataItem을 만들지 마라.
        - 아래 sourceItemId 목록 외의 id를 절대 반환하지 마라.
        - Android Intent나 도구 실행을 직접 제안하지 말고, 앱 내부 ActionDraft 후보만 생성해라.
        - title, body, reason은 한국어로 작성해라.
        - body는 사용자가 편집 가능한 초안 수준으로 작성해라.
        - 개인정보, URL, 주소, 연락처 등 민감한 값을 불필요하게 그대로 재출력하지 마라.

        ## 사용 가능한 sourceItemId 목록
        ${items.joinToString(", ") { it.item.id.toString() }}

        ## 사용 가능한 action type
        ${TopicActionType.entries.joinToString(", ") { it.name }}

        ## 출력 JSON schema
        {
          "actions": [
            {
              "type": "SUMMARY",
              "confidence": 0.86,
              "reason": "선택된 아이템들의 핵심을 요약합니다.",
              "title": "선택한 자료 요약",
              "body": "선택된 자료의 핵심 내용을 정리한 요약 초안입니다.",
              "payload": {},
              "sourceItemIds": [1, 2, 3]
            }
          ]
        }

        ## 필드 규칙
        - actions: 1~5개
        - type: 위 type 목록 중 하나만 사용
        - confidence: 0.0~1.0
        - reason: 한국어 1문장
        - title: 한국어 짧은 제목
        - body: 사용자가 편집 가능한 초안
        - payload: JSON object, 없으면 {}
        - sourceItemIds: 위 sourceItemId 목록에 있는 id만 사용, 비어 있으면 전체 사용

        ## 사용자 주제
        $topicQuery

        ## 검색 계획
        키워드: ${plan.keywords.joinToString(", ").ifBlank { "없음" }}

        ## 선택된 아이템 목록 (${items.size}개)
        ${items.joinToString(",\n", "[\n", "\n]") { c ->
        """
              {
                "id": ${c.item.id},
                "type": "${c.item.type.name}",
                "title": ${c.item.title?.let { "\"${GeminiUtils.escapeJson(it)}\"" }},
                "source": ${c.item.source?.let { "\"${GeminiUtils.escapeJson(it)}\"" }},
                "contentPreview": "${GeminiUtils.escapeJson(GeminiUtils.contentPreview(c.item, 1000))}",
                "createdAt": "${GeminiUtils.formatDate(c.item.createdAt)}",
                "relevanceScore": ${c.relevanceScore},
                "relevanceReason": "${GeminiUtils.escapeJson(c.relevanceReason)}"
              }
            """.trimIndent()
    }}
    """.trimIndent()

    private fun parseActions(raw: String, selectedItems: List<CandidateItem>): Result<List<AgentActionDraft>> = runCatching {
        val jsonText = GeminiUtils.extractJsonObject(raw) ?: throw IllegalArgumentException("유효한 JSON을 찾을 수 없습니다.")
        val obj = json.parseToJsonElement(jsonText).jsonObject
        val selectedIdSet = selectedItems.map { it.item.id }.toSet()

        val actionsArray = obj["actions"] as? JsonArray ?: throw IllegalArgumentException("actions 배열이 없습니다")

        val drafts = actionsArray.mapNotNull { element ->
            val actionObj = element.jsonObject

            val typeRaw = actionObj["type"]?.jsonPrimitive?.content?.trim()?.uppercase() ?: return@mapNotNull null
            val type = runCatching { TopicActionType.valueOf(typeRaw) }.getOrNull() ?: return@mapNotNull null

            val confidence = actionObj["confidence"]?.jsonPrimitive?.content?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f
            val reason = actionObj["reason"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() } ?: "선택된 아이템을 기반으로 생성된 작업 후보입니다."
            val title = actionObj["title"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }?.take(100) ?: "${type.name} 작업"
            val body = actionObj["body"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() } ?: "선택된 데이터를 기반으로 한 초안입니다."

            val parsedSourceIds = (actionObj["sourceItemIds"] as? JsonArray)
                ?.mapNotNull { it.jsonPrimitive.content.toLongOrNull() ?: it.jsonPrimitive.content.toDoubleOrNull()?.toLong() }
                ?.filter { it in selectedIdSet }
                ?.distinct() ?: emptyList()

            val finalSourceIds = parsedSourceIds.ifEmpty { selectedIdSet.take(3).toList() }

            AgentActionDraft(
                type = type,
                confidence = confidence,
                reason = reason,
                title = title,
                body = body,
                payload = null,
                sourceItemIds = finalSourceIds
            )
        }.distinctBy { it.type to it.title }.take(5)

        require(drafts.isNotEmpty()) { "유효한 action이 하나도 없습니다" }
        drafts
    }
}
