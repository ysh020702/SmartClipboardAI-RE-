package com.samsung.smartclipboard.domain.tool

import com.samsung.smartclipboard.domain.model.AgentActionDraft

/**
 * AgentActionDraft를 실행 가능한 ToolSpec + 검증된 payload로 변환하는 인터페이스.
 *
 * DB 접근 없음, LLM 호출 없음, Android Context 사용 안 함.
 */
interface ToolRouter {

    /**
     * @param action 라우팅할 AgentActionDraft
     * @return ToolRouteResult. missingRequiredInputs가 있으면 ViewModel에서 Failed 처리.
     */
    fun route(action: AgentActionDraft): Result<ToolRouteResult>
}