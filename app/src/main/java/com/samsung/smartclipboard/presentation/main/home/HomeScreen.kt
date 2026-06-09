package com.samsung.smartclipboard.presentation.main.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.main.history.HistoryTopicUi
import com.samsung.smartclipboard.presentation.main.history.HistoryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PanelEdgeShadowWidth: Dp = 8.dp
private val PanelOpenGestureStartPadding: Dp = 24.dp
private const val PanelEdgeShadowAlpha = 0.12f
private const val PanelCloseThresholdFraction = 0.18f
private const val PanelOpenThresholdFraction = 0.82f
private val SettingsIconShadowElevation: Dp = 20.dp
private val PanelFloatingButtonGradient = Brush.linearGradient(
    listOf(
        Color(0xFF2754D8),
        Color(0xFF2C66F0),
        Color(0xFF4387FF),
    )
)

@Composable
fun HomeScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String> = emptyMap(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
) {
    var query by remember { mutableStateOf("") }
    var settingsPanelMounted by remember { mutableStateOf(false) }
    var settingsPanelVisible by remember { mutableStateOf(false) }
    var settingsPanelOpenInstantly by remember { mutableStateOf(false) }
    var panelDragOffsetPx by remember { mutableStateOf<Float?>(null) }
    val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(data["openPanel"]) {
        if (data["openPanel"] == "instant" || data["openPanel"] == "true") {
            settingsPanelOpenInstantly = data["openPanel"] == "instant"
            settingsPanelMounted = true
            settingsPanelVisible = true
        }
    }

    HomeScreenContent(
        topics = historyState.topics,
        isHistoryLoading = historyState.isLoading,
        settingsPanelMounted = settingsPanelMounted,
        settingsPanelVisible = settingsPanelVisible,
        settingsPanelOpenInstantly = settingsPanelOpenInstantly,
        panelDragOffsetPx = panelDragOffsetPx,
        onSettingsPanelMountedChange = { settingsPanelMounted = it },
        onSettingsPanelVisibleChange = { settingsPanelVisible = it },
        onSettingsPanelOpenInstantlyChange = { settingsPanelOpenInstantly = it },
        onPanelDragOffsetChange = { panelDragOffsetPx = it },
        navigate = navigate,
    )
}

