package com.samsung.smartclipboard.domain.model

/**
 * 장기 실행 에이전트 세션 상태를 담는 DTO.
 *
 * Flow A 전체 생명주기 동안 ViewModel에서 관리됩니다.
 * MVP에서는 In-Memory로 관리하며, P1에서 Room Entity로 전환 예정입니다.
 *
 * @property sessionId 세션 식별자
 * @property topicTitle 사용자가 입력한 주제
 * @property state 현재 에이전트 상태 머신 상태
 * @property candidateItems 검색/추천된 후보 아이템 목록
 * @property actionDrafts 생성된 Action 항목 목록
 * @property selectedActionIndex 사용자가 선택한 Action 인덱스
 * @property toolResults Tool 실행 결과 이력
 * @property createdAt 세션 생성 시각 (epoch millis)
 * @property updatedAt 세션 마지막 수정 시각 (epoch millis)
 */
data class AgentSession(
    val sessionId: String,
    val topicTitle: String,
    val state: AgentSessionState = AgentSessionState.Idle,
    val candidateItems: List<CandidateItem> = emptyList(),
    val actionDrafts: List<AgentActionDraft> = emptyList(),
    val selectedActionIndex: Int? = null,
    val toolResults: List<ToolExecutionResult> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)