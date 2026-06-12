package com.samsung.smartclipboard.presentation.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.main.history.HistoryTopicUi
import com.samsung.smartclipboard.presentation.main.history.HistoryViewModel

@Composable
fun HomeScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String> = emptyMap(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
) {
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(data) {
        homeViewModel.handleNavData(data)
    }

    HomeScreenContent(
        state = homeState,
        topics = historyState.topics,
        isHistoryLoading = historyState.isLoading,
        onQueryChange = homeViewModel::onQueryChange,
        onSettingsClick = { homeViewModel.openSettingsPanel() },
        onSettingsPanelDismiss = homeViewModel::dismissSettingsPanel,
        onSettingsPanelDismissAnimationFinished = homeViewModel::onSettingsPanelDismissAnimationFinished,
        onSettingsDragStart = homeViewModel::onSettingsDragStart,
        onDragOffsetChange = homeViewModel::updateDragOffset,
        onDataDragStart = homeViewModel::onDataDragStart,
        onDataDragOffsetChange = homeViewModel::updateDataDragOffset,
        onDataPanelOpen = homeViewModel::openDataPanel,
        onDataPanelDismiss = homeViewModel::dismissDataPanel,
        onDataPanelDismissAnimationFinished = homeViewModel::onDataPanelDismissAnimationFinished,
        onPortalCardPositioned = homeViewModel::updatePortalCardPosition,
        onAiSuggestClick = homeViewModel::startPortalAnimation,
        onPortalFinished = {
            navigate(Screen.AiSuggest, HomePortalTransition.aiSuggestNavigationData())
            homeViewModel.finishPortalAnimationAfterNavigation()
        },
        onManualTopicClick = {
            navigate(
                Screen.TopicDataSelect,
                mapOf("topic" to homeState.query.trim(), "mode" to "manual"),
            )
        },
        onNavigate = navigate,
    )
}

