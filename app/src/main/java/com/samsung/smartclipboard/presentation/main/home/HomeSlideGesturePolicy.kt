package com.samsung.smartclipboard.presentation.main.home

internal object HomeSlideGesturePolicy {
    private const val CenterGapStartFraction = 0.45f
    private const val CenterGapEndFraction = 0.55f

    fun canStartSettingsOpen(
        startX: Float,
        screenWidthPx: Float,
    ): Boolean {
        return startX < screenWidthPx * CenterGapStartFraction
    }

    fun canStartDataOpen(
        startX: Float,
        screenWidthPx: Float,
    ): Boolean {
        return startX > screenWidthPx * CenterGapEndFraction
    }
}
