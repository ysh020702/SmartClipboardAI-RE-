package com.samsung.smartclipboard.presentation.main.topicaisuggest

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
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.DarkGradient
import com.samsung.smartclipboard.presentation.Pill
import com.samsung.smartclipboard.presentation.Screen
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

    if (uiState.isLoading) {
        AiSuggestLoading(query = query) { navigate(Screen.Home, emptyMap()) }
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
private fun AiSuggestLoading(query: String, onClose: () -> Unit) {
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
        Spacer(Modifier.height(132.dp))
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .border(2.dp, Color(0xFF93C5FD).copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF93C5FD), modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("AI가 분석 중입니다", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        if (query.isNotBlank()) {
            Text("\"$query\"", color = Color(0xFF93C5FD), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(28.dp))
        listOf("수집 데이터 확인 중", "패턴 분류 중", "추천 주제 준비 중").forEachIndexed { index, label ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
            ) {
                Icon(
                    imageVector = if (index == 0) Icons.Default.Check else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (index == 0) Color(0xFF34D399) else Color(0xFF93C5FD),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(label, color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
