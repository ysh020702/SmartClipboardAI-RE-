package com.samsung.smartclipboard.presentation.main.aitopicselection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.smartclipboard.presentation.AnalysisStep
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.DarkGradient
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.StepStatus
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val analyzingSteps = listOf(
    "선택한 데이터 불러오는 중",
    "텍스트와 이미지 분석 중",
    "실행 항목 준비 중",
)

private data class PortalParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
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

    // 진행률 계산 (포털 애니메이션용)
    val progress = if (useExternalSteps) {
        val total = steps?.size ?: 1
        val completed = steps?.count { it.status == StepStatus.Success } ?: 0
        val runningFraction = steps?.indexOfFirst { it.status == StepStatus.Running }
            ?.takeIf { it >= 0 }?.let { 0.5f / total } ?: 0f
        (completed.toFloat() / total) + runningFraction
    } else {
        val total = analyzingSteps.size
        val completed = stepIndex.coerceAtMost(total - 1)
        val runningFraction = if (!done) 0.5f / total else 0f
        (completed.toFloat() / total) + runningFraction
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGradient),
    ) {
        PortalAnalysisAnimation(
            progress = progress.coerceIn(0f, 1f),
            isRunning = !allSuccess && !hasFailure,
            isCompleted = allSuccess,
            isFailed = hasFailure,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(260)) + slideInVertically { it / 8 },
            exit = fadeOut(tween(180)) + slideOutVertically { -it / 8 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp)
                .padding(bottom = 54.dp),
        ) {
            AnalysisBottomContent(
                topicName = topicName,
                displaySteps = displaySteps,
                hasFailure = hasFailure,
                failedStepIndex = failedStepIndex,
                allSuccess = allSuccess,
                onGoBack = onGoBack,
                onNavigateHome = { navigate(Screen.Home, emptyMap()) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AnalysisBottomContent(
    topicName: String,
    displaySteps: List<AnalysisStep>,
    hasFailure: Boolean,
    failedStepIndex: Int,
    allSuccess: Boolean,
    onGoBack: (() -> Unit)?,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when {
        hasFailure -> "분석에 실패했어요."
        allSuccess -> "분석이 완료됐어요."
        else -> "AI 가 분석 중이에요."
    }

    val body = when {
        hasFailure -> "오류가 발생했습니다.\n다시 시도해주세요."
        allSuccess -> "결과 화면으로 이동 중..."
        else -> "수집한 데이터에서\n실행 항목을 준비하고 있어요."
    }

    AnimatedContent(
        targetState = Triple(title, body, hasFailure),
        transitionSpec = {
            fadeIn(tween(260)) + slideInVertically { it / 8 } togetherWith
                fadeOut(tween(180)) + slideOutVertically { -it / 8 }
        },
        label = "analysis-bottom-content",
        modifier = modifier,
    ) { (animatedTitle, animatedBody, animatedHasFailure) ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = animatedTitle,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = animatedBody,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(24.dp))

            // 실패 시 홈으로 돌아가기 버튼
            if (animatedHasFailure) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateHome() }
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
                        "홈으로 돌아가기",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            // 단계 표시 (체크박스 유지)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
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
                        Spacer(Modifier.width(10.dp))
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

            // 실패 시 재시도 버튼
            if (animatedHasFailure) {
                Spacer(Modifier.height(14.dp))
                val failedLabel = displaySteps.getOrNull(failedStepIndex)?.label ?: "이전 단계"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                        .clickable { onGoBack?.invoke() ?: onNavigateHome() }
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
            }

            Spacer(Modifier.height(8.dp))

            Text(
                when {
                    hasFailure -> "오류가 발생했습니다. 다시 시도해주세요."
                    allSuccess -> "결과 화면으로 이동 중..."
                    else -> "AI 에이전트가 분석 중입니다..."
                },
                color = if (hasFailure) Color(0xFFFCA5A5).copy(alpha = 0.70f) else Color(0xFF93C5FD).copy(alpha = 0.50f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }
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

@Composable
private fun PortalAnalysisAnimation(
    progress: Float,
    isRunning: Boolean,
    isCompleted: Boolean,
    isFailed: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "portal-analysis")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "portal-rotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "portal-pulse",
    )
    val particles = remember {
        listOf(
            PortalParticle(0.20f, 0.24f, 2.4f, 0.00f),
            PortalParticle(0.34f, 0.18f, 1.8f, 0.13f),
            PortalParticle(0.57f, 0.20f, 2.2f, 0.27f),
            PortalParticle(0.78f, 0.27f, 1.7f, 0.42f),
            PortalParticle(0.18f, 0.43f, 1.9f, 0.55f),
            PortalParticle(0.31f, 0.54f, 2.6f, 0.68f),
            PortalParticle(0.64f, 0.52f, 2.0f, 0.81f),
            PortalParticle(0.83f, 0.48f, 2.4f, 0.94f),
            PortalParticle(0.24f, 0.72f, 1.7f, 0.07f),
            PortalParticle(0.46f, 0.78f, 2.3f, 0.21f),
            PortalParticle(0.70f, 0.74f, 1.8f, 0.36f),
            PortalParticle(0.84f, 0.66f, 2.1f, 0.49f),
            PortalParticle(0.39f, 0.34f, 1.6f, 0.62f),
            PortalParticle(0.52f, 0.40f, 2.5f, 0.75f),
            PortalParticle(0.60f, 0.63f, 1.7f, 0.88f),
            PortalParticle(0.42f, 0.66f, 2.0f, 0.97f),
        )
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height * 0.32f)
        val baseRadius = min(size.width, size.height) * 0.18f
        val pulseRadius = baseRadius * (1f + pulse * 0.06f)
        val importPull = if (isRunning) 0.18f + pulse * 0.08f else if (isCompleted) 0.09f else 0f

        // 배경 글로우
        drawCircle(
            color = if (isFailed) Color(0xFFEF4444).copy(alpha = 0.12f + pulse * 0.06f)
            else AppColors.Blue.copy(alpha = 0.16f + pulse * 0.08f),
            radius = pulseRadius * 1.75f,
            center = center,
        )
        drawCircle(
            color = if (isFailed) Color(0xFFFCA5A5).copy(alpha = 0.08f + pulse * 0.03f)
            else Color(0xFF67E8F9).copy(alpha = 0.10f + pulse * 0.04f),
            radius = pulseRadius * 1.25f,
            center = center,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = pulseRadius * 0.68f,
            center = center,
        )

        // 메인 원 테두리
        drawCircle(
            color = if (isFailed) Color(0xFFEF4444).copy(alpha = 0.34f) else Color.White.copy(alpha = 0.34f),
            radius = pulseRadius,
            center = center,
            style = Stroke(width = 2.2.dp.toPx()),
        )

        // 외곽 링
        drawArc(
            color = Color.White.copy(alpha = 0.16f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius * 1.34f, center.y - pulseRadius * 1.34f),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2.68f, pulseRadius * 2.68f),
            style = Stroke(width = 3.4.dp.toPx(), cap = StrokeCap.Round),
        )

        // 진행률 호
        val progressColor = if (isFailed) Color(0xFFFCA5A5).copy(alpha = 0.92f)
        else Color(0xFFBFEAFF).copy(alpha = 0.92f)
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius * 1.34f, center.y - pulseRadius * 1.34f),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2.68f, pulseRadius * 2.68f),
            style = Stroke(width = 5.0.dp.toPx(), cap = StrokeCap.Round),
        )

        // 회전하는 내부 호
        val arcColor = if (isFailed) Color(0xFFFCA5A5).copy(alpha = 0.86f)
        else Color(0xFF9DD9FF).copy(alpha = 0.86f)
        drawArc(
            color = arcColor,
            startAngle = rotation * 360f,
            sweepAngle = 114f,
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius, center.y - pulseRadius),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2f, pulseRadius * 2f),
            style = Stroke(width = 5.2.dp.toPx(), cap = StrokeCap.Round),
        )

        val outerArcColor = if (isFailed) Color(0xFFEF4444).copy(alpha = 0.64f)
        else AppColors.Blue.copy(alpha = 0.64f)
        drawArc(
            color = outerArcColor,
            startAngle = rotation * -360f + 180f,
            sweepAngle = 76f,
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius * 1.18f, center.y - pulseRadius * 1.18f),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2.36f, pulseRadius * 2.36f),
            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
        )

        // 파티클
        val particlePositions = particles.map { particle ->
            val wave = sin((rotation + particle.phase) * 2f * PI).toFloat()
            val driftX = cos((rotation + particle.phase) * 2f * PI).toFloat() * 10f
            val driftY = wave * 8f
            val original = Offset(
                x = particle.x * size.width + driftX,
                y = particle.y * size.height + driftY,
            )
            Offset(
                x = original.x + (center.x - original.x) * importPull,
                y = original.y + (center.y - original.y) * importPull,
            )
        }

        for (index in 0 until 6) {
            val start = particlePositions[index]
            val end = particlePositions[(index + 2) % particlePositions.size]
            drawLine(
                color = Color.White.copy(alpha = 0.07f + pulse * 0.03f),
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
            )
        }

        particles.forEachIndexed { index, particle ->
            val position = particlePositions[index]
            val alpha = 0.48f + (sin((rotation + particle.phase) * 2f * PI).toFloat() + 1f) * 0.18f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = particle.radius.dp.toPx(),
                center = position,
            )
            drawCircle(
                color = if (isFailed) Color(0xFFFCA5A5).copy(alpha = alpha * 0.22f)
                else Color(0xFF9DD9FF).copy(alpha = alpha * 0.22f),
                radius = (particle.radius * 3.2f).dp.toPx(),
                center = position,
            )
        }

        // 완료 시 체크마크 또는 실패 시 X 표시
        if (isCompleted) {
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 36.sp.toPx()
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD,
                    )
                    alpha = 230
                }
                drawText(
                    "✓",
                    center.x,
                    center.y - (paint.descent() + paint.ascent()) / 2f,
                    paint,
                )
            }
        } else if (isFailed) {
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 36.sp.toPx()
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD,
                    )
                    alpha = 200
                }
                drawText(
                    "✕",
                    center.x,
                    center.y - (paint.descent() + paint.ascent()) / 2f,
                    paint,
                )
            }
        }
    }
}