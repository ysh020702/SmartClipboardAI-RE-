package com.samsung.smartclipboard.presentation.clipboard

data class ClipboardCaptureUiState(
    val isProcessing: Boolean = true,
    val message: String = "Saving clipboard...",
    val savedCount: Int = 0,
    val shouldFinish: Boolean = false,
    val isSuccess: Boolean = false
)