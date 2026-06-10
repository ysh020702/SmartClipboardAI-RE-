package com.samsung.smartclipboard.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    val Blue = Color(0xFF1D4ED8)
    val BlueDeep = Color(0xFF1E3A8A)
    val BlueSoft = Color(0xFFEFF6FF)
    val Cyan = Color(0xFF0891B2)
    val Green = Color(0xFF059669)
    val Slate900 = Color(0xFF0F172A)
    val Slate800 = Color(0xFF1E293B)
    val Slate500 = Color(0xFF64748B)
    val Slate400 = Color(0xFF94A3B8)
    val Slate200 = Color(0xFFE2E8F0)
    val Surface = Color(0xFFF8FAFC)
    val Border = Color(0xFFE8EDF8)
    val Red = Color(0xFFDC2626)

    // Home 전용 확장 색상
    val PanelBorder = Color(0xFFD7DCE5)
    val PromptHint = Color(0xFFD7E1FF)
    val IconSecondary = Color(0xFF5F6368)
}

val BlueGradient = Brush.linearGradient(listOf(AppColors.BlueDeep, AppColors.Blue, Color(0xFF3B82F6)))
val DarkGradient = Brush.linearGradient(listOf(Color(0xFF0F1F3D), Color(0xFF1A3660), AppColors.BlueDeep))


@Composable
fun SmartClipboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.Blue,
            secondary = AppColors.BlueDeep,
            background = AppColors.Surface,
            surface = Color.White,
            onSurface = AppColors.Slate800,
        ),
        content = content,
    )
}

