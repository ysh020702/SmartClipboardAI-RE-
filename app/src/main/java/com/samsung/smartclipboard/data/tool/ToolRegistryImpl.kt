package com.samsung.smartclipboard.data.tool

import com.samsung.smartclipboard.domain.model.RequiredInput
import com.samsung.smartclipboard.domain.model.ToolRiskLevel
import com.samsung.smartclipboard.domain.model.ToolSpec
import com.samsung.smartclipboard.domain.tool.ToolRegistry

class ToolRegistryImpl : ToolRegistry {

    private val tools: List<ToolSpec> = listOf(
        ToolSpec(
            toolName = "copy_to_clipboard",
            description = "텍스트를 클립보드에 복사",
            riskLevel = ToolRiskLevel.LOW,
            requiresConfirmation = false,
            androidAction = "android.content.ClipboardManager",
            requiredInputs = listOf(
                RequiredInput(key = "textToCopy", label = "복사할 텍스트", required = true)
            )
        ),
        ToolSpec(
            toolName = "share_text",
            description = "Android 공유 시트로 텍스트 공유",
            riskLevel = ToolRiskLevel.LOW,
            requiresConfirmation = true,
            androidAction = "android.intent.action.SEND",
            requiredInputs = listOf(
                RequiredInput(key = "shareTitle", label = "공유 제목", required = false),
                RequiredInput(key = "shareText", label = "공유 내용", required = true)
            )
        ),
        ToolSpec(
            toolName = "open_url",
            description = "브라우저 또는 연결 앱에서 URL 열기",
            riskLevel = ToolRiskLevel.LOW,
            requiresConfirmation = true,
            androidAction = "android.intent.action.VIEW",
            requiredInputs = listOf(
                RequiredInput(key = "url", label = "열 URL", required = true)
            )
        ),
        ToolSpec(
            toolName = "compose_email",
            description = "이메일 앱에서 초안 작성",
            riskLevel = ToolRiskLevel.MEDIUM,
            requiresConfirmation = true,
            androidAction = "android.intent.action.SENDTO",
            requiredInputs = listOf(
                RequiredInput(key = "to", label = "받는 사람", required = false),
                RequiredInput(key = "subject", label = "제목", required = true),
                RequiredInput(key = "body", label = "본문", required = true)
            )
        ),
        ToolSpec(
            toolName = "save_note",
            description = "앱 내부 노트로 저장",
            riskLevel = ToolRiskLevel.LOW,
            requiresConfirmation = false,
            androidAction = "internal.save_note",
            requiredInputs = listOf(
                RequiredInput(key = "noteTitle", label = "노트 제목", required = true),
                RequiredInput(key = "noteBody", label = "노트 내용", required = true)
            )
        ),
        ToolSpec(
            toolName = "insert_calendar_event",
            description = "삼성 캘린더 일정 작성 화면을 우선 실행하고 기본 캘린더로 폴백",
            riskLevel = ToolRiskLevel.MEDIUM,
            requiresConfirmation = true,
            androidAction = "android.intent.action.INSERT",
            requiredInputs = listOf(
                RequiredInput(key = "eventTitle", label = "일정 제목", required = true),
                RequiredInput(key = "eventDescription", label = "일정 설명", required = false),
                RequiredInput(key = "eventBeginTime", label = "시작 시간 (epoch ms)", required = false),
                RequiredInput(key = "eventEndTime", label = "종료 시간 (epoch ms)", required = false),
                RequiredInput(key = "eventLocation", label = "장소", required = false)
            )
        ),
        ToolSpec(
            toolName = "save_note_share",
            description = "삼성 노트로 직접 전달하고, 미설치 시 공유 시트로 폴백",
            riskLevel = ToolRiskLevel.LOW,
            requiresConfirmation = true,
            androidAction = "android.intent.action.SEND",
            requiredInputs = listOf(
                RequiredInput(key = "noteTitle", label = "노트 제목", required = true),
                RequiredInput(key = "noteBody", label = "노트 내용", required = false)
            )
        ),
        ToolSpec(
            toolName = "set_reminder",
            description = "삼성 리마인더를 우선 실행하고 캘린더 또는 알람으로 폴백",
            riskLevel = ToolRiskLevel.MEDIUM,
            requiresConfirmation = true,
            androidAction = "android.intent.action.INSERT",
            requiredInputs = listOf(
                RequiredInput(key = "reminderTitle", label = "알림 제목", required = true),
                RequiredInput(key = "reminderDescription", label = "알림 설명", required = false),
                RequiredInput(key = "reminderTime", label = "알림 시간 (epoch ms)", required = false)
            )
        )
    )

    override fun getAllTools(): List<ToolSpec> = tools

    override fun getTool(toolName: String): ToolSpec? = tools.firstOrNull { it.toolName == toolName }
}
