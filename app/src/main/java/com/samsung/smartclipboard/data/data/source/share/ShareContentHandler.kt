package com.samsung.smartclipboard.data.source.share

import android.content.Intent

interface ShareContentHandler {
    suspend fun saveFromIntent(intent: Intent): ShareSaveResult
}