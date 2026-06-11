package com.samsung.smartclipboard.data.source.clipboard

import com.samsung.smartclipboard.di.IoDispatcher
import com.samsung.smartclipboard.domain.repository.DataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultClipboardCaptureHandler @Inject constructor(
    private val clipboardDataSource: ClipboardDataSource,
    private val dataRepository: DataRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ClipboardCaptureHandler {

    private var lastSavedText: String? = null
    private var lastSaveTime: Long = 0L
    private val dedupWindowMs = 3000L

    override suspend fun captureLatestClipboard(): ClipboardCaptureResult {
        return withContext(ioDispatcher) {
            val text = clipboardDataSource.getLatestText()
            if (text.isNullOrBlank()) {
                return@withContext ClipboardCaptureResult(
                    isSuccess = false,
                    savedCount = 0,
                    message = "No clipboard text found"
                )
            }

            val normalized = text.trim()

            // Duplicate guard
            val now = System.currentTimeMillis()
            if (normalized == lastSavedText && (now - lastSaveTime) < dedupWindowMs) {
                return@withContext ClipboardCaptureResult(
                    isSuccess = true,
                    savedCount = 0,
                    message = "Already saved"
                )
            }

            // Detect URL
            val lower = normalized.lowercase()
            val isLink = lower.startsWith("http://") ||
                    lower.startsWith("https://") ||
                    lower.startsWith("www.")

            if (isLink) {
                val url = if (lower.startsWith("www.")) {
                    "https://$normalized"
                } else {
                    normalized
                }
                dataRepository.addLink(url, title = null, source = "clipboard_tile")
                lastSavedText = normalized
                lastSaveTime = now
                return@withContext ClipboardCaptureResult(
                    isSuccess = true,
                    savedCount = 1,
                    message = "Saved link"
                )
            }

            dataRepository.addText(normalized, source = "clipboard_tile")
            lastSavedText = normalized
            lastSaveTime = now
            ClipboardCaptureResult(
                isSuccess = true,
                savedCount = 1,
                message = "Saved clipboard text"
            )
        }
    }
}