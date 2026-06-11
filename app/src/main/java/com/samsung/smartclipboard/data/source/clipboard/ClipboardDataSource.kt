package com.samsung.smartclipboard.data.source.clipboard

interface ClipboardDataSource {
    suspend fun getLatestText(): String?
}