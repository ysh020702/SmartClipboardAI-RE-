package com.samsung.smartclipboard.presentation.main

data class MainUiState(
    val dataSelectMode: Boolean = false,

    val bottomSheetVisible: Boolean = false,
    val sheetCount: Int = 0,
    val sheetTopicName: String = "",
)