@Composable
private fun HomeScreenContent(
    topics: List<HistoryTopicUi>,
    isHistoryLoading: Boolean,
    settingsPanelMounted: Boolean,
    settingsPanelVisible: Boolean,
    settingsPanelOpenInstantly: Boolean,
    panelDragOffsetPx: Float?,
    onSettingsPanelMountedChange: (Boolean) -> Unit,
    onSettingsPanelVisibleChange: (Boolean) -> Unit,
    onSettingsPanelOpenInstantlyChange: (Boolean) -> Unit,
    onPanelDragOffsetChange: (Float?) -> Unit,
    navigate: (Screen, Map<String, String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var portalAnimating by remember { mutableStateOf(false) }
    var portalCardTopLeft by remember { mutableStateOf(Offset.Zero) }
    var portalCardSize by remember { mutableStateOf(IntSize.Zero) }
    val trimmedQuery = query.trim()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        val panelWidthPx = with(density) { maxWidth.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() / 2 }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(panelWidthPx) {
                    var openingOffsetPx = -panelWidthPx
                    var opening = false
                    var canOpenFromDrag = false
                    val openGestureStartPaddingPx = with(density) {
                        PanelOpenGestureStartPadding.toPx()
                    }

                    detectHorizontalDragGestures(
                        onDragStart = { start ->
                            openingOffsetPx = -panelWidthPx
                            opening = false
                            canOpenFromDrag = start.x >= openGestureStartPaddingPx
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (canOpenFromDrag && (dragAmount > 0f || opening)) {
                                opening = true
                                onSettingsPanelOpenInstantlyChange(false)
                                onSettingsPanelMountedChange(true)
                                onSettingsPanelVisibleChange(true)

                                openingOffsetPx = (openingOffsetPx + dragAmount)
                                    .coerceIn(-panelWidthPx, 0f)
                                onPanelDragOffsetChange(openingOffsetPx)
                            }
                        },
                        onDragEnd = {
                            if (opening) {
                                val shouldOpen =
                                    openingOffsetPx > -panelWidthPx * PanelOpenThresholdFraction
                                onPanelDragOffsetChange(null)
                                onSettingsPanelVisibleChange(shouldOpen)
                            }
                        },
                    )
                },
        ) {
        HomeContentColumn(
            scrollState = scrollState,
            query = query,
            isPortalAnimating = portalAnimating,
            onQueryChange = { query = it },
            onSettingsClick = {
                onSettingsPanelOpenInstantlyChange(false)
                onPanelDragOffsetChange(null)
                onSettingsPanelMountedChange(true)
                onSettingsPanelVisibleChange(true)
            },
            onPromptCardPositioned = { topLeft, size ->
                portalCardTopLeft = topLeft
                portalCardSize = size
            },
            onManualTopicClick = {
                navigate(
                    Screen.TopicDataSelect,
                    mapOf(
                        "topic" to trimmedQuery,
                        "mode" to "manual",
                    ),
                )
            },
            onAiSuggestClick = {
                if (!portalAnimating) {
                    portalAnimating = true
                }
            },
        )

        LaunchedEffect(settingsPanelVisible, settingsPanelMounted) {
            if (settingsPanelMounted && !settingsPanelVisible) {
                delay(240)
                onSettingsPanelMountedChange(false)
                onPanelDragOffsetChange(null)
            }
        }

        if (settingsPanelMounted) {
            HomeSettingsPanel(
                topics = topics,
                isLoading = isHistoryLoading,
                visible = settingsPanelVisible,
                openInstantly = settingsPanelOpenInstantly,
                externalDragOffsetPx = panelDragOffsetPx,
                onDismiss = { onSettingsPanelVisibleChange(false) },
                onHome = {
                    navigate(Screen.Home, emptyMap())
                    onSettingsPanelVisibleChange(false)
                },
                onNavigate = { screen, data ->
                    navigate(screen, data)
                },
            )
        }

        if (portalAnimating) {
            PortalExpandOverlay(
                startTopLeft = portalCardTopLeft,
                startSize = portalCardSize,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                onFinished = {
                    navigate(Screen.AiSuggest, HomePortalTransition.aiSuggestNavigationData())
                },
            )
        }
        }
    }
}

@Composable
private fun HomeContentColumn(
    scrollState: ScrollState,
    query: String,
    isPortalAnimating: Boolean,
    onQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onPromptCardPositioned: (Offset, IntSize) -> Unit,
    onManualTopicClick: () -> Unit,
    onAiSuggestClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 36.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            SettingsMenuButton(
                modifier = Modifier.align(Alignment.TopStart),
                onClick = onSettingsClick,
            )
        }

        Spacer(Modifier.height(42.dp))

        HomeHeroHeader()

        Spacer(Modifier.height(38.dp))

        HomePromptCard(
            query = query,
            isPortalAnimating = isPortalAnimating,
            onQueryChange = onQueryChange,
            onPositioned = onPromptCardPositioned,
            onManualTopicClick = onManualTopicClick,
            onAiSuggestClick = onAiSuggestClick,
        )

        Spacer(Modifier.height(24.dp))

        Spacer(
            modifier = Modifier.windowInsetsBottomHeight(WindowInsets.ime)
        )
    }
}

