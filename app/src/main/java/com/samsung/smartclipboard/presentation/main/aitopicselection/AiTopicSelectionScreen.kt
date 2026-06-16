package com.samsung.smartclipboard.presentation.main.aitopicselection

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.smartclipboard.presentation.AnalysisStep
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.DarkGradient
import com.samsung.smartclipboard.presentation.Pill
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.StepStatus
import com.samsung.smartclipboard.presentation.main.home.HomePortalTransition
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class TopicAiSuggestionCardUi(
    val id: String,
    val title: String,
    val description: String,
    val dataTypes: List<String>,
    val tags: List<String>,
    val icon: ImageVector,
    val color: Color,
    val accentBg: Color,
)

fun TopicAiSuggestionUi.toTopicAiSuggestionCardUi(): TopicAiSuggestionCardUi {
    val confidencePercent = (confidence.coerceIn(0.0f, 1.0f) * 100).toInt()
    return TopicAiSuggestionCardUi(
        id = id,
        title = title,
        description = description,
        dataTypes = listOf("자료 ${itemCount}개", "신뢰도 $confidencePercent%"),
        tags = listOf(clusterLabel).filter { it.isNotBlank() },
        icon = Icons.Default.Description,
        color = AppColors.Blue,
        accentBg = AppColors.BlueSoft,
    )
}

internal enum class TopicAiSuggestContent {
    Loading,
    Results,
    Creating,
}

internal fun resolveTopicAiSuggestContent(
    uiState: TopicAiSuggestUiState,
    preferInitialLoading: Boolean,
): TopicAiSuggestContent {
    return when {
        uiState.isCreatingTopic || uiState.creatingSteps.isNotEmpty() -> TopicAiSuggestContent.Creating
        uiState.isLoading || uiState.loadingSteps.isNotEmpty() -> TopicAiSuggestContent.Loading
        preferInitialLoading && uiState.suggestions.isEmpty() && uiState.errorMessage == null -> {
            TopicAiSuggestContent.Loading
        }
        else -> TopicAiSuggestContent.Results
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AiTopicSelectionScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String>,
    viewModel: AiTopicSelectionViewModel = hiltViewModel(),
) {
    val skipLoading = data["skipLoading"] == "true"
    val query = data["query"].orEmpty()
    val isHomeAiRecommendLaunch = data["mode"] == "ai_topic_recommend"
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = true){
        navigate(Screen.Home, emptyMap())
    }

    LaunchedEffect(query, skipLoading) {
        viewModel.loadSuggestions(query = query, forceRefresh = !skipLoading)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is TopicAiSuggestEffect.NavigateToTopicDetail -> navigate(
                    Screen.TopicDetail,
                    mapOf(
                        "topicId" to effect.topicId.toString(),
                        "topicTitle" to effect.topicTitle,
                        "from" to "aiSuggest",
                        "query" to effect.query,
                    ),
                )
            }
        }
    }

    val contentState = resolveTopicAiSuggestContent(
        uiState = uiState,
        preferInitialLoading = isHomeAiRecommendLaunch && !skipLoading,
    )

    AnimatedContent(
        targetState = contentState,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 280,
                    easing = FastOutSlowInEasing,
                ),
            ) + slideInHorizontally(
                animationSpec = tween(
                    durationMillis = HomePortalTransition.MainScreenCrossfadeMillis,
                    easing = FastOutSlowInEasing,
                ),
                initialOffsetX = { it / 5 },
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = 220,
                    easing = FastOutSlowInEasing,
                ),
            ) + slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = 280,
                    easing = FastOutSlowInEasing,
                ),
                targetOffsetX = { -it / 8 },
            )
        },
        label = "topic_ai_suggest_content",
    ) { target ->
        when (target) {
            TopicAiSuggestContent.Creating -> {
                val selectedSuggestion = uiState.suggestions.firstOrNull { it.id == uiState.selectedSuggestionId }
                AnalyzingScreen(
                    navigate = navigate,
                    data = mapOf(
                        "selectedCount" to (selectedSuggestion?.itemCount?.toString() ?: "0"),
                        "topicName" to (selectedSuggestion?.title ?: "주제"),
                    ),
                    autoNavigate = false,
                    steps = uiState.creatingSteps.ifEmpty { null },
                    onGoBack = { viewModel.resetCreatingSteps() },
                )
            }

            TopicAiSuggestContent.Loading -> {
                AiSuggestLoading(
                    query = query,
                    steps = uiState.loadingSteps,
                    onClose = { navigate(Screen.Home, emptyMap()) },
                    onRetry = { viewModel.resetLoadingSteps() },
                )
            }

            TopicAiSuggestContent.Results -> TopicAiSuggestResults(
                query = query,
                uiState = uiState,
                navigate = navigate,
                onSelectSuggestion = viewModel::selectSuggestion,
            )
        }
    }
}

