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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val analyzingSteps = listOf(
    "선택한 데이터 불러오는 중",
    "텍스트와 이미지 분석 중",
    "실행 초안 준비 중",
)

@Composable
fun AnalyzingScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String>,
    autoNavigate: Boolean = true,
) {
    val topicName = data["topicName"]?.ifBlank { "수집한 항목" } ?: "수집한 항목"
    var stepIndex by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(topicName) {
        delay(850)
        stepIndex = 1
        delay(850)
        stepIndex = 2
        delay(850)
        done = true
        delay(650)
        if (autoNavigate) {
            navigate(Screen.TopicDetail, mapOf("topicId" to "1", "topicTitle" to topicName))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGradient)
            .padding(horizontal = 28.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(54.dp))

        Spacer(Modifier.height(32.dp))
        Text(topicName, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text("AI 가 분석 중입니다", color = Color(0xFFA5B4FC), fontSize = 12.sp)

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
                    Text(
                        label,
                        color = Color.White.copy(alpha = if (index > stepIndex && !done) 0.34f else 1f),
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            if (done) "초안 화면으로 이동 중..." else "AI 에이전트가 분석 중입니다...",
            color = Color(0xFF93C5FD).copy(alpha = 0.50f),
            fontSize = 10.sp
        )
    }
}