@Composable
private fun HomeHeroHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = AppColors.Blue,
            modifier = Modifier.size(54.dp)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "SmartClipboardAI",
            color = AppColors.Slate800,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "수집한 정보를 AI가 정리해드려요",
            color = AppColors.Slate400,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun HomePromptCard(
    query: String,
    isPortalAnimating: Boolean,
    onQueryChange: (String) -> Unit,
    onPositioned: (Offset, IntSize) -> Unit,
    onManualTopicClick: () -> Unit,
    onAiSuggestClick: () -> Unit,
) {
    val hasQuery = query.trim().isNotEmpty()
    val homeCardGradient = Brush.linearGradient(
        listOf(
            Color(0xFF2754D8),
            Color(0xFF2C66F0),
            Color(0xFF4387FF),
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.positionInRoot(), coordinates.size)
            }
            .background(homeCardGradient, RoundedCornerShape(22.dp))
            .padding(20.dp),
    ) {
        Text(
            text = "무엇을 정리할까요?",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "주제가 있다면 직접 입력하고, 없다면 AI에게 추천받아보세요.",
            color = Color(0xFFD7E1FF),
            fontSize = 11.sp
        )

        Spacer(Modifier.height(14.dp))

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            placeholder = {
                Text(
                    text = "회의 메모 정리, 여행 계획, 일정 캡처 분석",
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 12.sp
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.16f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.16f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onManualTopicClick,
            enabled = hasQuery && !isPortalAnimating,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.60f),
                contentColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.35f),
                disabledContentColor = Color.White.copy(alpha = 0.72f),
            ),
        ) {
            Text(
                text = "이 주제로 데이터 선택",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.22f))
        )

        Button(
            onClick = onAiSuggestClick,
            enabled = !isPortalAnimating,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = AppColors.Blue,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            Spacer(Modifier.size(6.dp))

            Text(
                text = "AI 주제 추천",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PortalExpandOverlay(
    startTopLeft: Offset,
    startSize: IntSize,
    screenWidthPx: Float,
    screenHeightPx: Float,
    onFinished: () -> Unit,
) {
    // Overlay alpha: 0 → 1 (screen gradually covered)
    val overlayAlpha = remember { Animatable(0f) }
    // Gradient flow: creates active movement in the gradient
    val flow = remember { Animatable(0f) }

    val overlayEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    LaunchedEffect(Unit) {
        // Overlay fades in and gradient flows simultaneously
        launch {
            overlayAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = HomePortalTransition.OverlayDurationMillis,
                    easing = overlayEasing,
                ),
            )
        }
        launch {
            flow.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = HomePortalTransition.GradientFlowDurationMillis,
                    easing = CubicBezierEasing(0.1f, 0f, 0.2f, 1f),
                ),
            )
        }

        // Wait for overlay to fully cover screen, then navigate
        delay(HomePortalTransition.OverlayDurationMillis.toLong() + 50)
        onFinished()
        delay(HomePortalTransition.PostNavigateHoldMillis)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .drawBehind {
                val flowProgress = flow.value

                // Base: DarkGradient fills the entire screen
                drawRect(brush = DarkGradient)

                // Flowing gradient layer 1: diagonal sweep that shifts over time
                val sweepOffsetX = size.width * 0.3f * flowProgress
                val sweepOffsetY = size.height * 0.2f * flowProgress
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2754D8).copy(alpha = 0.35f * (1f - flowProgress * 0.6f)),
                            Color(0xFF2C66F0).copy(alpha = 0.25f * (1f - flowProgress * 0.5f)),
                            Color(0xFF1A3660).copy(alpha = 0.15f * (1f - flowProgress * 0.4f)),
                            Color.Transparent,
                        ),
                        start = Offset(
                            x = -sweepOffsetX,
                            y = -sweepOffsetY,
                        ),
                        end = Offset(
                            x = size.width * 0.6f + sweepOffsetX,
                            y = size.height * 0.5f + sweepOffsetY,
                        ),
                    ),
                )

                // Flowing gradient layer 2: counter-directional subtle sweep
                val counterOffset = size.width * 0.15f * flowProgress
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF4387FF).copy(alpha = 0.12f * (1f - flowProgress * 0.7f)),
                            Color(0xFF2C66F0).copy(alpha = 0.08f * (1f - flowProgress * 0.5f)),
                        ),
                        start = Offset(
                            x = size.width + counterOffset,
                            y = size.height * 0.3f,
                        ),
                        end = Offset(
                            x = size.width * 0.3f - counterOffset,
                            y = size.height * 0.7f,
                        ),
                    ),
                )
            }
            .clickable(onClick = {}),
    )
}

