package com.samsung.smartclipboard.presentation.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onDataSelectModeChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(dataSelectMode = enabled)
        }
    }

    fun openAnalysisSheet(count: Int, title: String) {
        _uiState.update {
            it.copy(
                sheetCount = count,
                sheetTopicName = title,
                bottomSheetVisible = true,
            )
        }
    }

    fun dismissAnalysisSheet() {
        _uiState.update {
            it.copy(bottomSheetVisible = false)
        }
    }

    fun onSheetTopicNameChanged(topicName: String) {
        _uiState.update {
            it.copy(sheetTopicName = topicName)
        }
    }

    fun confirmAnalysis(): Map<String, String> {
        val analysisData = _uiState.value.let { current ->
            mapOf(
                "selectedCount" to current.sheetCount.toString(),
                "topicName" to current.sheetTopicName,
            )
        }

        _uiState.update {
            it.copy(
                bottomSheetVisible = false,
                dataSelectMode = false,
            )
        }

        return analysisData
    }
}
