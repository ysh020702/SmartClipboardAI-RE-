package com.samsung.smartclipboard.data.tool

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.ToolExecutionResult
import com.samsung.smartclipboard.domain.model.ToolSpec
import com.samsung.smartclipboard.domain.tool.ToolExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Locale
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

    private companion object {
        const val PKG_SAMSUNG_NOTES =
            "com.samsung.android.app.notes"

        const val PKG_SAMSUNG_REMINDER =
            "com.samsung.android.app.reminder"

        val PKG_SAMSUNG_CALENDARS = listOf(
            "com.samsung.android.calendar",
            "com.samsung.android.app.calendar"
        )
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
        val eventTitle = payload["eventTitle"].orEmpty().trim()

        if (eventTitle.isBlank()) {
            return newExecutionResult(
                sessionId,
                toolSpec,
                false,
                "일정 제목이 필요합니다.",
                "empty_eventTitle"
            )
        }

        val now = System.currentTimeMillis()

        val beginTime =
            parseTimeMillis(payload["eventBeginTime"])
                ?: now + 60 * 60 * 1000L

        val parsedEndTime =
            parseTimeMillis(payload["eventEndTime"])

        val endTime = when {
            parsedEndTime == null ->
                beginTime + 60 * 60 * 1000L

            parsedEndTime <= beginTime ->
                beginTime + 60 * 60 * 1000L

            else -> parsedEndTime
        }

        val baseIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI

            putExtra(
                CalendarContract.Events.TITLE,
                eventTitle
            )
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                payload["eventDescription"].orEmpty()
            )
            putExtra(
                CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                beginTime
            )
            putExtra(
                CalendarContract.EXTRA_EVENT_END_TIME,
                endTime
            )
            putExtra(
                CalendarContract.EXTRA_EVENT_ALL_DAY,
                payload["isAllDay"].toBoolean()
            )

            payload["eventLocation"]
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    putExtra(
                        CalendarContract.Events.EVENT_LOCATION,
                        it
                    )
                }

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            resolvePackage(
                baseIntent,
                PKG_SAMSUNG_CALENDARS
            )?.let { installedPackage ->
                baseIntent.setPackage(installedPackage)
            }

            context.startActivity(baseIntent)

            newExecutionResult(
                sessionId,
                toolSpec,
                true,
                if (baseIntent.`package` != null) {
                    "삼성 캘린더 일정 작성 화면이 열렸습니다."
                } else {
                    "캘린더 일정 작성 화면이 열렸습니다."
                }
            )
        } catch (e: ActivityNotFoundException) {
            newExecutionResult(
                sessionId,
                toolSpec,
                false,
                "일정을 추가할 수 있는 캘린더 앱이 없습니다.",
                "no_calendar_app"
            )
        } catch (e: Exception) {
            newExecutionResult(
                sessionId,
                toolSpec,
                false,
                "캘린더 앱을 열지 못했습니다.",
                e.message ?: "calendar_failed"
            )
        }
    }

    private fun executeSaveNoteShare(
        sessionId: String,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult {
        val noteTitle =
            payload["noteTitle"].orEmpty().trim()

        val noteBody =
            payload["noteBody"].orEmpty().trim()

        if (noteTitle.isBlank() && noteBody.isBlank()) {
            return ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "노트로 보낼 내용이 없습니다.",
                errorDetail = "empty_note"
            )
        }

        val shareContent = buildString {
            if (noteTitle.isNotBlank()) {
                append(noteTitle)
            }

            if (noteBody.isNotBlank()) {
                if (isNotEmpty()) {
                    append("\n\n")
                }

                append(noteBody)
            }
        }

        /*
         * Android 전체 공유 시트를 띄우지 않고
         * 삼성 노트의 공유 수신 화면을 직접 실행합니다.
         */
        val samsungNotesIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"

                if (noteTitle.isNotBlank()) {
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        noteTitle
                    )
                }

                putExtra(
                    Intent.EXTRA_TEXT,
                    shareContent
                )

                setPackage(PKG_SAMSUNG_NOTES)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        return try {
            if (
                samsungNotesIntent.resolveActivity(
                    context.packageManager
                ) != null
            ) {
                context.startActivity(samsungNotesIntent)

                ToolExecutionResult(
                    resultId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    toolName = toolSpec.toolName,
                    success = true,
                    message = "삼성 노트로 내용을 전달했습니다."
                )
            } else {
                /*
                 * 삼성 노트가 설치되지 않은 경우에만
                 * 일반 공유 시트로 폴백합니다.
                 */
                val fallbackIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"

                        if (noteTitle.isNotBlank()) {
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                noteTitle
                            )
                        }

                        putExtra(
                            Intent.EXTRA_TEXT,
                            shareContent
                        )

                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                val chooser =
                    Intent.createChooser(
                        fallbackIntent,
                        "노트 앱으로 전달"
                    ).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }

                context.startActivity(chooser)

                ToolExecutionResult(
                    resultId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    toolName = toolSpec.toolName,
                    success = true,
                    message =
                        "삼성 노트를 찾지 못해 공유 시트를 열었습니다."
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                resultId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                toolName = toolSpec.toolName,
                success = false,
                message = "삼성 노트를 열지 못했습니다.",
                errorDetail =
                    e.message ?: "note_share_failed"
            )
        }
    }

    private fun executeSetReminder(
        sessionId: String,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult {
        val reminderTitle =
            payload["reminderTitle"].orEmpty().trim()

        if (reminderTitle.isBlank()) {
            return newExecutionResult(
                sessionId,
                toolSpec,
                false,
                "리마인더 제목이 필요합니다.",
                "empty_reminderTitle"
            )
        }

        val reminderDescription =
            payload["reminderDescription"].orEmpty().trim()

        val reminderTime =
            parseTimeMillis(payload["reminderTime"])

        val reminderContent = buildString {
            if (reminderTitle.isNotBlank()) {
                append(reminderTitle)
            }
            if (reminderDescription.isNotBlank()) {
                if (isNotEmpty()) {
                    append("\n\n")
                }
                append(reminderDescription)
            }
        }

        /*
         * 1순위: 삼성 리마인더로 직접 전달
         */
        val samsungReminderIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, reminderTitle)
                putExtra(Intent.EXTRA_TEXT, reminderContent)
                setPackage(PKG_SAMSUNG_REMINDER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        try {
            if (
                samsungReminderIntent.resolveActivity(
                    context.packageManager
                ) != null
            ) {
                context.startActivity(samsungReminderIntent)

                return newExecutionResult(
                    sessionId,
                    toolSpec,
                    true,
                    "삼성 리마인더 작성 화면이 열렸습니다."
                )
            }
        } catch (_: Exception) {
            // 아래 폴백 실행
        }

        /*
         * 2순위: 날짜가 있으면 캘린더 일정으로 폴백
         */
        if (reminderTime != null) {
            val calendarIntent =
                Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI

                    putExtra(
                        CalendarContract.Events.TITLE,
                        reminderTitle
                    )
                    putExtra(
                        CalendarContract.Events.DESCRIPTION,
                        reminderDescription
                    )
                    putExtra(
                        CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                        reminderTime
                    )
                    putExtra(
                        CalendarContract.EXTRA_EVENT_END_TIME,
                        reminderTime + 30 * 60 * 1000L
                    )

                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            resolvePackage(
                calendarIntent,
                PKG_SAMSUNG_CALENDARS
            )?.let { packageName ->
                calendarIntent.setPackage(packageName)
            }

            try {
                context.startActivity(calendarIntent)

                return newExecutionResult(
                    sessionId,
                    toolSpec,
                    true,
                    "캘린더에 리마인더 일정 초안이 열렸습니다."
                )
            } catch (_: Exception) {
                // 알람 앱 폴백
            }
        }
        /*
     * 3순위: 시간 정보가 없거나 캘린더가 없으면 알람 앱
     */
        return try {
            val alarmIntent =
                Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(
                        AlarmClock.EXTRA_MESSAGE,
                        reminderContent
                    )

                    reminderTime?.let { time ->
                        val calendar =
                            java.util.Calendar.getInstance().apply {
                                timeInMillis = time
                            }

                        putExtra(
                            AlarmClock.EXTRA_HOUR,
                            calendar.get(
                                java.util.Calendar.HOUR_OF_DAY
                            )
                        )
                        putExtra(
                            AlarmClock.EXTRA_MINUTES,
                            calendar.get(
                                java.util.Calendar.MINUTE
                            )
                        )
                    }

                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            context.startActivity(alarmIntent)

            newExecutionResult(
                sessionId,
                toolSpec,
                true,
                "알람 설정 화면이 열렸습니다."
            )
        } catch (e: Exception) {
            newExecutionResult(
                sessionId,
                toolSpec,
                false,
                "리마인더를 처리할 앱을 찾지 못했습니다.",
                e.message ?: "reminder_failed"
            )
        }
    }

        private fun resolvePackage(
        baseIntent: Intent,
        packageNames: List<String>
    ): String? {
        return packageNames.firstOrNull { packageName ->
            Intent(baseIntent)
                .setPackage(packageName)
                .resolveActivity(context.packageManager) != null
        }
    }

    private fun parseTimeMillis(rawValue: String?): Long? {
        val value = rawValue?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        value.toLongOrNull()?.let { return it }

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mmXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd"
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }.parse(value)?.time
            }.getOrNull()
        }
    }

    private fun newExecutionResult(
        sessionId: String,
        toolSpec: ToolSpec,
        success: Boolean,
        message: String,
        errorDetail: String? = null
    ): ToolExecutionResult {
        return ToolExecutionResult(
            resultId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            toolName = toolSpec.toolName,
            success = success,
            message = message,
            errorDetail = errorDetail
        )
    }
}
