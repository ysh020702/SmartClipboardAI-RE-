package com.samsung.smartclipboard.data.source.media

interface MediaImportHandler {
    suspend fun importRecentScreenshots(): MediaImportResult
}