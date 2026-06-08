package com.samsung.smartclipboard.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HistoryScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    onClick = { navigate(Screen.Home, emptyMap()) },
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = AppColors.Slate500),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("히스토리", color = AppColors.Slate800, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text("이전 AI 정리 작업 ${uiState.topics.size}개", color = AppColors.Slate400, fontSize = 10.sp)
                }
            }
        }

        when {
            uiState.isLoading -> item {
                Text(
                    "불러오는 중...",
                    color = AppColors.Slate400,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
            uiState.errorMessage != null -> item {
                Text(
                    uiState.errorMessage.orEmpty(),
                    color = AppColors.Slate400,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
            uiState.topics.isEmpty() -> item {
                Text(
                    "아직 히스토리가 없습니다. 데이터를 분석하면 여기에 기록됩니다.",
                    color = AppColors.Slate400,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        items(uiState.topics, key = { it.id }) { topic ->
            HistoryTopicCard(
                topic = topic,
                onDetail = {
                    navigate(
                        Screen.TopicDetail,
                        mapOf("topicId" to topic.id.toString(), "topicTitle" to topic.title, "from" to "history")
                    )
                },
            )
        }
    }
}

@Composable
private fun HistoryTopicCard(
    topic: HistoryTopicUi,
    onDetail: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onDetail),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(42.dp).background(AppColors.BlueSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, null, tint = AppColors.Blue, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(topic.title, color = AppColors.Slate800, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${topic.date} · 데이터 ${topic.dataCount}개 · 초안 ${topic.drafts.size}개", color = AppColors.Slate400, fontSize = 10.sp)
                }
                val executedCount = topic.drafts.count { it.status == "실행됨" }
                if (executedCount > 0) {
                    Pill("${executedCount}개 실행됨", Color(0xFFD1FAE5), AppColors.Green)
                }
                Icon(Icons.Default.KeyboardArrowRight, null, tint = AppColors.Slate200, modifier = Modifier.size(16.dp))
            }

            if (topic.summary.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 14.dp)
                        .background(Color(0xFFF0F5FF), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = AppColors.Blue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(topic.summary, color = AppColors.BlueDeep, fontSize = 11.sp, modifier = Modifier.weight(1f))
                }
            }

            // 상태 요약 바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val draftCount = topic.drafts.count { it.status == "초안" }
                val editedCount = topic.drafts.count { it.status == "수정됨" }
                val executedCount = topic.drafts.count { it.status == "실행됨" }
                val dismissedCount = topic.drafts.count { it.status == "제외됨" }

                if (draftCount > 0) StatusPill("초안 $draftCount", StatusColorType.DRAFT)
                if (editedCount > 0) StatusPill("수정됨 $editedCount", StatusColorType.EDITED)
                if (executedCount > 0) StatusPill("실행됨 $executedCount", StatusColorType.EXECUTED)
                if (dismissedCount > 0) StatusPill("제외됨 $dismissedCount", StatusColorType.DISMISSED)
            }
        }
    }
}

enum class StatusColorType(val bg: Color, val text: Color) {
    DRAFT(Color(0xFFDBEAFE), AppColors.Blue),           // 초안 = 파란색
    EDITED(Color(0xFFFEF3C7), Color(0xFFD97706)),         // 수정됨 = 노란색
    EXECUTED(Color(0xFFD1FAE5), AppColors.Green),         // 실행됨 = 초록색
    DISMISSED(Color(0xFFF1F5F9), AppColors.Slate400),    // 제외됨 = 회색
}

@Composable
private fun StatusPill(text: String, type: StatusColorType) {
    Pill(text, type.bg, type.text)
}