private val DarkGradient = Brush.linearGradient(
    listOf(
        Color(0xFF0F1F3D),
        Color(0xFF1A3660),
        Color(0xFF1E3A8A),
    )
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenPreview() {
    HomeScreenContent(
        topics = previewHistoryTopics,
        isHistoryLoading = false,
        settingsPanelMounted = false,
        settingsPanelVisible = false,
        settingsPanelOpenInstantly = false,
        panelDragOffsetPx = null,
        onSettingsPanelMountedChange = {},
        onSettingsPanelVisibleChange = {},
        onSettingsPanelOpenInstantlyChange = {},
        onPanelDragOffsetChange = {},
        navigate = { _, _ -> },
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeSettingsPanelPreview() {
    HomeScreenContent(
        topics = previewHistoryTopics,
        isHistoryLoading = false,
        settingsPanelMounted = true,
        settingsPanelVisible = true,
        settingsPanelOpenInstantly = true,
        panelDragOffsetPx = null,
        onSettingsPanelMountedChange = {},
        onSettingsPanelVisibleChange = {},
        onSettingsPanelOpenInstantlyChange = {},
        onPanelDragOffsetChange = {},
        navigate = { _, _ -> },
    )
}

private val previewHistoryTopics = listOf(
    HistoryTopicUi(
        id = 1L,
        title = "회의 자료 정리",
        date = "6월 8일 21:30",
        dataCount = 4,
        summary = "회의 자료와 액션 아이템을 정리했습니다.",
        drafts = emptyList(),
    ),
    HistoryTopicUi(
        id = 2L,
        title = "제주도 여행 계획",
        date = "6월 7일 18:12",
        dataCount = 3,
        summary = "여행 일정과 예약 정보를 묶었습니다.",
        drafts = emptyList(),
    ),
    HistoryTopicUi(
        id = 3L,
        title = "개발 참고 자료",
        date = "6월 6일 10:04",
        dataCount = 5,
        summary = "개발 문서와 링크를 요약했습니다.",
        drafts = emptyList(),
    ),
)

@Composable
private fun SettingsMenuButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(
                elevation = SettingsIconShadowElevation,
                shape = CircleShape,
                ambientColor = Color(0x77000000),
                spotColor = Color(0x50000000),
            )
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111827))
            )
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111827))
            )
        }
    }
}

