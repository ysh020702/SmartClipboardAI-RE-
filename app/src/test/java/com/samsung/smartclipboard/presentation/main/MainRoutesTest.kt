package com.samsung.smartclipboard.presentation.main

import com.samsung.smartclipboard.presentation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class MainRoutesTest {
    @Test
    fun routeForKeepsScreenAndData() {
        val data = mapOf(
            "topicId" to "7",
            "topicTitle" to "회의 자료 정리",
            "query" to "AI 추천 & 검토",
        )

        val route = MainRoutes.routeFor(Screen.TopicDetail, data)
        val decoded = MainRoutes.decodeNavData(route.substringAfter("${MainRoutes.NavDataArg}="))

        assertEquals("topicDetail", route.substringBefore("?"))
        assertEquals(data, decoded)
    }

    @Test
    fun routeForOmitsEmptyDataQuery() {
        assertEquals("home", MainRoutes.routeFor(Screen.Home, emptyMap()))
    }
}
