package com.samsung.smartclipboard.gemini


import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.CandidateItem
import com.samsung.smartclipboard.domain.model.RetrievalPlan
import com.samsung.smartclipboard.domain.model.TopicActionType
import com.samsung.smartclipboard.gemini.GeminiUtils.contentPreview
import com.samsung.smartclipboard.gemini.GeminiUtils.escapeJson
import com.samsung.smartclipboard.gemini.GeminiUtils.extractJsonObject
import com.samsung.smartclipboard.gemini.GeminiUtils.formatDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GeminiRefineAgent(
    private val geminiManager: GeminiManager
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun refineActions(
        topicQuery: String,
        plan: RetrievalPlan,
        selectedItems: List<CandidateItem>,
        currentActions: List<AgentActionDraft>,
        feedback: String
    ): Result<List<AgentActionDraft>> {
        if (topicQuery.isBlank() || selectedItems.isEmpty() || currentActions.isEmpty() || feedback.isBlank()) {
            return Result.failure(IllegalArgumentException("필수 파라미터가 누락되었거나 비어 있습니다."))
        }

        val targetItems = selectedItems.take(10)
        val targetActions = currentActions.take(5)

        return runCatching {
            // 1. 프롬프트 생성 및 Gemini 실행, 그리고 파싱
            val prompt = buildPrompt(topicQuery, plan, targetItems, targetActions, feedback)
            val actions = parseActions(geminiManager.run(prompt), targetItems)

            // 2. 결과 검증 (중복 선언되어 있던 validate 로직을 require 블록 하나로 통합)
            val selectedIdSet = targetItems.map { it.item.id }.toSet()
            require(actions.size in 1..5 && actions.all { action ->
                action.confidence in 0.0f..1.0f &&
                        action.title.isNotBlank() &&
                        action.body.isNotBlank() &&
                        action.sourceItemIds.isNotEmpty() &&
                        action.sourceItemIds.all { it in selectedIdSet }
            }) { "Action 검증 실패" }

            actions
        }
    }

    private fun buildPrompt(
        topicQuery: String, plan: RetrievalPlan, items: List<CandidateItem>, actions: List<AgentActionDraft>, feedback: String
    ): String = """
        너는 Android 앱의 작업 계획을 보완하는 비서다.
        사용자가 이미 선택한 DataItem과 현재 ActionDraft 후보를 보고,
        사용자 피드백에 맞게 작업 후보를 보완하거나 재정렬해라.

        ## 반드시 지킬 규칙
        - 응답은 반드시 JSON object 하나만 출력한다.
        - markdown 코드 펜스, 설명문, 주석을 절대 포함하지 마라.
        - 새 DataItem을 만들지 마라.
        - 아래 sourceItemId 목록 외의 id를 절대 반환하지 마라.
        - Android Intent나 도구 실행을 직접 제안하지 말고 앱 내부 ActionDraft 후보만 생성해라.
        - title, body, reason은 한국어로 작성해라. (단, 번역 요청은 제외)
        - body는 사용자가 편집 가능한 초안 수준으로 작성해라.
        - 기존 action을 완전히 버리기보다 피드백을 반영해 보완/재정렬해라.
        - 개인정보, URL, 주소, 연락처 등 민감한 값을 불필요하게 그대로 재출력하지 마라.

        ## 퀵 액션 피드백 처리 규칙
        - "더 간결하게" / "짧게": body를 절반 이하로 줄이고, 불필요한 설명·수식어를 제거하라. 핵심 문장만 남겨라.
        - "핵심만 요약": body를 3~5개의 핵심 포인트로만 재구성하라. 배경 설명은 빼라.
        - "제목 바꿔줘": title을 더 직관적이고 이해하기 쉬운 표현으로 바꿔라. body는 그대로 유지하라.
        - "영어로 번역": title과 body를 모두 자연스러운 영어로 번역하라. reason은 한국어로 유지하라.

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
              "reason": "사용자 피드백에 따라 더 짧은 요약 중심으로 보완했습니다.",
              "title": "선택한 자료 짧게 요약",
              "body": "선택된 자료의 핵심만 간단히 정리합니다.",
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
        - body: 편집 가능한 초안
        - payload: JSON object, 없으면 {}
        - sourceItemIds: 위 목록에 있는 id만 사용

        ## 사용자 주제: $topicQuery
        ## 검색 키워드: ${plan.keywords.joinToString(", ").ifBlank { "없음" }}
        ## 사용자 피드백
        ${feedback.take(1000)}

        ## 선택된 아이템 (${items.size}개)
        ${items.joinToString(",\n", "[\n", "\n]") { c -> """
          {
            "id": ${c.item.id},
            "type": "${c.item.type.name}",
            "title": ${c.item.title?.let { "\"${escapeJson(it)}\"" }},
            "source": ${c.item.source?.let { "\"${escapeJson(it)}\"" }},
            "contentPreview": "${escapeJson(contentPreview(c.item, 1000))}",
            "createdAt": "${formatDate(c.item.createdAt)}",
            "relevanceScore": ${c.relevanceScore},
            "relevanceReason": "${escapeJson(c.relevanceReason)}"
          }
        """.trimIndent() }}

        ## 현재 작업 후보
        ${actions.joinToString(",\n", "[\n", "\n]") { a -> """
          {
            "type": "${a.type.name}",
            "confidence": ${a.confidence},
            "reason": "${escapeJson(a.reason)}",
            "title": "${escapeJson(a.title)}",
            "body": "${escapeJson(a.body.take(500))}",
            "sourceItemIds": [${a.sourceItemIds.joinToString()}]
          }
        """.trimIndent() }}
    """.trimIndent()

    private fun parseActions(raw: String, selectedItems: List<CandidateItem>): List<AgentActionDraft> {
        val jsonText = extractJsonObject(raw) ?: throw IllegalArgumentException("유효한 JSON을 찾을 수 없습니다.")
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

            AgentActionDraft(
                type = type,
                confidence = confidence,
                reason = reason,
                title = title,
                body = body,
                payload = null,
                sourceItemIds = parsedSourceIds.ifEmpty { selectedIdSet.take(3).toList() }
            )
        }.distinctBy { it.type to it.title }.take(5)

        require(drafts.isNotEmpty()) { "유효한 action이 하나도 없습니다" }
        return drafts
    }
}