@Composable
private fun TopicAiSuggestResults(
    query: String,
    uiState: TopicAiSuggestUiState,
    navigate: (Screen, Map<String, String>) -> Unit,
    onSelectSuggestion: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Surface),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkGradient)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { navigate(Screen.Home, emptyMap()) },
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF93C5FD), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI 추천 주제", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                ) {
                    Text("분석 결과", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (query.isBlank()) "수집한 전체 데이터에서 ${uiState.suggestionCount}개의 주제를 찾았어요."
                        else "\"$query\"에 맞는 주제 ${uiState.suggestionCount}개를 찾았어요.",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (uiState.isCreatingTopic) "선택한 주제로 실행 항목을 준비하고 있어요."
                        else "하나를 선택하면 AI가 실행 항목을 준비해요.",
                        color = Color(0xFFA5B4FC),
                        fontSize = 10.sp,
                    )
                }
            }
        }
        if (uiState.errorMessage != null) {
            item {
                Text(
                    text = uiState.errorMessage.orEmpty(),
                    color = AppColors.Slate400,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                )
            }
        }
        itemsIndexed(uiState.suggestions) { _, suggestion ->
            SuggestedTopicCard(
                topic = suggestion.toTopicAiSuggestionCardUi(),
                enabled = uiState.canSelectSuggestion,
                isCreating = uiState.isCreatingSuggestion(suggestion.id),
            ) {
                onSelectSuggestion(suggestion.id)
            }
        }
    }
}

private data class LoadingPortalParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
)