@Composable
private fun HomeScreenContent(
    state: HomeUiState,
    topics: List<HistoryTopicUi>,
    isHistoryLoading: Boolean,
    onQueryChange: (String) -> Unit,                           // 쿼리 텍스트 변경 시 호출 → HomeViewModel.onQueryChange
    onSettingsClick: () -> Unit,                                // 설정 버튼 클릭 → HomeViewModel.openSettingsPanel
    onSettingsPanelDismiss: () -> Unit,                         // 설정 패널 닫기 요청 → HomeViewModel.dismissSettingsPanel
    onSettingsPanelDismissAnimationFinished: () -> Unit,        // 설정 패널 닫힘 애니메이션 완료 후 unmount → HomeViewModel.onSettingsPanelDismissAnimationFinished
    onSettingsDragStart: () -> Unit,                            // 설정 패널 드래그 시작 (패널 mounted/visible 즉시 설정) → HomeViewModel.onSettingsDragStart
    onDragOffsetChange: (Float?) -> Unit,                       // 드래그 오프셋 실시간 업데이트 (null이면 애니메이션 모드) → HomeViewModel.updateDragOffset
    onDataDragStart: (Float) -> Unit,                           // 데이터 패널 드래그 시작 (로딩 없이 패널 mounted/visible 즉시 설정) → HomeViewModel.onDataDragStart
    onDataDragOffsetChange: (Float?) -> Unit,                   // 데이터 패널 드래그 오프셋 실시간 업데이트 → HomeViewModel.updateDataDragOffset
    onDataPanelOpen: () -> Unit,                                // 데이터 패널 열기 → HomeViewModel.openDataPanel
    onDataPanelDismiss: () -> Unit,                              // 데이터 패널 닫기 요청 → HomeViewModel.dismissDataPanel
    onDataPanelDismissAnimationFinished: () -> Unit,            // 데이터 패널 닫힘 애니메이션 완료 후 unmount → HomeViewModel.onDataPanelDismissAnimationFinished
    onPortalCardPositioned: (Offset, IntSize) -> Unit,          // 프롬프트 카드 레이아웃 위치/크기 업데이트 → HomeViewModel.updatePortalCardPosition
    onAiSuggestClick: () -> Unit,                                // AI 추천 버튼 클릭 → 포털 애니메이션 시작 → HomeViewModel.startPortalAnimation
    onPortalFinished: () -> Unit,                                // 포털 애니메이션 종료 → HomeViewModel.finishPortalAnimation + AiSuggest 화면 이동
    onManualTopicClick: () -> Unit,                              // "이 주제로 데이터 선택" 버튼 클릭 → TopicDataSelect 화면 이동
    onNavigate: (Screen, Map<String, String>) -> Unit,          // 외부 화면 이동 공통 콜백
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        val panelWidthPx = with(density) { maxWidth.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(panelWidthPx) {
                    var settingsOpeningOffsetPx = -panelWidthPx
                    var settingsOpening = false
                    var canOpenSettingsFromDrag = false

                    var dataOpeningOffsetPx = panelWidthPx
                    var dataOpening = false
                    var canOpenDataFromDrag = false

                    detectHorizontalDragGestures(
                        onDragStart = { start ->
                            settingsOpeningOffsetPx = -panelWidthPx
                            settingsOpening = false
                            canOpenSettingsFromDrag = HomeSlideGesturePolicy.canStartSettingsOpen(
                                startX = start.x,
                                screenWidthPx = screenWidthPx,
                            )

                            dataOpeningOffsetPx = panelWidthPx
                            dataOpening = false
                            canOpenDataFromDrag = HomeSlideGesturePolicy.canStartDataOpen(
                                startX = start.x,
                                screenWidthPx = screenWidthPx,
                            )
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // Settings panel: 오른쪽 드래그 (dragAmount > 0) — 실시간 추적
                            if (canOpenSettingsFromDrag && (dragAmount > 0f || settingsOpening)) {
                                settingsOpening = true
                                onSettingsDragStart()

                                settingsOpeningOffsetPx = (settingsOpeningOffsetPx + dragAmount)
                                    .coerceIn(-panelWidthPx, 0f)
                                onDragOffsetChange(settingsOpeningOffsetPx)
                            }

                            // Data panel: 왼쪽 드래그 (dragAmount < 0) — 실시간 추적
                            if (canOpenDataFromDrag && (dragAmount < 0f || dataOpening)) {
                                dataOpeningOffsetPx = (dataOpeningOffsetPx + dragAmount)
                                    .coerceIn(0f, panelWidthPx)
                                if (!dataOpening) {
                                    dataOpening = true
                                    onDataDragStart(dataOpeningOffsetPx)
                                } else {
                                    onDataDragOffsetChange(dataOpeningOffsetPx)
                                }
                            }
                        },
                        onDragEnd = {
                            if (settingsOpening) {
                                val shouldOpen =
                                    settingsOpeningOffsetPx > -panelWidthPx * PanelDefaults.OpenThresholdFraction
                                onDragOffsetChange(null)
                                if (!shouldOpen) {
                                    onSettingsPanelDismiss()
                                }
                            }
                            if (dataOpening) {
                                val totalDrag = panelWidthPx - dataOpeningOffsetPx
                                val shouldOpen = totalDrag > panelWidthPx * PanelDefaults.DataPanelOpenThresholdFraction
                                onDataDragOffsetChange(null)
                                if (shouldOpen) {
                                    onDataPanelOpen()
                                } else {
                                    onDataPanelDismiss()
                                }
                            }
                        },
                    )
                },
        ) {
            HomeContentColumn(
                scrollState = scrollState,
                query = state.query,
                isPortalAnimating = state.portalAnimating,
                onQueryChange = onQueryChange,
                onSettingsClick = onSettingsClick,
                onPromptCardPositioned = onPortalCardPositioned,
                onManualTopicClick = onManualTopicClick,
                onAiSuggestClick = onAiSuggestClick,
            )

            if (state.settingsPanelMounted) {
                HomeSettingsPanel(
                    topics = topics,
                    isLoading = isHistoryLoading,
                    visible = state.settingsPanelVisible,
                    openInstantly = state.settingsPanelOpenInstantly,
                    externalDragOffsetPx = state.panelDragOffsetPx,
                    onDismiss = onSettingsPanelDismiss,
                    onDismissAnimationFinished = onSettingsPanelDismissAnimationFinished,
                    onHome = {
                        onSettingsPanelDismiss()
                    },
                    onNavigate = onNavigate,
                )
            }

            if (state.dataPanelMounted) {
                HomeDataPanel(
                    visible = state.dataPanelVisible,
                    loadContent = state.dataPanelLoadContent,
                    externalDragOffsetPx = state.dataPanelDragOffsetPx,
                    onDismiss = onDataPanelDismiss,
                    onDismissAnimationFinished = onDataPanelDismissAnimationFinished,
                    onNavigate = onNavigate,
                )
            }

            if (state.portalAnimating) {
                PortalExpandOverlay(
                    startTopLeft = state.portalCardTopLeft,
                    startSize = state.portalCardSize,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    onFinished = onPortalFinished,
                )
            }
        }
    }
}
