package com.samsung.smartclipboard.presentation.main

import com.samsung.smartclipboard.presentation.NavTab
import com.samsung.smartclipboard.presentation.Screen

data class MainUiState(
    val activeTab: NavTab = NavTab.Home,
    val screen: Screen = Screen.Home,
    val navData: Map<String, String> = emptyMap(),

    val dataSelectMode: Boolean = false,

    val bottomSheetVisible: Boolean = false,
    val sheetCount: Int = 0,
    val sheetTopicName: String = "",
)