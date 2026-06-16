package com.samsung.smartclipboard.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TasksScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String> = emptyMap(),
) {
    val topics = listOf(Topic("1", "수집한 항목 (5)", 5, "5월 26일 11:34 업데이트", AppColors.Blue, listOf("스크린샷", "회의", "여행")))
    val steps = listOf("수집" to true, "클러스터" to true, "주제" to true, "AI 분석" to false, "결과" to false, "확인" to false, "실행" to false)

    fun navigateBack() {
        if (data["from"] == "homePanel") {
            navigate(Screen.Home, mapOf("openPanel" to "instant"))
        } else {
            navigate(Screen.Home, emptyMap())
        }
    }

    BackHandler {
        navigateBack()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            TasksTopHeader(
                onBack = { navigateBack() },
            )
        }
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Column(Modifier.background(Brush.linearGradient(listOf(AppColors.Blue, AppColors.BlueDeep))).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderOpen, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("모은 주제", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                        Text("선택한 데이터를 주제별로 모아두고, 이후 요약/일정/알림 액션을 붙일 수 있습니다.", color = Color(0xFFC7D2FE), fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp))
                        Button(
                            onClick = { navigate(Screen.Data, emptyMap()) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("데이터 골라 주제 만들기", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("주제 목록", color = AppColors.Slate800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${topics.size}개", color = AppColors.Slate400, fontSize = 10.sp)
                }
                topics.forEach { topic ->
                    TopicRow(topic) { navigate(Screen.TopicDetail, mapOf("topicId" to topic.id)) }
                }
                CardBlock {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountTree, null, tint = AppColors.Blue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI 워크플로우", color = AppColors.Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    FlowStepRow(steps)
                    Text("현재 단계: 주제 생성 완료 · 다음: AI 분석 및 결과 생성", color = AppColors.Slate400, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun TasksTopHeader(onBack: () -> Unit) {
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
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF1F5F9),
                contentColor = AppColors.Slate500,
            ),
            contentPadding = PaddingValues(0.dp),
            border = BorderStroke(1.dp, AppColors.Border),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "작업",
                color = AppColors.Slate800,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "주제별로 수집한 데이터를 정리하세요",
                color = AppColors.Slate400,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
fun TopicRow(topic: Topic, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Default.FolderOpen, topic.color, topic.color.copy(alpha = 0.15f), 46)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(topic.title, color = AppColors.Slate800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Pill("AI 제안", Color(0xFFDBEAFE), AppColors.BlueDeep)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = AppColors.Slate400, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(topic.lastUpdated, color = AppColors.Slate400, fontSize = 10.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    topic.tags.forEach { Pill(it, Color.White, AppColors.Blue) }
                }
            }
            Pill(topic.items.toString(), Color.White, AppColors.Blue)
            Icon(Icons.Default.KeyboardArrowRight, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
        }
    }
}
