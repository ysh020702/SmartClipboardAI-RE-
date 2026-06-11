package com.samsung.smartclipboard.data.source.clipboard

interface ClipboardCaptureHandler {
    suspend fun captureLatestClipboard(): ClipboardCaptureResult
}