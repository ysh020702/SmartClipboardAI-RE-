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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class HistoryDraft(
    val id: String,
    val type: String,
    val icon: ImageVector,
    val color: Color,
    val description: String,
    val status: String,
)

private data class HistoryTopic(
    val id: String,
    val title: String,
    val date: String,
    val dataCount: Int,
    val summary: String,
    val drafts: List<HistoryDraft>,
)

private val historyTopics = listOf(
    HistoryTopic(
        id = "1",
        title = "스크린샷 모음",
        date = "5월 26일 11:34",
        dataCount = 5,
        summary = "회의 자료, 여행 계획, 레시피, 행사 메모가 함께 묶였어요.",
        drafts = listOf(
            HistoryDraft("note", "일일 노트", Icons.Default.Description, AppColors.Blue, "스크린샷 5개를 일일 노트로 정리했어요.", "실행됨"),
            HistoryDraft("calendar", "캘린더", Icons.Default.CalendarMonth, Color(0xFF2563EB), "워크숍과 여행 일정을 추가했어요.", "실행됨"),
            HistoryDraft("reminder", "리마인더", Icons.Default.Notifications, AppColors.BlueDeep, "쇼핑과 워크숍 알림을 준비했어요.", "제외됨"),
            HistoryDraft("share", "공유", Icons.Default.Share, AppColors.Cyan, "메시지 요약을 준비했어요.", "초안"),
        ),
    ),
    HistoryTopic(
        id = "2",
        title = "제주 여행 계획",
        date = "5월 22일 09:18",
        dataCount = 3,
        summary = "여행 일정, 숙소 정보, 저장한 장소를 정리했어요.",
        drafts = listOf(
            HistoryDraft("note", "일일 노트", Icons.Default.Description, AppColors.Blue, "여행 요약을 만들었어요.", "실행됨"),
            HistoryDraft("calendar", "캘린더", Icons.Default.CalendarMonth, Color(0xFF2563EB), "여행 날짜를 등록했어요.", "실행됨"),
            HistoryDraft("share", "공유", Icons.Default.Share, AppColors.Cyan, "여행 메시지 초안을 만들었어요.", "수정됨"),
        ),
    ),
    HistoryTopic(
        id = "3",
        title = "주간 회의 메모",
        date = "5월 19일 14:55",
        dataCount = 4,
        summary = "회의 메모, 다음 할 일, 예정 일정을 정리했어요.",
        drafts = listOf(
            HistoryDraft("note", "일일 노트", Icons.Default.Description, AppColors.Blue, "회의 핵심 내용을 요약했어요.", "실행됨"),
            HistoryDraft("reminder", "리마인더", Icons.Default.Notifications, AppColors.BlueDeep, "후속 알림을 만들었어요.", "실행됨"),
        ),
    ),
)

@Composable
fun HistoryScreen(navigate: (Screen, Map<String, String>) -> Unit) {
    var expandedId by remember { mutableStateOf<String?>(null) }

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
                    Text("이전 AI 정리 작업 ${historyTopics.size}개", color = AppColors.Slate400, fontSize = 10.sp)
                }
            }
        }
        items(historyTopics) { topic ->
            val open = expandedId == topic.id
            HistoryTopicCard(
                topic = topic,
                open = open,
                onToggle = { expandedId = if (open) null else topic.id },
                onDetail = { navigate(Screen.TopicDetail, mapOf("topicId" to topic.id, "topicTitle" to topic.title, "from" to "history")) },
                onDraft = { actionType -> navigate(Screen.ActionReview, mapOf("actionType" to actionType, "topicId" to topic.id, "topicTitle" to topic.title, "from" to "history")) },
            )
        }
    }
}

@Composable
private fun HistoryTopicCard(
    topic: HistoryTopic,
    open: Boolean,
    onToggle: () -> Unit,
    onDetail: () -> Unit,
    onDraft: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
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

            if (open) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .background(Color(0xFFF0F5FF), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = AppColors.Blue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(topic.summary, color = AppColors.BlueDeep, fontSize = 11.sp, modifier = Modifier.weight(1f))
                }
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topic.drafts.forEach { draft ->
                        val clickable = draft.status == "초안" || draft.status == "수정됨"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFAFBFF), RoundedCornerShape(14.dp))
                                .clickable(enabled = clickable) { onDraft(draft.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(34.dp).background(draft.color.copy(alpha = 0.10f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(draft.icon, null, tint = draft.color, modifier = Modifier.size(15.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(draft.type, color = AppColors.Slate800, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                    StatusPill(draft.status)
                                }
                                Text(draft.description, color = AppColors.Slate400, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (clickable) Icon(Icons.Default.KeyboardArrowRight, null, tint = AppColors.Slate200, modifier = Modifier.size(14.dp))
                        }
                    }
                    Button(
                        onClick = onDetail,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.BlueSoft, contentColor = AppColors.Blue),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    ) {
                        Text("전체 상세 보기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val pair = when (status) {
        "실행됨" -> Color(0xFFD1FAE5) to AppColors.Green
        "수정됨" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        "제외됨" -> Color(0xFFF1F5F9) to AppColors.Slate400
        else -> AppColors.BlueSoft to AppColors.Blue
    }
    Pill(status, pair.first, pair.second)
}