@Composable
private fun AiSuggestLoading(
    query: String,
    steps: List<AnalysisStep>,
    onClose: () -> Unit,
    onRetry: () -> Unit,
) {
    val useExternalSteps = steps.isNotEmpty()
    var stepIndex by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }

    val defaultLoadingSteps = HomePortalTransition.LoadingSteps

    LaunchedEffect(useExternalSteps) {
        if (useExternalSteps) return@LaunchedEffect
        stepIndex = 0
        done = false
        delay(850)
        stepIndex = 1
        delay(850)
        stepIndex = 2
        delay(850)
        done = true
    }

    val displaySteps: List<AnalysisStep> = if (useExternalSteps) {
        steps
    } else {
        defaultLoadingSteps.mapIndexed { index, label ->
            AnalysisStep(
                label = label,
                status = when {
                    done || index < stepIndex -> StepStatus.Success
                    index == stepIndex && !done -> StepStatus.Running
                    else -> StepStatus.Pending
                },
            )
        }
    }

    val hasFailure = displaySteps.any { it.status == StepStatus.Failed }
    val allSuccess = displaySteps.all { it.status == StepStatus.Success }

    // 진행률 계산
    val progress = if (useExternalSteps) {
        val total = steps.size
        val completed = steps.count { it.status == StepStatus.Success }
        val runningFraction = steps.indexOfFirst { it.status == StepStatus.Running }
            .takeIf { it >= 0 }?.let { 0.5f / total } ?: 0f
        (completed.toFloat() / total) + runningFraction
    } else {
        val total = defaultLoadingSteps.size
        val completed = stepIndex.coerceAtMost(total - 1)
        val runningFraction = if (!done) 0.5f / total else 0f
        (completed.toFloat() / total) + runningFraction
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGradient),
    ) {
        // 포털 애니메이션
        LoadingPortalAnimation(
            progress = progress.coerceIn(0f, 1f),
            isRunning = !allSuccess && !hasFailure,
            isCompleted = allSuccess,
            isFailed = hasFailure,
            modifier = Modifier.fillMaxSize(),
        )

        // 닫기 버튼 (포털 위에 오버레이)
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Button(
                onClick = onClose,
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        // 하단 콘텐츠
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(260)) + slideInVertically { it / 8 },
            exit = fadeOut(tween(180)) + slideOutVertically { -it / 8 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp)
                .padding(bottom = 54.dp),
        ) {
            LoadingBottomContent(
                query = query,
                displaySteps = displaySteps,
                hasFailure = hasFailure,
                allSuccess = allSuccess,
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LoadingBottomContent(
    query: String,
    displaySteps: List<AnalysisStep>,
    hasFailure: Boolean,
    allSuccess: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when {
        hasFailure -> "분석에 실패했어요."
        allSuccess -> "분석이 완료됐어요."
        else -> "AI 가 분석 중이에요."
    }

    val body = when {
        hasFailure -> "오류가 발생했습니다.\n다시 시도해주세요."
        allSuccess -> "추천 주제를 표시하는 중..."
        query.isNotBlank() -> "\"$query\"에 맞는 주제를\n찾고 있어요."
        else -> "수집한 데이터에서\n주제를 찾고 있어요."
    }

    AnimatedContent(
        targetState = Triple(title, body, hasFailure),
        transitionSpec = {
            fadeIn(tween(260)) + slideInVertically { it / 8 } togetherWith
                fadeOut(tween(180)) + slideOutVertically { -it / 8 }
        },
        label = "loading-bottom-content",
        modifier = modifier,
    ) { (animatedTitle, animatedBody, animatedHasFailure) ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = animatedTitle,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = animatedBody,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(24.dp))

            // 단계 표시 (체크박스 유지)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                displaySteps.forEachIndexed { index, step ->
                    val isActive = step.status == StepStatus.Running
                    val isCompleted = step.status == StepStatus.Success
                    val isFailed = step.status == StepStatus.Failed

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LoadingStepIndicator(
                            isActive = isActive,
                            isCompleted = isCompleted,
                            isFailed = isFailed,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                step.label,
                                color = when {
                                    isFailed -> Color(0xFFFCA5A5)
                                    step.status == StepStatus.Pending -> Color.White.copy(alpha = 0.34f)
                                    else -> Color.White
                                },
                                fontSize = 13.sp,
                                fontWeight = when {
                                    isActive -> FontWeight.ExtraBold
                                    isFailed -> FontWeight.ExtraBold
                                    isCompleted -> FontWeight.SemiBold
                                    else -> FontWeight.SemiBold
                                },
                            )
                            if (step.detail != null && (isActive || isFailed)) {
                                Text(
                                    step.detail,
                                    color = when {
                                        isFailed -> Color(0xFFFCA5A5).copy(alpha = 0.60f)
                                        else -> Color(0xFF93C5FD).copy(alpha = 0.55f)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            // 실패 시 재시도 버튼
            if (animatedHasFailure) {
                Spacer(Modifier.height(14.dp))
                val failedLabel = displaySteps.firstOrNull { it.status == StepStatus.Failed }?.label ?: "이전 단계"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                        .clickable { onRetry() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "\"$failedLabel\" 단계부터 다시 시도",
                            color = Color(0xFF93C5FD),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                when {
                    hasFailure -> "오류가 발생했습니다. 다시 시도해주세요."
                    allSuccess -> "추천 주제를 표시하는 중..."
                    else -> "AI 에이전트가 분석 중입니다..."
                },
                color = if (hasFailure) Color(0xFFFCA5A5).copy(alpha = 0.70f) else Color(0xFF93C5FD).copy(alpha = 0.50f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LoadingStepIndicator(
    isActive: Boolean,
    isCompleted: Boolean,
    isFailed: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading-pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loadingPulseAlpha",
    )

    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(
                when {
                    isCompleted -> Color(0xFF10B981)
                    isFailed -> Color(0xFFEF4444)
                    isActive -> Color(0xFF93C5FD).copy(alpha = 0.22f)
                    else -> Color.White.copy(alpha = 0.08f)
                },
            )
            .border(
                width = when {
                    isActive -> 1.5.dp
                    isFailed -> 1.5.dp
                    else -> 0.dp
                },
                color = when {
                    isActive -> Color(0xFF93C5FD)
                    isFailed -> Color(0xFFEF4444)
                    else -> Color.Transparent
                },
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isCompleted -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            isFailed -> Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
            isActive -> Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF93C5FD))
                    .graphicsLayer { alpha = pulseAlpha },
            )
        }
    }
}

@Composable
private fun LoadingPortalAnimation(
    progress: Float,
    isRunning: Boolean,
    isCompleted: Boolean,
    isFailed: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "loading-portal")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loading-portal-rotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading-portal-pulse",
    )
    val particles = remember {
        listOf(
            LoadingPortalParticle(0.20f, 0.24f, 2.4f, 0.00f),
            LoadingPortalParticle(0.34f, 0.18f, 1.8f, 0.13f),
            LoadingPortalParticle(0.57f, 0.20f, 2.2f, 0.27f),
            LoadingPortalParticle(0.78f, 0.27f, 1.7f, 0.42f),
            LoadingPortalParticle(0.18f, 0.43f, 1.9f, 0.55f),
            LoadingPortalParticle(0.31f, 0.54f, 2.6f, 0.68f),
            LoadingPortalParticle(0.64f, 0.52f, 2.0f, 0.81f),
            LoadingPortalParticle(0.83f, 0.48f, 2.4f, 0.94f),
            LoadingPortalParticle(0.24f, 0.72f, 1.7f, 0.07f),
            LoadingPortalParticle(0.46f, 0.78f, 2.3f, 0.21f),
            LoadingPortalParticle(0.70f, 0.74f, 1.8f, 0.36f),
            LoadingPortalParticle(0.84f, 0.66f, 2.1f, 0.49f),
            LoadingPortalParticle(0.39f, 0.34f, 1.6f, 0.62f),
            LoadingPortalParticle(0.52f, 0.40f, 2.5f, 0.75f),
            LoadingPortalParticle(0.60f, 0.63f, 1.7f, 0.88f),
            LoadingPortalParticle(0.42f, 0.66f, 2.0f, 0.97f),
        )
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height * 0.32f)
        val baseRadius = min(size.width, size.height) * 0.18f
        val pulseRadius = baseRadius * (1f + pulse * 0.06f)
        val importPull = if (isRunning) 0.18f + pulse * 0.08f else if (isCompleted) 0.09f else 0f

        drawCircle(
            color = if (isFailed) Color(0xFFEF4444).copy(alpha = 0.12f + pulse * 0.06f)
            else AppColors.Blue.copy(alpha = 0.16f + pulse * 0.08f),
            radius = pulseRadius * 1.75f,
            center = center,
        )
        drawCircle(
            color = if (isFailed) Color(0xFFFCA5A5).copy(alpha = 0.08f + pulse * 0.03f)
            else Color(0xFF67E8F9).copy(alpha = 0.10f + pulse * 0.04f),
            radius = pulseRadius * 1.25f,
            center = center,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = pulseRadius * 0.68f,
            center = center,
        )

        drawCircle(
            color = if (isFailed) Color(0xFFEF4444).copy(alpha = 0.34f) else Color.White.copy(alpha = 0.34f),
            radius = pulseRadius,
            center = center,
            style = Stroke(width = 2.2.dp.toPx()),
        )

        drawArc(
            color = Color.White.copy(alpha = 0.16f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius * 1.34f, center.y - pulseRadius * 1.34f),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2.68f, pulseRadius * 2.68f),
            style = Stroke(width = 3.4.dp.toPx(), cap = StrokeCap.Round),
        )

        val progressColor = if (isFailed) Color(0xFFFCA5A5).copy(alpha = 0.92f)
        else Color(0xFFBFEAFF).copy(alpha = 0.92f)
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius * 1.34f, center.y - pulseRadius * 1.34f),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2.68f, pulseRadius * 2.68f),
            style = Stroke(width = 5.0.dp.toPx(), cap = StrokeCap.Round),
        )

        val arcColor = if (isFailed) Color(0xFFFCA5A5).copy(alpha = 0.86f)
        else Color(0xFF9DD9FF).copy(alpha = 0.86f)
        drawArc(
            color = arcColor,
            startAngle = rotation * 360f,
            sweepAngle = 114f,
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius, center.y - pulseRadius),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2f, pulseRadius * 2f),
            style = Stroke(width = 5.2.dp.toPx(), cap = StrokeCap.Round),
        )

        val outerArcColor = if (isFailed) Color(0xFFEF4444).copy(alpha = 0.64f)
        else AppColors.Blue.copy(alpha = 0.64f)
        drawArc(
            color = outerArcColor,
            startAngle = rotation * -360f + 180f,
            sweepAngle = 76f,
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius * 1.18f, center.y - pulseRadius * 1.18f),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2.36f, pulseRadius * 2.36f),
            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
        )

        val particlePositions = particles.map { particle ->
            val wave = sin((rotation + particle.phase) * 2f * PI).toFloat()
            val driftX = cos((rotation + particle.phase) * 2f * PI).toFloat() * 10f
            val driftY = wave * 8f
            val original = Offset(
                x = particle.x * size.width + driftX,
                y = particle.y * size.height + driftY,
            )
            Offset(
                x = original.x + (center.x - original.x) * importPull,
                y = original.y + (center.y - original.y) * importPull,
            )
        }

        for (index in 0 until 6) {
            val start = particlePositions[index]
            val end = particlePositions[(index + 2) % particlePositions.size]
            drawLine(
                color = Color.White.copy(alpha = 0.07f + pulse * 0.03f),
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
            )
        }

        particles.forEachIndexed { index, particle ->
            val position = particlePositions[index]
            val alpha = 0.48f + (sin((rotation + particle.phase) * 2f * PI).toFloat() + 1f) * 0.18f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = particle.radius.dp.toPx(),
                center = position,
            )
            drawCircle(
                color = if (isFailed) Color(0xFFFCA5A5).copy(alpha = alpha * 0.22f)
                else Color(0xFF9DD9FF).copy(alpha = alpha * 0.22f),
                radius = (particle.radius * 3.2f).dp.toPx(),
                center = position,
            )
        }

        if (isCompleted) {
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 36.sp.toPx()
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD,
                    )
                    alpha = 230
                }
                drawText(
                    "✓",
                    center.x,
                    center.y - (paint.descent() + paint.ascent()) / 2f,
                    paint,
                )
            }
        } else if (isFailed) {
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 36.sp.toPx()
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD,
                    )
                    alpha = 200
                }
                drawText(
                    "✕",
                    center.x,
                    center.y - (paint.descent() + paint.ascent()) / 2f,
                    paint,
                )
            }
        }
    }
}

@Composable
private fun SuggestedTopicCard(
    topic: TopicAiSuggestionCardUi,
    enabled: Boolean,
    isCreating: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AppColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(topic.accentBg, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(topic.icon, null, tint = topic.color, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = topic.title,
                            color = AppColors.Slate800,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = AppColors.Slate200, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = if (isCreating) "실행 항목 생성 중입니다." else topic.description,
                        color = AppColors.Slate500,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFBFF))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    topic.dataTypes.forEach { Pill(it, topic.accentBg, topic.color) }
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                ) {
                    topic.tags.take(1).forEach {
                        Text(
                            text = "#$it",
                            color = AppColors.Slate400,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
