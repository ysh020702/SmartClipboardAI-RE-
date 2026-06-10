package com.samsung.smartclipboard.presentation.main.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // === 쿼리 ===

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    // === 설정 패널 ===

    fun openSettingsPanel(instantly: Boolean = false) {
        _uiState.update {
            it.copy(
                settingsPanelMounted = true,
                settingsPanelVisible = true,
                settingsPanelOpenInstantly = instantly,
                panelDragOffsetPx = null,
            )
        }
    }

    fun dismissSettingsPanel() {
        _uiState.update { it.copy(settingsPanelVisible = false) }
    }

    fun onSettingsPanelDismissAnimationFinished() {
        _uiState.update {
            it.copy(settingsPanelMounted = false, panelDragOffsetPx = null)
        }
    }

    fun updateDragOffset(offset: Float?) {
        _uiState.update { it.copy(panelDragOffsetPx = offset) }
    }

    fun onSettingsDragStart() {
        _uiState.update {
            it.copy(
                settingsPanelOpenInstantly = false,
                settingsPanelMounted = true,
                settingsPanelVisible = true,
            )
        }
    }

    // === 데이터 패널 ===

    fun openDataPanel() {
        _uiState.update {
            it.copy(dataPanelMounted = true, dataPanelVisible = true)
        }
    }

    fun dismissDataPanel() {
        _uiState.update { it.copy(dataPanelVisible = false) }
    }

    fun onDataPanelDismissAnimationFinished() {
        _uiState.update { it.copy(dataPanelMounted = false) }
    }

    // === 포털 전환 ===

    fun startPortalAnimation() {
        _uiState.update { it.copy(portalAnimating = true) }
    }

    fun finishPortalAnimation() {
        _uiState.update { it.copy(portalAnimating = false) }
    }

    fun updatePortalCardPosition(topLeft: Offset, size: IntSize) {
        _uiState.update { it.copy(portalCardTopLeft = topLeft, portalCardSize = size) }
    }

    // === 네비게이션 데이터 처리 ===

    fun handleNavData(data: Map<String, String>) {
        val openPanel = data["openPanel"]
        if (openPanel == "instant" || openPanel == "true") {
            openSettingsPanel(instantly = openPanel == "instant")
        }
    }
}