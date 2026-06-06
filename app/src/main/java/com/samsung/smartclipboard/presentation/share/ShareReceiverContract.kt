package com.samsung.smartclipboard.presentation.share

data class ShareReceiverUiState(
    val isProcessing: Boolean = true,
    val message: String = "Saving shared item...",
    val savedCount: Int = 0,
    val shouldFinish: Boolean = false,
    val isSuccess: Boolean = false
)