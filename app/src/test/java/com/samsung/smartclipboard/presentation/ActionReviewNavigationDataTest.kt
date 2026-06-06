package com.samsung.smartclipboard.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class ActionReviewNavigationDataTest {

    @Test
    fun `back data preserves topic context and action id`() {
        val data = mapOf(
            "actionType" to "calendar",
            "actionId" to "42",
            "topicId" to "7",
            "topicTitle" to "출장 일정",
            "from" to "history",
            "query" to "출장"
        )

        val backData = buildActionReviewBackData(data)

        assertEquals("42", backData["actionId"])
        assertEquals("7", backData["topicId"])
        assertEquals("출장 일정", backData["topicTitle"])
        assertEquals("history", backData["from"])
        assertEquals("출장", backData["query"])
    }
}
