package com.samsung.smartclipboard.data.ai

import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.AgentResult
import com.samsung.smartclipboard.domain.model.TopicActionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentJsonParser @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(
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
        element: kotlinx.serialization.json.JsonElement,
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
}