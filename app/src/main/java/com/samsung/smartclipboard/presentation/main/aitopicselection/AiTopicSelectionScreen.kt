package com.samsung.smartclipboard.presentation.main.aitopicselection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.smartclipboard.presentation.AnalyzingScreen
import com.samsung.smartclipboard.presentation.AnalysisStep
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.DarkGradient
import com.samsung.smartclipboard.presentation.Pill
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.StepStatus
import com.samsung.smartclipboard.presentation.main.home.HomePortalTransition
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

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

@Composable
fun TopicAiSuggestScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String>,
    viewModel: TopicAiSuggestViewModel = hiltViewModel(),
) {
    val skipLoading = data["skipLoading"] == "true"
    val query = data["query"].orEmpty()
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

    if (uiState.isCreatingTopic || uiState.creatingSteps.isNotEmpty()) {
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
        return
    }

    if (uiState.isLoading || uiState.loadingSteps.isNotEmpty()) {
        AiSuggestLoading(
            query = query,
            steps = uiState.loadingSteps,
            onClose = { navigate(Screen.Home, emptyMap()) },
            onRetry = { viewModel.resetLoadingSteps() },
        )
        return
    }

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
                        if (uiState.isCreatingTopic) "선택한 주제로 실행 초안을 준비하고 있어요."
                        else "하나를 선택하면 AI가 실행 초안을 준비해요.",
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
                viewModel.selectSuggestion(suggestion.id)
            }
        }
        item {
            Button(
                onClick = { navigate(Screen.Data, emptyMap()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AppColors.Slate500),
                border = BorderStroke(1.dp, AppColors.Slate200),
            ) {
                Text("데이터 직접 선택", fontWeight = FontWeight.Bold)
            }
        }
    }
}

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
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
        Spacer(Modifier.height(54.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (hasFailure) Color(0xFFEF4444) else Color(0xFF34D399))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (hasFailure) "분석 실패" else "AI 분석 진행 중",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(32.dp))
        Text(
            if (hasFailure) "분석에 실패했습니다" else "AI가 분석 중입니다",
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        if (hasFailure) {
            Text("오류가 발생했습니다. 다시 시도해주세요.", color = Color(0xFFFCA5A5), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        } else if (query.isNotBlank()) {
            Text("\"$query\"", color = Color(0xFFA5B4FC), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        } else {
            Text("수집한 데이터에서 주제를 찾고 있어요", color = Color(0xFFA5B4FC), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(42.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            displaySteps.forEachIndexed { index, step ->
                val isActive = step.status == StepStatus.Running
                val isCompleted = step.status == StepStatus.Success
                val isFailed = step.status == StepStatus.Failed
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            isActive -> Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF93C5FD)))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
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

        Spacer(Modifier.weight(1f))

        // 실패 시 이전으로 돌아가기 버튼
        if (hasFailure) {
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
            Spacer(Modifier.height(8.dp))
        }

        Text(
            when {
                hasFailure -> "오류가 발생했습니다. 다시 시도해주세요."
                allSuccess -> "추천 주제를 표시하는 중..."
                else -> "AI 에이전트가 분석 중입니다..."
            },
            color = if (hasFailure) Color(0xFFFCA5A5).copy(alpha = 0.70f) else Color(0xFF93C5FD).copy(alpha = 0.50f),
            fontSize = 10.sp,
        )
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
                        text = if (isCreating) "실행 초안 생성 중입니다." else topic.description,
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
