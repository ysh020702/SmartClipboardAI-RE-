package com.samsung.smartclipboard.domain.tool

import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.ToolExecutionResult
import com.samsung.smartclipboard.domain.model.ToolSpec

/**
 * ToolSpec에 따라 실제 Android 동작을 실행하는 인터페이스.
 *
 * 모든 실행 결과는 ToolExecutionResult로 반환하며, 예외를 밖으로 던지지 않는다.
 */
interface ToolExecutor {

    /**
     * @param sessionId 연관 AgentSession ID
     * @param action 실행할 AgentActionDraft
     * @param toolSpec 매핑된 ToolSpec
     * @param payload 검증된 payload
     * @return 실행 결과
     */
    suspend fun execute(
        sessionId: String,
        action: AgentActionDraft,
        toolSpec: ToolSpec,
        payload: Map<String, String>
    ): ToolExecutionResult
}