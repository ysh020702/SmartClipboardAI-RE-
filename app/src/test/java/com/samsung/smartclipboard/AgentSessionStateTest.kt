package com.samsung.smartclipboard

import com.samsung.smartclipboard.domain.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * AgentSessionState sealed class의 각 상태 생성 검증.
 */
class AgentSessionStateTest {

    @Test
    fun `Idle 생성`() {
        val state: AgentSessionState = AgentSessionState.Idle
        assertNotNull(state)
        assertTrue(state is AgentSessionState.Idle)
    }

    @Test
    fun `PlanningRetrieval 생성`() {
        val state: AgentSessionState = AgentSessionState.PlanningRetrieval
        assertNotNull(state)
        assertTrue(state is AgentSessionState.PlanningRetrieval)
    }

    @Test
    fun `RetrievingItems 생성`() {
        val state = AgentSessionState.RetrievingItems(
            query = "테스트 쿼리",
            progress = 0.5f
        )
        assertEquals("테스트 쿼리", state.query)
        assertEquals(0.5f, state.progress)
    }

    @Test
    fun `AwaitingItemSelection 생성`() {
        val candidateItems = emptyList<CandidateItem>()
        val state = AgentSessionState.AwaitingItemSelection(
            candidateItems = candidateItems,
            recommendationReason = "연관성이 높은 항목입니다",
            suggestedQueries = listOf("검색어1", "검색어2"),
            selectedItemIds = setOf(1L, 2L)
        )
        assertNotNull(state)
        assertEquals(candidateItems, state.candidateItems)
        assertEquals("연관성이 높은 항목입니다", state.recommendationReason)
        assertEquals(2, state.suggestedQueries.size)
        assertEquals(setOf(1L, 2L), state.selectedItemIds)
    }

    @Test
    fun `GeneratingActions 생성`() {
        val state = AgentSessionState.GeneratingActions(selectedItemCount = 5)
        assertEquals(5, state.selectedItemCount)
    }

    @Test
    fun `Failed 생성_recoverable`() {
        val state = AgentSessionState.Failed(
            step = "PlanningRetrieval",
            message = "네트워크 오류",
            recoverable = true
        )
        assertEquals("PlanningRetrieval", state.step)
        assertEquals("네트워크 오류", state.message)
        assertTrue(state.recoverable)
    }

    @Test
    fun `Failed 생성_nonRecoverable`() {
        val state = AgentSessionState.Failed(
            step = "Executing",
            message = "권한 없음",
            recoverable = false
        )
        assertFalse(state.recoverable)
    }

    @Test
    fun `Completed 생성`() {
        val state = AgentSessionState.Completed(sessionId = "session-001")
        assertEquals("session-001", state.sessionId)
    }

    @Test
    fun `상태 객체 동등성_Idle은_singleton`() {
        val a = AgentSessionState.Idle
        val b = AgentSessionState.Idle
        assertSame(a, b)
    }

    @Test
    fun `상태 객체 동등성_RetrievingItems는_데이터_동등성`() {
        val a = AgentSessionState.RetrievingItems("q", 0.3f)
        val b = AgentSessionState.RetrievingItems("q", 0.3f)
        assertEquals(a, b)
    }
}