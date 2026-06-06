package com.samsung.smartclipboard.data.source.media

data class MediaImportResult(
    val isSuccess: Boolean,
    val importedCount: Int,
    val scannedCount: Int,
    val message: String
)