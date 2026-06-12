package com.samsung.smartclipboard.presentation.main.permission

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.DarkGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class OnboardingMediaImportResult(
    val isSuccess: Boolean,
    val importedCount: Int,
    val scannedCount: Int,
    val message: String,
)

private enum class OnboardingMediaImportPhase {
    Preparing,
    Importing,
    Completed,
    Failed,
}

private data class PortalParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
)

@Composable
fun OnboardingMediaImportScreen(
    onImportScreenshots: suspend (onProgress: suspend (Float) -> Unit) -> OnboardingMediaImportResult,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var phase by remember { mutableStateOf(OnboardingMediaImportPhase.Preparing) }
    var result by remember { mutableStateOf<OnboardingMediaImportResult?>(null) }
    var isLeaving by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var rotatingMessageIndex by remember { mutableStateOf(0) }
    var countdownSeconds by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        delay(520)
        phase = OnboardingMediaImportPhase.Importing
        progress = 0.05f
        val importResult = runCatching {
            onImportScreenshots { nextProgress ->
                withContext(Dispatchers.Main.immediate) {
                    progress = nextProgress.coerceIn(0f, 1f)
                }
            }
        }.getOrElse { throwable ->
            OnboardingMediaImportResult(
                isSuccess = false,
                importedCount = 0,
                scannedCount = 0,
                message = throwable.message ?: "스크린샷 확인 실패",
            )
        }
        progress = 1f
        delay(420)
        result = importResult
        phase = if (importResult.isSuccess) {
            OnboardingMediaImportPhase.Completed
        } else {
            OnboardingMediaImportPhase.Failed
        }
    }

    LaunchedEffect(phase) {
        if (phase == OnboardingMediaImportPhase.Importing) {
            while (true) {
                delay(1500)
                rotatingMessageIndex = (rotatingMessageIndex + 1) % 2
            }
        } else {
            rotatingMessageIndex = 0
        }
    }

    LaunchedEffect(phase) {
        if (
            phase == OnboardingMediaImportPhase.Completed ||
            phase == OnboardingMediaImportPhase.Failed
        ) {
            for (second in 3 downTo 1) {
                countdownSeconds = second
                delay(1000)
            }
            countdownSeconds = null
            isLeaving = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkGradient),
    ) {
        PortalMediaImportAnimation(
            progress = progress,
            countdownSeconds = countdownSeconds,
            isImporting = phase == OnboardingMediaImportPhase.Importing,
            isCompleted = phase == OnboardingMediaImportPhase.Completed,
            isLeaving = isLeaving,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = !isLeaving,
            enter = fadeIn(tween(260)) + slideInVertically { it / 8 },
            exit = fadeOut(tween(180)) + slideOutVertically { -it / 8 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp)
                .padding(bottom = 88.dp),
        ) {
            GreetingCopy(
                phase = phase,
                result = result,
                rotatingMessageIndex = rotatingMessageIndex,
                countdownSeconds = countdownSeconds,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    LaunchedEffect(isLeaving) {
        if (isLeaving) {
            delay(320)
            onContinue()
        }
    }
}

@Composable
private fun GreetingCopy(
    phase: OnboardingMediaImportPhase,
    result: OnboardingMediaImportResult?,
    rotatingMessageIndex: Int,
    countdownSeconds: Int?,
    modifier: Modifier = Modifier,
) {
    val title = when (phase) {
        OnboardingMediaImportPhase.Preparing -> "안녕하세요."
        OnboardingMediaImportPhase.Importing -> {
            if (rotatingMessageIndex == 0) "안녕하세요." else "스크린샷을 살펴보는 중이에요."
        }
        OnboardingMediaImportPhase.Completed -> {
            val count = result?.importedCount ?: 0
            if (count > 0) "새 스크린샷 ${count}개를 찾았어요." else "새로 찾은 스크린샷은 없어요."
        }
        OnboardingMediaImportPhase.Failed -> "스크린샷을 확인하지 못했어요."
    }

    val body = when (phase) {
        OnboardingMediaImportPhase.Preparing ->
            "필요한 정보만 고르고,\nAI가 주제로 묶을 수 있게 준비할게요."
        OnboardingMediaImportPhase.Importing -> {
            if (rotatingMessageIndex == 0) {
                "필요한 정보만 고르고,\nAI가 주제로 묶을 수 있게 준비할게요."
            } else {
                "AI가 주제를 추천할 수 있도록\n최근 화면 자료를 정리하고 있어요."
            }
        }
        OnboardingMediaImportPhase.Completed -> {
            val count = result?.importedCount ?: 0
            if (count > 0) {
                "AI가 이 자료들을 바탕으로\n정리할 만한 주제를 추천해드릴 수 있어요."
            } else {
                "자료를 모으면 AI가 관련 내용을 묶어\n정리할 만한 주제를 추천해드릴 수 있어요."
            }
        }
        OnboardingMediaImportPhase.Failed ->
            "괜찮아요. 공유하기나 빠른 설정 타일로\n나중에 자료를 추가할 수 있어요."
    }
    val countdownText = countdownSeconds?.let { "${it}초 뒤 다음 단계로 이동합니다." }

    AnimatedContent(
        targetState = Triple(title, body, countdownText),
        transitionSpec = {
            fadeIn(tween(260)) + slideInVertically { it / 8 } togetherWith
                fadeOut(tween(180)) + slideOutVertically { -it / 8 }
        },
        label = "media-import-copy",
        modifier = modifier,
    ) { (animatedTitle, animatedBody, animatedCountdown) ->
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

            if (animatedCountdown != null) {
                Spacer(Modifier.height(12.dp))

                Text(
                    text = animatedCountdown,
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun PortalMediaImportAnimation(
    progress: Float,
    countdownSeconds: Int?,
    isImporting: Boolean,
    isCompleted: Boolean,
    isLeaving: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "portal-media-import")
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
        val center = Offset(size.width / 2f, size.height * 0.42f)
        val baseRadius = min(size.width, size.height) * 0.20f
        val leaveBoost = if (isLeaving) 0.16f else 0f
        val pulseRadius = baseRadius * (1f + pulse * 0.06f + leaveBoost)
        val importPull = if (isImporting) 0.18f + pulse * 0.08f else if (isCompleted) 0.09f else 0f

        drawCircle(
            color = AppColors.Blue.copy(alpha = 0.16f + pulse * 0.08f + leaveBoost * 0.55f),
            radius = pulseRadius * 1.75f,
            center = center,
        )
        drawCircle(
            color = Color(0xFF67E8F9).copy(alpha = 0.10f + pulse * 0.04f + leaveBoost * 0.35f),
            radius = pulseRadius * 1.25f,
            center = center,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = pulseRadius * 0.68f,
            center = center,
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.34f),
            radius = pulseRadius,
            center = center,
            style = Stroke(width = 2.2.dp.toPx()),
        )
        drawArc(
            color = Color.White.copy(alpha = 0.16f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius * 1.34f, center.y - pulseRadius * 1.34f),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2.68f, pulseRadius * 2.68f),
            style = Stroke(width = 3.4.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = Color(0xFFBFEAFF).copy(alpha = 0.92f),
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius * 1.34f, center.y - pulseRadius * 1.34f),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2.68f, pulseRadius * 2.68f),
            style = Stroke(width = 5.0.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = Color(0xFF9DD9FF).copy(alpha = 0.86f),
            startAngle = rotation * 360f,
            sweepAngle = 114f,
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius, center.y - pulseRadius),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2f, pulseRadius * 2f),
            style = Stroke(width = 5.2.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = AppColors.Blue.copy(alpha = 0.64f),
            startAngle = rotation * -360f + 180f,
            sweepAngle = 76f,
            useCenter = false,
            topLeft = Offset(center.x - pulseRadius * 1.18f, center.y - pulseRadius * 1.18f),
            size = androidx.compose.ui.geometry.Size(pulseRadius * 2.36f, pulseRadius * 2.36f),
            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
        )

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
                color = Color(0xFF9DD9FF).copy(alpha = alpha * 0.22f),
                radius = (particle.radius * 3.2f).dp.toPx(),
                center = position,
            )
        }

        countdownSeconds?.let { second ->
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 42.sp.toPx()
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD,
                    )
                }
                drawText(
                    second.toString(),
                    center.x,
                    center.y - (paint.descent() + paint.ascent()) / 2f,
                    paint,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingMediaImportScreenPreview() {
    OnboardingMediaImportScreen(
        onImportScreenshots = { onProgress ->
            onProgress(1f)
            OnboardingMediaImportResult(
                isSuccess = true,
                importedCount = 8,
                scannedCount = 34,
                message = "Imported 8 screenshots",
            )
        },
        onContinue = {},
    )
}
