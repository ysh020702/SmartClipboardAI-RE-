package com.samsung.smartclipboard.presentation.main.topicselection

import com.samsung.smartclipboard.domain.model.TopicAction
import com.samsung.smartclipboard.domain.model.TopicActionStatus
import com.samsung.smartclipboard.domain.model.TopicActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicSelectionUiStateTest {

    @Test
    fun `default state starts loading with no topic or actions`() {
        val state = TopicSelectionUiState()

        assertNull(state.topicId)
        assertNull(state.topicTitle)
        assertTrue(state.actions.isEmpty())
        assertTrue(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(0, state.actionCount)
    }

    @Test
    fun `topic action maps to card ui with action review route key`() {
        val action = TopicAction(
            id = 7L,
            topicId = 3L,
            analysisResultId = 2L,
            type = TopicActionType.CALENDAR,
            title = "일정 초안",
            body = "6월 10일 미팅을 추가해요.",
            status = TopicActionStatus.DRAFT,
            editablePayload = null,
            createdAt = 100L,
            updatedAt = 200L
        )

        val card = action.toTopicActionCardUi()

        assertEquals(7L, card.actionId)
        assertEquals("calendar", card.routeActionType)
        assertEquals("캘린더", card.typeLabel)
        assertEquals("초안", card.statusLabel)
        assertEquals("일정 초안", card.title)
        assertEquals("6월 10일 미팅을 추가해요.", card.description)
    }
}
