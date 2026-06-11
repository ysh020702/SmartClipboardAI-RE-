package com.samsung.smartclipboard.presentation.main.data

import com.samsung.smartclipboard.domain.model.DataItem

data class DataUiState(
    val activeFilter: String = "전체",
    val items: List<DataItem> = emptyList(),
    val selectMode: Boolean = false,
    val selected: Set<Long> = emptySet(),
)