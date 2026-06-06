package com.samsung.smartclipboard.data.tool

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.ToolExecutionResult
import com.samsung.smartclipboard.domain.model.ToolSpec
import com.samsung.smartclipboard.domain.tool.ToolExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

class ToolExecutorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ToolExecutor {

    override suspend fun execute(
        sessionId: String,
        action: AgentActionDraft,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult {
        return try {
            when (toolSpec.toolName) {
                "copy_to_clipboard" -> executeCopyToClipboard(sessionId, toolSpec, payload)
                "share_text" -> executeShareText(sessionId, toolSpec, payload)
                "open_url" -> executeOpenUrl(sessionId, toolSpec, payload)
                "compose_email" -> executeComposeEmail(sessionId, toolSpec, payload)
                "save_note" -> executeSaveNote(sessionId, toolSpec, payload)
                "insert_calendar_event" -> executeInsertCalendarEvent(sessionId, toolSpec, payload)
                "save_note_share" -> executeSaveNoteShare(sessionId, toolSpec, payload)
                "set_reminder" -> executeSetReminder(sessionId, toolSpec, payload)
                else -> ToolExecutionResult(
                    resultId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    toolName = toolSpec.toolName,
                    success = false,
                    message = "지원하지 않는 도구입니다.",
                    errorDetail = "unknown_tool: ${toolSpec.toolName}"
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "도구 실행 중 오류가 발생했습니다.",
                errorDetail = e.message ?: "unknown_error"
            )
        }
    }

    private fun executeCopyToClipboard(
        sessionId: String,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult {
        val text = payload["textToCopy"]
        if (text.isNullOrBlank()) {
            return ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "복사할 텍스트가 없습니다.",
                errorDetail = "empty_textToCopy"
            )
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SmartClipboard AI", text)
        clipboard.setPrimaryClip(clip)
        return ToolExecutionResult(
            resultId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            toolName = toolSpec.toolName,
            success = true,
            message = "클립보드에 복사되었습니다."
        )
    }

    private fun executeShareText(
        sessionId: String,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult {
        val shareText = payload["shareText"]
        if (shareText.isNullOrBlank()) {
            return ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "공유할 내용이 없습니다.",
                errorDetail = "empty_shareText"
            )
        }
        return try {
            val title = payload["shareTitle"].orEmpty()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, title.ifBlank { "공유" })
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = true,
                message = "공유 시트가 열렸습니다."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "공유 시트를 열지 못했습니다.",
                errorDetail = e.message ?: "share_failed"
            )
        }
    }

    private fun executeOpenUrl(
        sessionId: String,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult {
        val url = payload["url"]
        if (url.isNullOrBlank()) {
            return ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "열 URL이 없습니다.",
                errorDetail = "empty_url"
            )
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "안전하지 않은 URL은 열 수 없습니다.",
                errorDetail = "invalid_url_scheme"
            )
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = true,
                message = "URL을 열었습니다."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "URL을 열지 못했습니다.",
                errorDetail = e.message ?: "open_url_failed"
            )
        }
    }

    private fun executeComposeEmail(
        sessionId: String,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult {
        val subject = payload["subject"]
        val body = payload["body"]
        if (subject.isNullOrBlank() || body.isNullOrBlank()) {
            return ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "이메일 제목과 본문이 필요합니다.",
                errorDetail = "empty_email_fields"
            )
        }
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                val to = payload["to"]
                if (!to.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = true,
                message = "이메일 앱이 열렸습니다."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "이메일 앱을 열지 못했습니다.",
                errorDetail = e.message ?: "email_failed"
            )
        }
    }

    private fun executeSaveNote(
        sessionId: String,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult {
        // save_note는 repository 연동 없이 지원하지 않음
        return ToolExecutionResult(
            resultId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            toolName = toolSpec.toolName,
            success = false,
            message = "내부 노트 저장은 아직 데이터 저장 API와 연결되지 않았습니다.",
            errorDetail = "save_note_unsupported_without_existing_repository_api"
        )
    }

    private fun executeInsertCalendarEvent(
        sessionId: String,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult {
        val eventTitle = payload["eventTitle"]
        if (eventTitle.isNullOrBlank()) {
            return ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "일정 제목이 필요합니다.",
                errorDetail = "empty_eventTitle"
            )
        }
        return try {
            val now = System.currentTimeMillis()
            val beginTime = payload["eventBeginTime"]?.toLongOrNull() ?: (now + 60 * 60 * 1000L)
            val endTime = payload["eventEndTime"]?.toLongOrNull() ?: (beginTime + 60 * 60 * 1000L)

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, eventTitle)
                putExtra(CalendarContract.Events.DESCRIPTION, payload["eventDescription"].orEmpty())
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                val location = payload["eventLocation"]
                if (!location.isNullOrBlank()) {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = true,
                message = "캘린더 일정 초안이 열렸습니다."
            )
        } catch (e: android.content.ActivityNotFoundException) {
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "캘린더 앱을 찾을 수 없습니다.",
                errorDetail = "no_calendar_app: ${e.message}"
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "캘린더 앱을 열지 못했습니다.",
                errorDetail = e.message ?: "calendar_failed"
            )
        }
    }

    private fun executeSaveNoteShare(
        sessionId: String,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult {
        val noteTitle = payload["noteTitle"]
        val noteBody = payload["noteBody"]
        if (noteTitle.isNullOrBlank()) {
            return ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "노트 제목이 필요합니다.",
                errorDetail = "empty_noteTitle"
            )
        }
        return try {
            val shareContent = buildString {
                append(noteTitle)
                if (!noteBody.isNullOrBlank()) {
                    append("\n\n")
                    append(noteBody)
                }
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, noteTitle)
                putExtra(Intent.EXTRA_TEXT, shareContent)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "노트 앱으로 전달")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = true,
                message = "공유 시트가 열렸습니다. 삼성 노트 등 노트 앱을 선택하세요."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "노트 공유를 열지 못했습니다.",
                errorDetail = e.message ?: "note_share_failed"
            )
        }
    }

    private fun executeSetReminder(
        sessionId: String,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult {
        val reminderTitle = payload["reminderTitle"]
        if (reminderTitle.isNullOrBlank()) {
            return ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "알림 제목이 필요합니다.",
                errorDetail = "empty_reminderTitle"
            )
        }
        val reminderDescription = payload["reminderDescription"].orEmpty()
        val reminderTime = payload["reminderTime"]?.toLongOrNull()

        // 1차: AlarmClock ACTION_SET_ALARM 시도
        return try {
            val alarmIntent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, reminderTitle)
                if (reminderTime != null) {
                    val calendar = java.util.Calendar.getInstance().apply {
                        timeInMillis = reminderTime
                    }
                    putExtra(android.provider.AlarmClock.EXTRA_HOUR, calendar.get(java.util.Calendar.HOUR_OF_DAY))
                    putExtra(android.provider.AlarmClock.EXTRA_MINUTES, calendar.get(java.util.Calendar.MINUTE))
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(alarmIntent)
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = true,
                message = "알람/리마인더 앱이 열렸습니다."
            )
        } catch (_: Exception) {
            // 2차: 캘린더 일정으로 fallback
            try {
                val fallbackIntent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, reminderTitle)
                    putExtra(CalendarContract.Events.DESCRIPTION, reminderDescription)
                    if (reminderTime != null) {
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, reminderTime)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, reminderTime + 60 * 60 * 1000L)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                ToolExecutionResult(
                    resultId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    toolName = toolSpec.toolName,
                    success = true,
                    message = "캘린더 일정으로 알림 초안이 열렸습니다."
                )
            } catch (e: android.content.ActivityNotFoundException) {
                ToolExecutionResult(
                    resultId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    toolName = toolSpec.toolName,
                    success = false,
                    message = "알람/리마인더 앱을 찾을 수 없습니다.",
                    errorDetail = "no_reminder_app: ${e.message}"
                )
            } catch (e: Exception) {
                ToolExecutionResult(
                    resultId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    toolName = toolSpec.toolName,
                    success = false,
                    message = "알림 설정을 열지 못했습니다.",
                    errorDetail = e.message ?: "reminder_failed"
                )
            }
        }
    }
}
