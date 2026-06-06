package com.samsung.smartclipboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private data class DraftType(val label: String, val icon: ImageVector, val color: Color)

private val analyzingSteps = listOf(
    "선택한 데이터 불러오는 중",
    "텍스트와 이미지 분석 중",
    "실행 초안 준비 중",
)

private val generatedDrafts = listOf(
    DraftType("일일 노트", Icons.Default.Description, AppColors.Blue),
    DraftType("캘린더", Icons.Default.CalendarMonth, Color(0xFF2563EB)),
    DraftType("리마인더", Icons.Default.Notifications, AppColors.BlueDeep),
    DraftType("공유", Icons.Default.Share, AppColors.Cyan),
)

@Composable
fun AnalyzingScreen(navigate: (Screen, Map<String, String>) -> Unit, data: Map<String, String>) {
    val selectedCount = data["selectedCount"] ?: "0"
    val topicName = data["topicName"]?.ifBlank { "수집한 항목" } ?: "수집한 항목"
    var stepIndex by remember { mutableStateOf(0) }
    var visibleDrafts by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(topicName, selectedCount) {
        delay(850)
        stepIndex = 1
        delay(850)
        stepIndex = 2
        delay(850)
        done = true
        repeat(generatedDrafts.size) { index ->
            visibleDrafts = index + 1
            delay(120)
        }
        delay(650)
        navigate(Screen.TopicDetail, mapOf("topicId" to "1", "topicTitle" to topicName))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGradient)
            .padding(horizontal = 28.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(54.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF34D399)))
            Spacer(Modifier.width(8.dp))
            Text("${selectedCount}개 선택됨", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))
        Text(topicName, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text("AI가 선택한 데이터를 읽고 초안을 생성하고 있어요.", color = Color(0xFFA5B4FC), fontSize = 12.sp)

        Spacer(Modifier.height(42.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            analyzingSteps.forEachIndexed { index, label ->
                val completed = done || index < stepIndex
                val active = index == stepIndex && !done
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    completed -> Color(0xFF10B981)
                                    active -> Color(0xFF93C5FD).copy(alpha = 0.22f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                },
                            )
                            .border(
                                if (active) 1.5.dp else 0.dp,
                                if (active) Color(0xFF93C5FD) else Color.Transparent,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (completed) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        } else if (active) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF93C5FD)))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(label, color = Color.White.copy(alpha = if (index > stepIndex && !done) 0.34f else 1f), fontSize = 13.sp, fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold)
                }
            }
        }

        if (done) {
            Spacer(Modifier.height(36.dp))
            Text("생성된 초안 ${visibleDrafts}개", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                generatedDrafts.take(visibleDrafts).forEach { draft ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(draft.icon, null, tint = Color(0xFF93C5FD), modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(draft.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF34D399)))
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(if (done) "초안 화면으로 이동 중..." else "AI 에이전트가 분석 중입니다...", color = Color(0xFF93C5FD).copy(alpha = 0.50f), fontSize = 10.sp)
    }
}
