package com.samsung.smartclipboard.presentation.main.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.Pill
import com.samsung.smartclipboard.presentation.Screen

@Composable
fun HistoryScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface),
        ) {
            // 상단 네비게이션 바
            if (uiState.selectMode) {
                HistorySelectHeader(
                    selectedCount = uiState.selectedIds.size,
                    onSelectAll = { viewModel.selectAll() },
                    onCancel = { viewModel.exitSelectMode() },
                )
            } else {
                HistoryNormalHeader(
                    topicCount = uiState.topics.size,
                    onBack = { navigate(Screen.Home, emptyMap()) },
                )
            }

            when {
                uiState.isLoading -> {
                    Text(
                        "불러오는 중...",
                        color = AppColors.Slate400,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                uiState.errorMessage != null -> {
                    Text(
                        uiState.errorMessage.orEmpty(),
                        color = AppColors.Slate400,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                uiState.topics.isEmpty() -> {
                    Text(
                        "아직 히스토리가 없습니다. 데이터를 분석하면 여기에 기록됩니다.",
                        color = AppColors.Slate400,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                items(uiState.topics, key = { it.id }) { topic ->
                    val selected = topic.id in uiState.selectedIds
                    HistoryTopicCard(
                        topic = topic,
                        selectMode = uiState.selectMode,
                        selected = selected,
                        onToggle = { viewModel.toggleSelected(topic.id) },
                        onLongClick = { viewModel.enterSelectMode(topic.id) },
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

        // 선택 모드 시 하단 삭제 버튼
        if (uiState.selectMode && uiState.selectedIds.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shadowElevation = 8.dp,
                color = Color.White,
            ) {
                Button(
                    onClick = { viewModel.deleteSelected() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${uiState.selectedIds.size}개 삭제", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun HistoryNormalHeader(
    topicCount: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onBack,
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = AppColors.Slate500),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("히스토리", color = AppColors.Slate800, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text("이전 AI 정리 작업 ${topicCount}개", color = AppColors.Slate400, fontSize = 10.sp)
        }
    }
}

@Composable
private fun HistorySelectHeader(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.BlueSoft)
            .border(1.dp, Color(0xFFBFDBFE))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 왼쪽: 전체 선택
        Text(
            "전체 선택",
            color = AppColors.Blue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onSelectAll),
        )

        // 가운데: 선택 개수
        Text(
            "${selectedCount}개 선택됨",
            modifier = Modifier.weight(1f),
            color = AppColors.Slate800,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )

        // 오른쪽: 취소
        Row(
            modifier = Modifier.clickable(onClick = onCancel),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Close, null, tint = AppColors.Slate500, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("취소", color = AppColors.Slate500, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryTopicCard(
    topic: HistoryTopicUi,
    selectMode: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
    onDetail: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .then(
                if (selectMode) {
                    Modifier.clickable(onClick = onToggle)
                } else {
                    Modifier.clickable(onClick = onDetail)
                }
            )
            .then(
                if (!selectMode) {
                    Modifier.combinedClickable(
                        onClick = onDetail,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) AppColors.Blue else AppColors.Border,
        ),
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
                // 선택 모드 시 체크 인디케이터
                if (selectMode) {
                    SelectionIndicator(checked = selected)
                    Spacer(Modifier.width(10.dp))
                }

                Box(Modifier.size(42.dp).background(AppColors.BlueSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, null, tint = AppColors.Blue, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(topic.title, color = AppColors.Slate800, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${topic.date} · 데이터 ${topic.dataCount}개 · 초안 ${topic.drafts.size}개", color = AppColors.Slate400, fontSize = 10.sp)
                }
                if (!selectMode) {
                    val executedCount = topic.drafts.count { it.status == "실행됨" }
                    if (executedCount > 0) {
                        Pill("${executedCount}개 실행됨", Color(0xFFD1FAE5), AppColors.Green)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppColors.Slate200, modifier = Modifier.size(16.dp))
                }
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

@Composable
private fun SelectionIndicator(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(if (checked) AppColors.Blue else Color.White.copy(alpha = 0.85f), CircleShape)
            .border(
                width = 1.dp,
                color = if (checked) AppColors.Blue else AppColors.Border,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

enum class StatusColorType(val bg: Color, val text: Color) {
    DRAFT(Color(0xFFDBEAFE), AppColors.Blue),
    EDITED(Color(0xFFFEF3C7), Color(0xFFD97706)),
    EXECUTED(Color(0xFFD1FAE5), AppColors.Green),
    DISMISSED(Color(0xFFF1F5F9), AppColors.Slate400),
}

@Composable
private fun StatusPill(text: String, type: StatusColorType) {
    Pill(text, type.bg, type.text)
}

