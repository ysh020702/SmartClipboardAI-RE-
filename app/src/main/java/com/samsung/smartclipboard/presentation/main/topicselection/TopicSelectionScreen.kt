package com.samsung.smartclipboard.presentation.main.topicselection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.Pill
import com.samsung.smartclipboard.presentation.Screen

@Composable
fun TopicSelectionScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String>,
    viewModel: TopicSelectionViewModel = hiltViewModel(),
) {
    val from = data["from"].orEmpty()
    val query = data["query"].orEmpty()
    val topicId = data["topicId"]?.toLongOrNull() ?: 1L
    val fallbackTitle = data["topicTitle"] ?: "스크린샷 모음"
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title = uiState.topicTitle ?: fallbackTitle

    LaunchedEffect(topicId, fallbackTitle) {
        viewModel.observeTopic(topicId, fallbackTitle)
    }

    fun goBack() {
        when (from) {
            "aiSuggest" -> navigate(Screen.AiSuggest, mapOf("skipLoading" to "true", "query" to query))
            "history" -> navigate(Screen.History, emptyMap())
            else -> navigate(Screen.Home, emptyMap())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Surface),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { goBack() },
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = AppColors.Slate500,
                    ),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = AppColors.Slate800,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "AI가 실행 초안 ${uiState.actionCount}개를 생성했어요",
                        color = AppColors.Slate400,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        item {
            Text(
                text = "실행하기 전에 초안을 확인하세요",
                color = AppColors.Slate500,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }

        when {
            uiState.isLoading -> item {
                TopicSelectionMessage("실행 초안을 불러오는 중입니다")
            }
            uiState.errorMessage != null -> item {
                TopicSelectionMessage(uiState.errorMessage.orEmpty())
            }
            uiState.actions.isEmpty() -> item {
                TopicSelectionMessage("생성된 초안이 없습니다")
            }
        }

        uiState.actions.forEach { action ->
            item {
                TopicActionRow(action = action) {
                    navigate(
                        Screen.ActionReview,
                        mapOf(
                            "actionType" to action.routeActionType,
                            "actionId" to action.actionId.toString(),
                            "topicId" to topicId.toString(),
                            "topicTitle" to title,
                            "from" to from,
                            "query" to query,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicSelectionMessage(text: String) {
    Text(
        text = text,
        color = AppColors.Slate400,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
    )
}

@Composable
private fun TopicActionRow(action: TopicActionCardUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(action.color.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(action.icon, null, tint = action.color, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = action.title.ifBlank { action.typeLabel },
                        color = AppColors.Slate800,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(6.dp))
                    Pill(action.statusLabel, AppColors.BlueSoft, AppColors.Blue)
                }
                Text(
                    text = action.description,
                    color = AppColors.Slate400,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = AppColors.Slate200,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
