package com.samsung.smartclipboard.presentation.main.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

data class HomeUiState(
    // 쿼리 입력
    val query: String = "",

    // 설정 패널
    val settingsPanelMounted: Boolean = false,
    val settingsPanelVisible: Boolean = false,
    val settingsPanelOpenInstantly: Boolean = false,
    val panelDragOffsetPx: Float? = null,

    // 데이터 패널
    val dataPanelMounted: Boolean = false,
    val dataPanelVisible: Boolean = false,
    val dataPanelLoadContent: Boolean = false,
    val dataPanelDragOffsetPx: Float? = null,

    // 포털 전환 애니메이션
    val portalAnimating: Boolean = false,
    val portalCardTopLeft: Offset = Offset.Zero,
    val portalCardSize: IntSize = IntSize.Zero,
)
