package com.samsung.smartclipboard.presentation.main.home

internal object HomePortalTransition {
    const val TotalDurationMillis = 840
    const val PressDurationMillis = 120
    const val ExpandDurationMillis = 520
    const val DarkRevealDurationMillis = 180
    const val MainScreenCrossfadeMillis = 360
    const val PostNavigateHoldMillis = 360L

    val LoadingSteps = listOf("수집 데이터 확인 중", "패턴 분류 중", "추천 주제 준비 중")

    fun aiSuggestNavigationData(): Map<String, String> {
        return mapOf("mode" to "ai_topic_recommend")
    }
}
