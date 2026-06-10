package com.samsung.smartclipboard.presentation.main.home

internal object HomePortalTransition {
    const val TotalDurationMillis = 550
    const val OverlayDurationMillis = 500
    const val GradientFlowDurationMillis = 600
    const val MainScreenCrossfadeMillis = 150
    const val PostNavigateHoldMillis = 200L

    val LoadingSteps = listOf("수집 데이터 확인 중", "패턴 분류 중", "추천 주제 준비 중")

    fun aiSuggestNavigationData(): Map<String, String> {
        return mapOf("mode" to "ai_topic_recommend")
    }
}
