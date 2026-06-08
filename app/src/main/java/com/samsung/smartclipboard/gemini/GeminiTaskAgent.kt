package com.samsung.smartclipboard.gemini

import android.util.Log
import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.AgentResult
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.Topic
import com.samsung.smartclipboard.domain.model.TopicActionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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

    suspend fun analyze(
        topic: Topic,
        items: List<DataItem>,
        userInstruction: String?
    ): Result<AgentResult> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("분석할 자료가 없습니다."))
        }

        val prompt = buildPrompt(topic, items, userInstruction)

        return try {
            val rawResponse = geminiManager.run(prompt)
            if (rawResponse.isBlank()) {
                Log.w(TAG, "Gemini 응답이 비어 있습니다.")
                Result.failure(IllegalStateException("Gemini 응답이 비어 있습니다. API 키 또는 네트워크 연결을 확인하세요."))
            } else {
                Log.d(TAG, "Gemini raw response:\n$rawResponse")
                val result = parse(topic.id, items.map { it.id }.toSet(), rawResponse)
                if (result.isFailure) {
                    Log.w(TAG, "Gemini JSON 파싱 실패", result.exceptionOrNull())
                }
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini 호출 실패", e)
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        topic: Topic,
        items: List<DataItem>,
        userInstruction: String?
    ): String {
        // 사용할 수 있는 ID 목록을 명시적으로 추출
        val availableItemIds = items.joinToString(", ") { it.id.toString() }

        // 자료 목록을 모델이 이해하기 쉽고 명확한 형태로 구성
        val itemDescriptions = items.joinToString("\n\n") { item ->
            """
            [Item ID: ${item.id}]
            - Type: ${item.type.name}
            - Title: ${item.title ?: "없음"}
            - Source: ${item.source ?: "없음"}
            - Content Preview: ${item.effectiveContent.take(1000).replace("\n", " ")}
            """.trimIndent()
        }

        val instructionLine = if (!userInstruction.isNullOrBlank()) {
            "\n## 사용자 추가 지시\n$userInstruction\n"
        } else ""

        return """
            당신은 SmartClipboardAI의 스마트 작업 계획 및 초안 생성 Agent(비서)입니다.
            사용자가 선택한 Topic과 자료 목록(DataItems)을 분석하여 실행 가능한 후속 작업(Action) 초안을 생성하세요.

            ## 반드시 지킬 규칙 (CRITICAL)
            - 응답은 반드시 JSON object 하나만 출력한다.
            - markdown 코드 펜스(```json, ``` 등), 설명문, 주석, 인사말을 절대 포함하지 마라. JSON 외의 텍스트는 허용되지 않는다.
            - 새 DataItem을 임의로 상상하여 만들지 마라.
            - [사용 가능한 sourceItemId 목록] 외의 id를 절대 반환하지 마라.
            - 근거 없는 장소, 참석자, 수신자, 일정을 상상하지 마라. 날짜/시간은 자료에 명확히 있을 때만 ISO-8601 형식으로 추출한다.
            - title, body, reason은 한국어로 자연스럽게 작성한다.
            - body는 사용자가 바로 복사하거나 편집하여 사용할 수 있는 '완성된 초안' 수준으로 작성한다.
            - 개인정보, URL, 주소, 연락처 등 민감한 값을 불필요하게 그대로 재출력하지 말고, 필요시 [OOO] 형태로 마스킹 처리한다.
            - Action은 최소 1개에서 최대 5개까지만 생성한다.

            ## 사용 가능한 sourceItemId 목록
            $availableItemIds

            ## 사용자 주제 (Topic)
            - title: ${topic.title}
            $instructionLine

            ## 분석할 자료 목록 (${items.size}개)
            $itemDescriptions

            ## 출력 JSON Schema
            {
              "summary": "주제와 자료의 핵심을 관통하는 요약 (2~3문장)",
              "keyPoints": ["핵심 포인트 1", "핵심 포인트 2", "핵심 포인트 3"],
              "sourceItemIds": [전체 분석에 사용된 id 배열],
              "recommendedActions": [
                {
                  "type": "SUMMARY|CALENDAR|REMINDER|TODO|SHARE_DRAFT",
                  "confidence": 0.95,
                  "reason": "이 액션을 추천하는 이유 (한국어 1문장)",
                  "title": "Action의 직관적인 짧은 제목",
                  "body": "사용자가 검토하고 바로 사용할 수 있는 초안 본문",
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
            - TODO: 해야 할 일 목록이 명확하나 캘린더/리마인더로 특정하기 어려울 때
              → payload: {"app":"INTERNAL_TODO", "tasks":[{"title":"...", "description":"...", "sourceItemIds":[...]}], "needsUserInput":[]}
            - SHARE_DRAFT: 누군가에게 전달할 메시지, 이메일, 보고서 초안 작성이 자연스러울 때
              → payload: {"app":"SHARE", "shareTitle":"...", "shareText":"...", "sourceItemIds":[...], "needsUserInput":[]}
        """.trimIndent()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun parse(
        topicId: Long,
        validItemIds: Set<Long>,
        rawResponse: String
    ): Result<AgentResult> {
        return try {
            val clean = rawResponse
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val root = json.decodeFromString<JsonObject>(clean)

            val summary = root["summary"]?.jsonPrimitive?.content
                ?: return Result.failure(IllegalArgumentException("summary 필드 누락"))

            val keyPoints: List<String> = root["keyPoints"]?.jsonArray?.mapNotNull { element ->
                element.jsonPrimitive.content.takeIf { it.isNotBlank() }
            } ?: emptyList()

            val sourceItemIds: List<Long> = root["sourceItemIds"]?.jsonArray?.mapNotNull { element ->
                element.jsonPrimitive.content.toLongOrNull()
            }?.filter { it in validItemIds } ?: emptyList()

            val actions: List<AgentActionDraft> = root["recommendedActions"]?.jsonArray?.mapNotNull { element ->
                parseAction(element, validItemIds)
            } ?: emptyList()

            Result.success(
                AgentResult(
                    topicId = topicId,
                    summary = summary,
                    keyPoints = keyPoints,
                    sourceItemIds = sourceItemIds,
                    actions = actions
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseAction(
        element: JsonElement,
        validItemIds: Set<Long>
    ): AgentActionDraft? {
        return try {
            val obj = element.jsonObject
            val typeStr = obj["type"]?.jsonPrimitive?.content ?: return null
            val type = TopicActionType.entries.find { it.name == typeStr } ?: return null
            val title = obj["title"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return null
            val body = obj["body"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return null

            val confidence: Float = obj["confidence"]?.jsonPrimitive?.content?.toFloatOrNull()
                ?.coerceIn(0f, 1f) ?: 0.5f

            val reason: String = obj["reason"]?.jsonPrimitive?.content ?: ""

            val actionSourceItemIds: List<Long> = obj["sourceItemIds"]?.jsonArray?.mapNotNull { el ->
                el.jsonPrimitive.content.toLongOrNull()
            }?.filter { it in validItemIds } ?: emptyList()

            val payload: String? = obj["payload"]?.let {
                json.encodeToString(JsonObject.serializer(), it.jsonObject)
            }

            AgentActionDraft(
                type = type,
                confidence = confidence,
                reason = reason,
                title = title,
                body = body,
                payload = payload,
                sourceItemIds = actionSourceItemIds
            )
        } catch (e: Exception) {
            null
        }
    }



    companion object {
        private const val TAG = "GeminiTopicAgent"
    }
}