package com.samsung.smartclipboard.data.tool

import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.RequiredInput
import com.samsung.smartclipboard.domain.model.TopicActionType
import com.samsung.smartclipboard.domain.tool.ToolRegistry
import com.samsung.smartclipboard.domain.tool.ToolRouteResult
import com.samsung.smartclipboard.domain.tool.ToolRouter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
            val j = Json { ignoreUnknownKeys = true; isLenient = true }
            val obj = j.parseToJsonElement(jsonStr).jsonObject
            obj.mapValues { it.value.jsonPrimitive.content }
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
            TopicActionType.CALENDAR -> "insert_calendar_event"
            TopicActionType.REMINDER -> "set_reminder"
            TopicActionType.SUMMARY -> "save_note_share"
            TopicActionType.TODO -> "save_note_share"
            TopicActionType.SHARE_DRAFT -> "share_text"
        }
    }

    private fun buildPayloadForTool(
        toolName: String,
        action: AgentActionDraft,
        parsedPayload: Map<String, String>
    ): Map<String, String> {
        val combinedText = "${action.title}\n\n${action.body}".trim()
        return when (toolName) {
            "copy_to_clipboard" -> mapOf("textToCopy" to capText(combinedText, 10000))
            "share_text" -> mapOf(
                "shareTitle" to capText(action.title, 200),
                "shareText" to capText(action.body.ifBlank { action.title }, 10000)
            )
            "open_url" -> {
                val url = parsedPayload["url"]
                    ?: extractFirstHttpUrl("${action.title} ${action.body} ${action.payload.orEmpty()}")
                if (url.isNullOrBlank()) emptyMap() else mapOf("url" to url)
            }
            "compose_email" -> mapOf(
                "to" to parsedPayload["to"].orEmpty(),
                "subject" to capText(parsedPayload["subject"] ?: action.title, 200),
                "body" to capText(parsedPayload["body"] ?: action.body, 10000)
            )
            "save_note" -> mapOf(
                "noteTitle" to capText(action.title, 200),
                "noteBody" to capText(action.body, 10000)
            )
            "insert_calendar_event" -> mapOf(
                "eventTitle" to capText(parsedPayload["eventTitle"] ?: action.title, 200),
                "eventDescription" to capText(parsedPayload["eventDescription"] ?: action.body, 10000),
                "eventBeginTime" to parsedPayload["eventBeginTime"].orEmpty(),
                "eventEndTime" to parsedPayload["eventEndTime"].orEmpty(),
                "eventLocation" to capText(parsedPayload["eventLocation"].orEmpty(), 200)
            )
            "save_note_share" -> mapOf(
                "noteTitle" to capText(parsedPayload["noteTitle"] ?: action.title, 200),
                "noteBody" to capText(parsedPayload["noteBody"] ?: action.body, 10000)
            )
            "set_reminder" -> mapOf(
                "reminderTitle" to capText(parsedPayload["reminderTitle"] ?: action.title, 200),
                "reminderDescription" to capText(parsedPayload["reminderDescription"] ?: action.body, 10000),
                "reminderTime" to parsedPayload["reminderTime"].orEmpty()
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
}
