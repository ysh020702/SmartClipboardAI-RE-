package com.samsung.smartclipboard.presentation

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class TopicActionCard(
    val id: String,
    val type: String,
    val icon: ImageVector,
    val color: Color,
    val status: String,
    val description: String,
)

private val topicActionCards = listOf(
    TopicActionCard("note", "일일 노트", Icons.Default.Description, AppColors.Blue, "초안", "스크린샷 5개를 일일 노트로 정리해요."),
    TopicActionCard("calendar", "캘린더", Icons.Default.CalendarMonth, Color(0xFF2563EB), "초안", "워크숍과 여행 일정을 추가해요."),
    TopicActionCard("reminder", "리마인더", Icons.Default.Notifications, AppColors.BlueDeep, "초안", "쇼핑과 워크숍 알림을 준비해요."),
    TopicActionCard("share", "공유", Icons.Default.Share, AppColors.Cyan, "초안", "공유할 수 있는 요약 메시지를 만들어요."),
)

@Composable
fun TopicDetailScreen(navigate: (Screen, Map<String, String>) -> Unit, data: Map<String, String>) {
    val from = data["from"].orEmpty()
    val query = data["query"].orEmpty()
    val topicId = data["topicId"] ?: "1"
    val title = data["topicTitle"] ?: "스크린샷 모음"

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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = AppColors.Slate500),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = AppColors.Slate800, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("AI가 실행 초안 ${topicActionCards.size}개를 생성했어요", color = AppColors.Slate400, fontSize = 10.sp)
                }
            }
        }

        item {
            Text(
                "실행하기 전에 초안을 확인하세요.",
                color = AppColors.Slate500,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }

        topicActionCards.forEach { action ->
            item {
                TopicActionRow(action = action) {
                    navigate(
                        Screen.ActionReview,
                        mapOf(
                            "actionType" to action.id,
                            "topicId" to topicId,
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
private fun TopicActionRow(action: TopicActionCard, onClick: () -> Unit) {
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
                    Text(action.type, color = AppColors.Slate800, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Pill(action.status, AppColors.BlueSoft, AppColors.Blue)
                }
                Text(action.description, color = AppColors.Slate400, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = AppColors.Slate200, modifier = Modifier.size(16.dp))
        }
    }
}
