package com.samsung.smartclipboard.data.tool

import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.RequiredInput
import com.samsung.smartclipboard.domain.model.TaskSelectionType
import com.samsung.smartclipboard.domain.tool.ToolRegistry
import com.samsung.smartclipboard.domain.tool.ToolRouteResult
import com.samsung.smartclipboard.domain.tool.ToolRouter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class ToolRouterImpl(
    private val toolRegistry: ToolRegistry
) : ToolRouter {

    override fun route(action: AgentActionDraft): Result<ToolRouteResult> {
        if (action.title.isBlank() && action.body.isBlank()) {
            return Result.failure(IllegalArgumentException("작업 제목과 본문이 모두 비어 있습니다"))
        }

        val parsedPayload = parsePayloadString(action.payload)
        val toolName = chooseToolName(action, parsedPayload)
        val toolSpec = toolRegistry.getTool(toolName)
            ?: return Result.failure(IllegalArgumentException("등록된 도구를 찾을 수 없습니다: $toolName"))

        val resolvedPayload = buildPayloadForTool(toolName, action, parsedPayload)
        val missingRequiredInputs = validateRequiredInputs(toolSpec, resolvedPayload)

        return Result.success(
            ToolRouteResult(
                action = action,
                toolSpec = toolSpec,
                resolvedPayload = resolvedPayload,
                missingRequiredInputs = missingRequiredInputs
            )
        )
    }

    // --- private helpers ---

    private fun parsePayloadString(payload: String?): Map<String, String> {
        if (payload.isNullOrBlank()) return emptyMap()
        val trimmed = payload.trim()
        return if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            tryParseJsonObject(trimmed)
        } else {
            mapOf("payload" to trimmed)
        }
    }

    private fun tryParseJsonObject(jsonStr: String): Map<String, String> {
        return try {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            val obj = json.parseToJsonElement(jsonStr).jsonObject

            obj.mapNotNull { (key, value) ->
                if (value === JsonNull) {
                    return@mapNotNull null
                }

                val primitive = value as? JsonPrimitive
                    ?: return@mapNotNull null

                key to primitive.content
            }.toMap()
        } catch (_: Exception) {
            mapOf("payload" to jsonStr)
        }
    }

    private fun chooseToolName(
        action: AgentActionDraft,
        parsedPayload: Map<String, String>
    ): String {
        parsedPayload["preferredTool"]?.let { preferred ->
            if (toolRegistry.getTool(preferred) != null) return preferred
        }
        parsedPayload["toolName"]?.let { tn ->
            if (toolRegistry.getTool(tn) != null) return tn
        }
        return when (action.type) {
            TaskSelectionType.CALENDAR -> "insert_calendar_event"
            TaskSelectionType.REMINDER -> "set_reminder"
            TaskSelectionType.SUMMARY -> "save_note_share"
            TaskSelectionType.TODO -> "save_note_share"
            TaskSelectionType.SHARE_DRAFT -> "share_text"
        }
    }

    private fun buildPayloadForTool(
        toolName: String,
        action: AgentActionDraft,
        parsedPayload: Map<String, String>
    ): Map<String, String> {
        val combinedText = "${action.title}\n\n${action.body}".trim()
        return when (toolName) {
            "copy_to_clipboard" -> mapOf(
                "textToCopy" to capText(combinedText, 10000)
            )
            "share_text" -> mapOf(
                "shareTitle" to capText(action.title, 200),
                "shareText" to capText(combinedText, 10000)
            )
            "open_url" -> {
                val url = parsedPayload["url"]
                    ?: extractFirstHttpUrl("${action.title} ${action.body} ${action.payload.orEmpty()}")
                if (url.isNullOrBlank()) emptyMap() else mapOf("url" to url)
            }
            "compose_email" -> mapOf(
                "to" to parsedPayload["to"].orEmpty(),
                "subject" to capText(action.title, 200),
                "body" to capText(action.body, 10000)
            )
            "save_note" -> mapOf(
                "noteTitle" to capText(action.title, 200),
                "noteBody" to capText(action.body, 10000)
            )
            "insert_calendar_event" -> mapOf(
                "eventTitle" to capText(action.title, 200),
                "eventDescription" to capText(action.body, 10000),
                "eventBeginTime" to firstNonBlank(
                    parsedPayload["eventBeginTime"],
                    parsedPayload["startTime"]
                ),
                "eventEndTime" to firstNonBlank(
                    parsedPayload["eventEndTime"],
                    parsedPayload["endTime"]
                ),
                "eventLocation" to capText(
                    firstNonBlank(
                        parsedPayload["eventLocation"],
                        parsedPayload["location"]
                    ),
                    200
                ),
                "isAllDay" to firstNonBlank(
                    parsedPayload["isAllDay"],
                    "false"
                )
            )
            "save_note_share" -> mapOf(
                "noteTitle" to capText(action.title, 200),
                "noteBody" to capText(action.body, 10000)
            )
            "set_reminder" -> mapOf(
                "reminderTitle" to capText(action.title, 200),
                "reminderDescription" to capText(action.body, 10000),
                "reminderTime" to firstNonBlank(
                    parsedPayload["reminderTime"],
                    parsedPayload["dueTime"]
                )
            )
            else -> parsedPayload
        }
    }

    private fun validateRequiredInputs(
        toolSpec: com.samsung.smartclipboard.domain.model.ToolSpec,
        payload: Map<String, String>
    ): List<RequiredInput> {
        return toolSpec.requiredInputs.filter { input ->
            input.required && payload[input.key].isNullOrBlank()
        }
    }

    private fun extractFirstHttpUrl(text: String): String? {
        val regex = Regex("(https?://[^\\s,;。，]+)")
        return regex.find(text)?.value
    }

    private fun capText(value: String, maxLength: Int): String {
        return if (value.length > maxLength) value.take(maxLength) else value
    }

    private fun firstNonBlank(vararg values: String?): String {
        return values.firstOrNull { !it.isNullOrBlank() }.orEmpty()
    }
}
