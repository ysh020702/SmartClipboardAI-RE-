package com.samsung.smartclipboard.domain.tool

import com.samsung.smartclipboard.domain.model.ToolSpec

/**
 * MVP 도구(ToolSpec) 목록을 관리하는 인터페이스.
 *
 * ToolSpec 목록은 앱 내부에서 고정 관리하며, LLM이 새 toolName을 만들어도 사용할 수 없다.
 */
interface ToolRegistry {

    /** 등록된 모든 ToolSpec 목록을 반환한다. */
    fun getAllTools(): List<ToolSpec>

    /** toolName으로 특정 ToolSpec을 조회한다. 없으면 null. */
    fun getTool(toolName: String): ToolSpec?
}