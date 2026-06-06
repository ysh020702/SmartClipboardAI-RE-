package com.samsung.smartclipboard.domain.model

/**
 * 도구 실행 위험 수준.
 */
enum class ToolRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * 도구 스펙 DTO.
 *
 * @property toolName 도구 식별자 (e.g., "save_note", "share_text")
 * @property description 도구 설명 (UI 표시 및 문서화)
 * @property riskLevel 도구 실행 위험 수준
 * @property requiresConfirmation 실행 전 사용자 확인 필요 여부
 * @property androidAction Android Intent 생성을 위한 action 문자열
 * @property requiredInputs 실행에 필요한 입력 필드 목록
 */
data class ToolSpec(
    val toolName: String,
    val description: String,
    val riskLevel: ToolRiskLevel,
    val requiresConfirmation: Boolean,
    val androidAction: String,
    val requiredInputs: List<RequiredInput> = emptyList()
)