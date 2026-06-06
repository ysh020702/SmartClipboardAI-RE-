package com.samsung.smartclipboard.domain.tool

import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.RequiredInput
import com.samsung.smartclipboard.domain.model.ToolSpec

/**
 * ToolRouter.route()의 결과.
 *
 * @property action 라우팅된 원본 AgentActionDraft
 * @property toolSpec 매핑된 ToolSpec
 * @property resolvedPayload 검증된 payload (key: RequiredInput.key, value: 사용자 입력 또는 추출 값)
 * @property missingRequiredInputs 채워지지 않은 필수 입력 목록
 */
data class ToolRouteResult(
    val action: AgentActionDraft,
    val toolSpec: ToolSpec,
    val resolvedPayload: Map<String, String>,
    val missingRequiredInputs: List<RequiredInput> = emptyList()
)