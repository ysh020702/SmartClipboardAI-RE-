package com.samsung.smartclipboard.gemini

import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.CandidateItem
import com.samsung.smartclipboard.domain.model.RetrievalPlan
import com.samsung.smartclipboard.domain.model.TaskSelectionType
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
            val prompt = buildPrompt(topicQuery, plan, targetItems, targetActions, feedback)
            val actions = parseActions(geminiManager.run(prompt), targetItems)

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
        사용자 피드백에 맞게 작업 후보를 보완해라.

        ## 핵심 규칙 (가장 중요)
        - '현재 작업 후보'의 title과 body가 사용자가 보고 있는 **가장 최신 버전의 텍스트**다. 
        - 무조건 이 '현재 작업 후보'의 내용을 베이스로 하여 피드백을 적용해라.
        - 원본 DataItem을 보고 글을 처음부터 완전히 새로 작성(초기화)하지 마라. 기존 문맥과 내용을 최대한 유지하면서 요청받은 부분만 보완해라.

        ## 언어 유지 및 번역 규칙 (★ 매우 중요)
        - **언어 상태 유지**: 피드백에 명시적인 번역 요청(예: "영어로 번역", "Translate")이 없다면, **무조건 '현재 작업 후보'에 작성되어 있는 언어(한국어, 영어, 일본어 등)를 그대로 유지**해라. 
          (예: 현재 제목과 본문이 영어 or 일본어 상태인데 "더 간결하게"라는 피드백을 받았다면, 한국어로 바꾸지 말고 영어 or 일본어 상태 그대로 문장을 간결하게 다듬어야 한다.)
        - **번역 요청인 경우만 변경**: 피드백이 특정 언어로의 번역을 명시적으로 요구하는 경우에만 해당 언어로 전체 텍스트를 전환해라. 
        - 단, AI의 작업 이유를 나타내는 `reason` 필드는 사용자의 이해를 위해 어떤 상황이든 항상 **한국어 1문장**으로 작성해라.

        ## 반드시 지킬 규칙
        - 응답은 반드시 JSON object 하나만 출력한다.
        - markdown 코드 펜스, 설명문, 주석을 절대 포함하지 마라.
        - 새 DataItem을 만들지 마라.
        - 아래 sourceItemId 목록 외의 id를 절대 반환하지 마라.
        - body는 사용자가 편집 가능한 결과 수준으로 작성해라.
        - 개인정보, URL, 주소, 연락처 등 민감한 값을 불필요하게 그대로 재출력하지 마라.

        ## 퀵 액션 피드백 처리 규칙
        - "더 간결하게" / "짧게": 현재 body를 절반 이하로 줄이고, 불필요한 설명·수식어를 제거하라. 핵심 문장만 남겨라. (현재 언어 유지)
        - "핵심만 요약": 현재 body를 3~5개의 핵심 포인트로만 재구성하라. 배경 설명은 빼라. (현재 언어 유지)
        - "제목 바꿔줘": title을 더 직관적이고 이해하기 쉬운 표현으로 바꿔라. body는 그대로 유지하라. (현재 언어 유지)
        - "영어로 번역": 현재 title과 body를 모두 자연스러운 영어로 번역하라.

        ## 사용 가능한 sourceItemId 목록
        ${items.joinToString(", ") { it.item.id.toString() }}

        ## 사용 가능한 action type
        ${TaskSelectionType.entries.joinToString(", ") { it.name }}

        ## 출력 JSON schema
        {
          "actions": [
            {
              "type": "SUMMARY",
              "confidence": 0.86,
              "reason": "사용자 피드백에 따라 기존 내용을 보완했습니다.",
              "title": "Action Title",
              "body": "Action Body content...",
              "payload": {},
              "sourceItemIds": [1, 2, 3]
            }
          ]
        }

        ## 필드 규칙
        - actions: 1~5개
        - type: 위 type 목록 중 하나만 사용
        - confidence: 0.0~1.0
        - reason: 무조건 한국어 1문장 (작업 수행 이유 설명)
        - title: 피드백 및 현재 언어(컨텍스트)가 반영된 짧은 제목
        - body: 피드백 및 현재 언어(컨텍스트)가 반영된 편집 가능한 결과
        - payload: JSON object, 없으면 {}
        - sourceItemIds: 위 목록에 있는 id만 사용

        ## 사용자 주제: $topicQuery
        ## 검색 키워드: ${plan.keywords.joinToString(", ").ifBlank { "없음" }}
        ## 사용자 피드백
        $feedback

        ## 선택된 아이템 (${items.size}개) - 참고용 원본 데이터
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

        ## 현재 작업 후보 (★ 이 내용의 언어와 본문을 베이스로 수정할 것)
        ${actions.joinToString(",\n", "[\n", "\n]") { a -> """
          {
            "type": "${a.type.name}",
            "confidence": ${a.confidence},
            "reason": "${escapeJson(a.reason)}",
            "title": "${escapeJson(a.title)}",
            "body": "${escapeJson(a.body.take(4000))}",
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
            val type = runCatching { TaskSelectionType.valueOf(typeRaw) }.getOrNull() ?: return@mapNotNull null

            val confidence = actionObj["confidence"]?.jsonPrimitive?.content?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f
            val reason = actionObj["reason"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() } ?: "선택된 아이템을 기반으로 생성된 작업 후보입니다."
            val title = actionObj["title"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }?.take(100) ?: "${type.name} 작업"
            val body = actionObj["body"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() } ?: "선택된 데이터를 기반으로 한 결과입니다."

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