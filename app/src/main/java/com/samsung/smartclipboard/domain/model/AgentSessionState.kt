package com.samsung.smartclipboard.domain.model

/**
 * 에이전트 세션의 상태 머신을 정의하는 sealed class.
 *
 * 상태 전이 테이블은 docs/AGENT_MOBILE_IMPLEMENTATION_HARNESS.md §5 참고.
 */
sealed class AgentSessionState {

    /** 초기 상태. TopicInputScreen 표시 */
    data object Idle : AgentSessionState()

    /** LLM이 검색 계획 수립 중 */
    data object PlanningRetrieval : AgentSessionState()

    /** 로컬 DB에서 DataItem 검색 중 */
    data class RetrievingItems(
        val query: String,
        val progress: Float
    ) : AgentSessionState()

    /** 검색 완료, CandidateItem 목록 표시. 사용자 선택 대기 */
    data class AwaitingItemSelection(
        val candidateItems: List<CandidateItem>,
        val recommendationReason: String,
        val suggestedQueries: List<String>,
        val selectedItemIds: Set<Long>
    ) : AgentSessionState()

    /** Action 후보 생성 중 (LLM 호출) */
    data class GeneratingActions(
        val selectedItemCount: Int
    ) : AgentSessionState()

    /** Action 후보 표시. 사용자 선택 대기 */
    data class AwaitingActionSelection(
        val actionDrafts: List<AgentActionDraft>,
        val selectedActionIndex: Int?
    ) : AgentSessionState()

    /** 선택된 Action에 맞는 Tool routing 중 */
    data class RoutingTool(
        val action: AgentActionDraft
    ) : AgentSessionState()

    /** 실행 전 확인. payload 표시 */
    data class AwaitingExecutionConfirm(
        val action: AgentActionDraft,
        val toolSpec: ToolSpec,
        val resolvedPayload: Map<String, String>
    ) : AgentSessionState()

    /** Tool 실행 중 */
    data class Executing(
        val action: AgentActionDraft
    ) : AgentSessionState()

    /** 실행 완료. 결과 표시 */
    data class Observing(
        val result: ToolExecutionResult
    ) : AgentSessionState()

    /** 사용자 피드백 기반 재생성 */
    data class Refining(
        val feedback: String
    ) : AgentSessionState()

    /** 전체 플로우 완료 */
    data class Completed(
        val sessionId: String
    ) : AgentSessionState()

    /** 오류 발생 */
    data class Failed(
        val step: String,
        val message: String,
        val recoverable: Boolean
    ) : AgentSessionState()
}