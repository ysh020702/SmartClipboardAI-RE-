package com.samsung.smartclipboard.data.source.clipboard

data class ClipboardCaptureResult(
    val isSuccess: Boolean,
    val savedCount: Int,
    val message: String
)