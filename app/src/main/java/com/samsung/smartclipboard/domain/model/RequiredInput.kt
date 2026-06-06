package com.samsung.smartclipboard.domain.model

/**
 * Tool 실행에 필요한 입력 필드 정의.
 *
 * @property key payload 내 키 식별자
 * @property label UI 표시용 레이블
 * @property value 사용자 입력 또는 LLM이 추출한 값. null이면 입력 필요.
 * @property required 필수 입력 여부
 */
data class RequiredInput(
    val key: String,
    val label: String,
    val value: String? = null,
    val required: Boolean = true
)