package com.samsung.smartclipboard.gemini

import android.util.Log
import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.AgentResult
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.Topic
import com.samsung.smartclipboard.domain.model.TaskSelectionType
import com.samsung.smartclipboard.gemini.GeminiUtils.contentPreview
import com.samsung.smartclipboard.gemini.GeminiUtils.escapeJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiTaskAgent @Inject constructor(
    private val geminiManager: GeminiManager
) {
    companion object {
        private const val TAG = "GeminiTaskAgent"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun analyze(topic: Topic, items: List<DataItem>, userInstruction: String?): Result<AgentResult> {
        if (items.isEmpty()) return Result.failure(IllegalArgumentException("분석할 자료가 없습니다."))

        return runCatching {
            // 1. 프롬프트 생성 및 Gemini 실행
            val rawResponse = geminiManager.run(buildPrompt(topic, items, userInstruction))
            require(rawResponse.isNotBlank()) { "Gemini 응답이 비어 있습니다. API 키 또는 네트워크 연결을 확인하세요." }

            Log.d(TAG, "Gemini raw response:\n$rawResponse")

            // 2. JSON 정제 및 파싱 준비
            val cleanJson = rawResponse.replace(Regex("```json\\s*|```\\s*"), "").trim()
            val root = json.decodeFromString<JsonObject>(cleanJson)
            val validIds = items.map { it.id }.toSet()

            // 3. 필드 파싱
            val summary = root["summary"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("summary 필드 누락")

            val keyPoints = root["keyPoints"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.content.takeIf { c -> c.isNotBlank() } } ?: emptyList()

            val sourceItemIds = root["sourceItemIds"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.content.toLongOrNull() }?.filter { it in validIds } ?: emptyList()

            val actions = root["recommendedActions"]?.jsonArray?.mapNotNull { el ->
                val obj = el.jsonObject
                val typeStr = obj["type"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val type = runCatching { TaskSelectionType.valueOf(typeStr) }.getOrNull() ?: return@mapNotNull null
                val title = obj["title"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val body = obj["body"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

                AgentActionDraft(
                    type = type,
                    confidence = obj["confidence"]?.jsonPrimitive?.content?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f,
                    reason = obj["reason"]?.jsonPrimitive?.content ?: "",
                    title = title,
                    body = body,
                    payload = obj["payload"]?.jsonObject?.toString(), // ✅ encodeToString 대신 .toString() 사용
                    sourceItemIds = obj["sourceItemIds"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content.toLongOrNull() }?.filter { it in validIds } ?: emptyList()
                )
            } ?: emptyList()

            AgentResult(topic.id, summary, keyPoints, sourceItemIds, actions)

        }.onFailure { e ->
            // 4. 예외 발생 시 로그 출력 및 실패 반환
            Log.e(TAG, "Gemini 호출/파싱 실패", e)
        }
    }

    private fun buildPrompt(topic: Topic, items: List<DataItem>, userInstruction: String?): String = """
        당신은 SmartClipboardAI의 스마트 작업 계획 및 결과 생성 Agent(비서)입니다.
        사용자가 선택한 **주제(Topic)**를 핵심 목적으로 삼고, 관련 자료(DataItems)를 근거로 활용하여 실행 가능한 후속 작업(Action) 결과를 생성하세요.

        ## 반드시 지킬 규칙 (CRITICAL)
        - 응답은 반드시 JSON object 하나만 출력한다.
        - markdown 코드 펜스(```json, ``` 등), 설명문, 주석, 인사말을 절대 포함하지 마라. JSON 외의 텍스트는 허용되지 않는다.
        - 새 DataItem을 임의로 상상하여 만들지 마라.
        - [사용 가능한 sourceItemId 목록] 외의 id를 절대 반환하지 마라.
        - 근거 없는 장소, 참석자, 수신자, 일정을 상상하지 마라. 날짜/시간은 자료에 명확히 있을 때만 ISO-8601 형식으로 추출한다.
        - title, body, reason은 한국어로 자연스럽게 작성한다.
        - body는 사용자가 바로 복사하거나 편집하여 사용할 수 있는 '완성된 결과' 수준으로 작성한다.
        - 개인정보, URL, 주소, 연락처 등 민감한 값을 불필요하게 그대로 재출력하지 말고, 필요시 [OOO] 형태로 마스킹 처리한다.
        - Action은 최소 3개에서 최대 5개까지 생성한다.
        - **가능하면 SUMMARY, CALENDAR, REMINDER, SHARE_DRAFT 각 종류별로 최소 1개 이상의 Action을 생성하라.** 자료에 근거가 있다면 모든 종류를 포함하는 것을 권장한다.
        - **주제(Topic)의 의도를 최우선으로 반영하여 Action을 생성하라.** 단순히 자료를 요약하는 것이 아니라, 주제가 요구하는 방향과 목적에 맞는 Action을 도출해야 한다.
        - summary와 keyPoints도 주제의 관점에서 자료를 해석한 내용이어야 한다.

        ## 사용 가능한 sourceItemId 목록
        ${items.joinToString(", ") { it.id.toString() }}

        ## 사용자 주제 (Topic)
        - title: ${topic.title}
        - 설명: 사용자가 "${topic.title}"이라는 주제로 ${topic.itemCount}개의 자료를 모았습니다. 이 주제의 의도와 목적을 파악하고, 그에 맞는 작업을 생성하세요.
        ${userInstruction?.takeIf { it.isNotBlank() }?.let { "- 사용자 추가 지시: $it\n" } ?: ""}
        ## 분석할 자료 목록 (${items.size}개)
        ${buildItemsJson(items)}

        ## 출력 JSON Schema
        {
          "summary": "주제와 자료의 핵심을 관통하는 요약 (2~3문장)",
          "keyPoints": ["핵심 포인트 1", "핵심 포인트 2", "핵심 포인트 3"],
          "sourceItemIds": [전체 분석에 사용된 id 배열],
          "recommendedActions": [
            {
              "type": "SUMMARY|CALENDAR|REMINDER|SHARE_DRAFT",
              "confidence": 0.95,
              "reason": "이 액션을 추천하는 이유 (한국어 1문장)",
              "title": "Action의 직관적인 짧은 제목",
              "body": "사용자가 검토하고 바로 사용할 수 있는 결과 본문",
              "payload": { ... Action Type별 구조 참조 ... },
              "sourceItemIds": [이 액션을 도출하는데 사용된 id 배열]
            }
          ]
        }

        ## Action 생성 기준 및 Payload 규칙
        - SUMMARY: 자료가 2개 이상일 때 기본 후보 (노트 정리, 리서치 요약, 회의록 등)
          → payload: {"app":"NOTES", "noteTitle":"...", "noteBody":"...", "sourceItemIds":[...], "needsUserInput":[]}
        - CALENDAR: 날짜/시간/장소 정보가 명확할 때만 (예: "내일 오후 2시 회의")
          → payload: {"app":"CALENDAR", "eventTitle":"...", "eventDescription":"...", "startTime":"2026-05-30T14:00:00+09:00", "endTime":"...", "location":null, "sourceItemIds":[...], "needsUserInput":["startTime"]} (불확실한 정보가 있다면 needsUserInput에 필드명 추가)
        - REMINDER: 마감일, 제출, 준비물, 연락 등 잊지 말아야 할 후속 행동이 있을 때
          → payload: {"app":"REMINDER", "reminderTitle":"...", "reminderBody":"...", "dueTime":"2026-05-31T09:00:00+09:00", "sourceItemIds":[...], "needsUserInput":[]}
        - SHARE_DRAFT: 누군가에게 전달할 메시지, 이메일, 보고서 작성이 자연스러울 때
          → payload: {"app":"SHARE", "shareTitle":"...", "shareText":"...", "sourceItemIds":[...], "needsUserInput":[]}
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
}