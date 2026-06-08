package com.samsung.smartclipboard.presentation.main.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePortalTransitionTest {
    @Test
    fun portalExpandDurationStaysInBalancedRange() {
        assertTrue(HomePortalTransition.TotalDurationMillis in 700..900)
    }

    @Test
    fun portalLayerStaysVisibleThroughMainScreenCrossfade() {
        assertTrue(
            HomePortalTransition.PostNavigateHoldMillis >=
                HomePortalTransition.MainScreenCrossfadeMillis,
        )
    }

    @Test
    fun portalExpandUsesAiTopicRecommendNavigationData() {
        assertEquals(
            mapOf("mode" to "ai_topic_recommend"),
            HomePortalTransition.aiSuggestNavigationData(),
        )
    }

    @Test
    fun portalExpandKeepsPracticalLoadingSteps() {
        assertEquals(
            listOf("수집 데이터 확인 중", "패턴 분류 중", "추천 주제 준비 중"),
            HomePortalTransition.LoadingSteps,
        )
    }
}
