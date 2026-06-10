package com.samsung.smartclipboard.presentation.main.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSlideGesturePolicyTest {
    @Test
    fun settingsPanelCanStartFromLeftSideButNotCenterGap() {
        val screenWidthPx = 1000f

        assertTrue(HomeSlideGesturePolicy.canStartSettingsOpen(startX = 0f, screenWidthPx = screenWidthPx))
        assertTrue(HomeSlideGesturePolicy.canStartSettingsOpen(startX = 449f, screenWidthPx = screenWidthPx))
        assertFalse(HomeSlideGesturePolicy.canStartSettingsOpen(startX = 450f, screenWidthPx = screenWidthPx))
        assertFalse(HomeSlideGesturePolicy.canStartSettingsOpen(startX = 550f, screenWidthPx = screenWidthPx))
    }

    @Test
    fun dataPanelCanStartFromRightSideButNotCenterGap() {
        val screenWidthPx = 1000f

        assertFalse(
            HomeSlideGesturePolicy.canStartDataOpen(
                startX = 450f,
                screenWidthPx = screenWidthPx,
            ),
        )
        assertFalse(
            HomeSlideGesturePolicy.canStartDataOpen(
                startX = 550f,
                screenWidthPx = screenWidthPx,
            ),
        )
        assertTrue(
            HomeSlideGesturePolicy.canStartDataOpen(
                startX = 551f,
                screenWidthPx = screenWidthPx,
            ),
        )
        assertTrue(
            HomeSlideGesturePolicy.canStartDataOpen(
                startX = 1000f,
                screenWidthPx = screenWidthPx,
            ),
        )
    }
}
