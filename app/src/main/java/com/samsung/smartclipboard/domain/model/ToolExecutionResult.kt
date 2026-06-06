package com.samsung.smartclipboard.domain.model

/**
 * Tool 실행 결과 DTO.
 *
 * @property resultId 결과 식별자
 * @property sessionId 연관 에이전트 세션 ID
 * @property toolName 실행된 도구 이름
 * @property success 실행 성공 여부
 * @property message 사용자 표시용 메시지
 * @property executedAt 실행 시각 (epoch millis)
 * @property errorDetail 실패 시 디버깅용 상세 오류 메시지
 */
data class ToolExecutionResult(
    val resultId: String,
    val sessionId: String,
    val toolName: String,
    val success: Boolean,
    val message: String,
    val executedAt: Long = System.currentTimeMillis(),
    val errorDetail: String? = null
)