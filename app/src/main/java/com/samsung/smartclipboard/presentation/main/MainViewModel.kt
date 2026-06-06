package com.samsung.smartclipboard.presentation.main

import androidx.lifecycle.ViewModel
import com.samsung.smartclipboard.presentation.NavTab
import com.samsung.smartclipboard.presentation.Screen
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

    fun navigate(target: Screen, data: Map<String, String> = emptyMap()) {
        _uiState.update { current ->
            current.copy(
                screen = target,
                navData = data,
                activeTab = resolveActiveTab(
                    target = target,
                    currentTab = current.activeTab,
                ),
            )
        }
    }

    fun onBottomTabSelected(tab: NavTab) {
        when (tab) {
            NavTab.Home -> navigate(Screen.Home)
            NavTab.Data -> navigate(Screen.Data)
            NavTab.Tasks -> navigate(Screen.Tasks)
        }
    }

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

    fun confirmAnalysis() {
        _uiState.update { current ->
            current.copy(
                bottomSheetVisible = false,
                dataSelectMode = false,
                screen = Screen.Analyzing,
                navData = mapOf(
                    "selectedCount" to current.sheetCount.toString(),
                    "topicName" to current.sheetTopicName,
                ),
                // 기존 로직과 동일하게 Analyzing 화면으로 가도 activeTab은 유지
                activeTab = current.activeTab,
            )
        }
    }

    private fun resolveActiveTab(
        target: Screen,
        currentTab: NavTab,
    ): NavTab {
        return when (target) {
            Screen.Home -> NavTab.Home
            Screen.Data -> NavTab.Data
            Screen.Tasks -> NavTab.Tasks
            else -> currentTab
        }
    }
}