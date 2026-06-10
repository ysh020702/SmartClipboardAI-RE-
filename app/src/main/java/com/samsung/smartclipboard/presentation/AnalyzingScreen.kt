package com.samsung.smartclipboard.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.graphicsLayer
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
    steps: List<AnalysisStep>? = null,
    onGoBack: (() -> Unit)? = null,
) {
    val topicName = data["topicName"]?.ifBlank { "수집한 항목" } ?: "수집한 항목"

    // steps가 제공되면 실제 진행상황 사용, 아니면 기존 delay 기반 동작
    val useExternalSteps = steps != null
    var stepIndex by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var hasFailure by remember { mutableStateOf(false) }
    var failedStepIndex by remember { mutableStateOf(-1) }

    // 외부 steps가 없을 때 기존 delay 기반 동작
    LaunchedEffect(useExternalSteps) {
        if (useExternalSteps) return@LaunchedEffect
        stepIndex = 0
        done = false
        hasFailure = false
        failedStepIndex = -1
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

    // 외부 steps 기반으로 상태 파생
    val displaySteps: List<AnalysisStep> = if (useExternalSteps) {
        steps!!
    } else {
        analyzingSteps.mapIndexed { index, label ->
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

    // 외부 steps에서 실패 감지
    LaunchedEffect(steps) {
        if (steps != null) {
            val failedIdx = steps.indexOfFirst { it.status == StepStatus.Failed }
            if (failedIdx >= 0) {
                hasFailure = true
                failedStepIndex = failedIdx
            } else {
                hasFailure = false
                failedStepIndex = -1
            }
            // 모든 단계 성공 시 완료 처리
            if (steps.all { it.status == StepStatus.Success } && !done) {
                done = true
                if (autoNavigate) {
                    delay(650)
                    navigate(Screen.TopicDetail, mapOf("topicId" to "1", "topicTitle" to topicName))
                }
            }
        }
    }

    val allSuccess = if (useExternalSteps) {
        steps?.all { it.status == StepStatus.Success } == true
    } else {
        done
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGradient)
            .padding(horizontal = 28.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 실패 시 뒤로가기 버튼
        if (hasFailure) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGoBack?.invoke() ?: navigate(Screen.Home, emptyMap()) }
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "이전으로 돌아가기",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(if (hasFailure) 24.dp else 54.dp))

        Spacer(Modifier.height(32.dp))
        Text(topicName, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)

        if (hasFailure) {
            Text("분석에 실패했습니다", color = Color(0xFFFCA5A5), fontSize = 12.sp)
        } else {
            Text("AI 가 분석 중입니다", color = Color(0xFFA5B4FC), fontSize = 12.sp)
        }

        Spacer(Modifier.height(42.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            displaySteps.forEachIndexed { index, step ->
                val isActive = step.status == StepStatus.Running
                val isCompleted = step.status == StepStatus.Success
                val isFailed = step.status == StepStatus.Failed

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepIndicator(
                        isActive = isActive,
                        isCompleted = isCompleted,
                        isFailed = isFailed,
                    )
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
                                isCompleted -> FontWeight.SemiBold
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

        // 실패 시 재시도 버튼
        if (hasFailure) {
            val failedLabel = displaySteps.getOrNull(failedStepIndex)?.label ?: "이전 단계"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                    .clickable { onGoBack?.invoke() ?: navigate(Screen.Home, emptyMap()) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
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
                allSuccess -> "초안 화면으로 이동 중..."
                else -> "AI 에이전트가 분석 중입니다..."
            },
            color = if (hasFailure) Color(0xFFFCA5A5).copy(alpha = 0.70f) else Color(0xFF93C5FD).copy(alpha = 0.50f),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun StepIndicator(
    isActive: Boolean,
    isCompleted: Boolean,
    isFailed: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

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
            isCompleted -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
            isFailed -> {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
            isActive -> {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF93C5FD))
                        .graphicsLayer { alpha = pulseAlpha },
                )
            }
        }
    }
}