@Composable
private fun HomeSettingsPanel(
    topics: List<HistoryTopicUi>,
    isLoading: Boolean,
    visible: Boolean,
    openInstantly: Boolean,
    externalDragOffsetPx: Float?,
    onDismiss: () -> Unit,
    onHome: () -> Unit,
    onNavigate: (Screen, Map<String, String>) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val scope = rememberCoroutineScope()

    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val panelWidthPx = with(density) { maxWidth.toPx() }
            val panelOffset = remember(panelWidthPx, openInstantly) {
                Animatable(if (visible && openInstantly) 0f else -panelWidthPx)
            }

            LaunchedEffect(externalDragOffsetPx, panelWidthPx) {
                externalDragOffsetPx?.let { dragOffset ->
                    panelOffset.snapTo(dragOffset.coerceIn(-panelWidthPx, 0f))
                }
            }

            LaunchedEffect(visible, panelWidthPx, openInstantly, externalDragOffsetPx == null) {
                if (externalDragOffsetPx == null) {
                    if (visible && openInstantly) {
                        panelOffset.snapTo(0f)
                    } else {
                        panelOffset.animateTo(
                            targetValue = if (visible) 0f else -panelWidthPx,
                            animationSpec = if (visible) {
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                )
                            } else {
                                tween(240)
                            },
                        )
                    }
                }
            }

            val edgeShadowAlpha = if (panelWidthPx > 0f) {
                (-panelOffset.value / panelWidthPx)
                    .coerceIn(0f, 1f) * PanelEdgeShadowAlpha
            } else {
                0f
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(panelOffset.value.roundToInt(), 0) }
                    .background(Color.White)
                    .pointerInput(panelWidthPx) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    val nextOffset = (panelOffset.value + dragAmount)
                                        .coerceIn(-panelWidthPx, 0f)
                                    panelOffset.snapTo(nextOffset)
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    if (panelOffset.value < -panelWidthPx * PanelCloseThresholdFraction) {
                                        onDismiss()
                                    } else {
                                        panelOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                        )
                                    }
                                }
                            },
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(PanelEdgeShadowWidth)
                        .graphicsLayer { alpha = edgeShadowAlpha }
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black,
                                ),
                            ),
                        ),
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(1.dp)
                        .graphicsLayer { alpha = edgeShadowAlpha }
                        .background(Color(0xFFD7DCE5)),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 36.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "SmartClipboardAI",
                            color = AppColors.Slate800,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    QuickAccessRow(
                        icon = Icons.Default.Storage,
                        title = "수집 데이터",
                        description = "저장된 DataItem 보기",
                        onClick = { onNavigate(Screen.Data, mapOf("from" to "homePanel")) },
                    )

                    QuickAccessRow(
                        icon = Icons.Default.FolderOpen,
                        title = "저장공간",
                        description = "앱 저장 상태 확인",
                        onClick = { onNavigate(Screen.Storage, mapOf("from" to "homePanel")) },
                    )

                    QuickAccessRow(
                        icon = Icons.Default.Work,
                        title = "작업",
                        description = "생성된 실행 초안 보기",
                        onClick = { onNavigate(Screen.Tasks, mapOf("from" to "homePanel")) },
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(top = 18.dp, bottom = 22.dp)
                            .height(1.dp)
                            .background(Color(0xFFE5E7EB))
                    )

                    Text(
                        text = "최근",
                        color = AppColors.Slate500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 22.dp),
                    )

                    Spacer(Modifier.height(10.dp))

                    when {
                        isLoading -> {
                            Text(
                                text = "불러오는 중...",
                                color = AppColors.Slate400,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }

                        topics.isEmpty() -> {
                            Text(
                                text = "아직 정리한 작업이 없습니다.",
                                color = AppColors.Slate400,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }

                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 32.dp),
                            ) {
                                items(topics, key = { it.id }) { topic ->
                                    RecentTopicRow(
                                        topic = topic,
                                        onClick = {
                                            onNavigate(
                                                Screen.TopicDetail,
                                                mapOf(
                                                    "topicId" to topic.id.toString(),
                                                    "topicTitle" to topic.title,
                                                    "from" to "homePanel",
                                                ),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                PanelFloatingAnalysisButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 22.dp, bottom = 26.dp),
                    onClick = onHome,
                )
            }
        }
    }
}

@Composable
private fun QuickAccessRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    PanelPressGradientRow(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF5F6368),
            modifier = Modifier.size(19.dp),
        )

        Spacer(Modifier.size(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = AppColors.Slate800,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                color = AppColors.Slate400,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun PanelFloatingAnalysisButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = CircleShape,
                ambientColor = Color(0x33000000),
                spotColor = Color(0x26000000),
            )
            .clip(CircleShape)
            .background(PanelFloatingButtonGradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = "AI분석",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RecentTopicRow(
    topic: HistoryTopicUi,
    onClick: () -> Unit,
) {
    PanelPressGradientRow(onClick = onClick) {
        Column(Modifier.weight(1f)) {
            Text(
                text = topic.title,
                color = AppColors.Slate800,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "${topic.date} · 데이터 ${topic.dataCount}개 · 초안 ${topic.drafts.size}개",
                color = AppColors.Slate400,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PanelPressGradientRow(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val spread = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    var pressPosition by remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (alpha.value > 0.01f) {
                    val centerX = pressPosition.x.coerceIn(0f, size.width)
                    val spreadWidth = (size.width * 1.25f * spread.value).coerceAtLeast(1f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.5f to Color(0xFFE7EAEE),
                                1f to Color.Transparent,
                            ),
                            startX = centerX - spreadWidth,
                            endX = centerX + spreadWidth,
                        ),
                        alpha = alpha.value,
                    )
                }
            }
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = { offset ->
                        pressPosition = offset
                        scope.launch {
                            spread.snapTo(0f)
                            alpha.snapTo(0.92f)
                            launch {
                                spread.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 420),
                                )
                            }
                            alpha.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 430),
                            )
                        }
                        if (tryAwaitRelease()) {
                            delay(50)
                            onClick()
                        }
                    },
                )
            }
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
