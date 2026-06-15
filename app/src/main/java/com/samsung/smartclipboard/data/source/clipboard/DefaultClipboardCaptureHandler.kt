package com.samsung.smartclipboard.data.source.clipboard

import android.util.Log
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

    companion object {
        private const val TAG = "DefaultClipboardCaptureHandler"
    }

    override suspend fun captureLatestClipboard(): ClipboardCaptureResult {
        return withContext(ioDispatcher) {
            Log.d(TAG, "captureLatestClipboard: 클립보드 읽기 시작")
            val text = clipboardDataSource.getLatestText()

            if (text.isNullOrBlank()) {
                Log.d(TAG, "captureLatestClipboard: 클립보드 비어 있음 → EMPTY_CLIPBOARD")
                return@withContext ClipboardCaptureResult(
                    isSuccess = false,
                    savedCount = 0,
                    message = "No clipboard text found",
                    type = CaptureResultType.EMPTY_CLIPBOARD
                )
            }

            val normalized = text.trim()
            Log.d(TAG, "captureLatestClipboard: 읽은 텍스트 길이=${normalized.length}, 앞 50자=${normalized.take(50)}")

            // Duplicate guard
            val now = System.currentTimeMillis()
            if (normalized == lastSavedText && (now - lastSaveTime) < dedupWindowMs) {
                Log.d(TAG, "captureLatestClipboard: 중복 감지 (${now - lastSaveTime}ms 내 동일 텍스트) → ALREADY_SAVED")
                return@withContext ClipboardCaptureResult(
                    isSuccess = true,
                    savedCount = 0,
                    message = "Already saved",
                    type = CaptureResultType.ALREADY_SAVED
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
                Log.d(TAG, "captureLatestClipboard: URL 감지 → addLink() 호출, url=$url")
                dataRepository.addLink(url, title = null, source = "clipboard_tile")
                lastSavedText = normalized
                lastSaveTime = now
                Log.d(TAG, "captureLatestClipboard: 링크 저장 완료 → LINK_SAVED")
                return@withContext ClipboardCaptureResult(
                    isSuccess = true,
                    savedCount = 1,
                    message = "Saved link",
                    type = CaptureResultType.LINK_SAVED
                )
            }

            Log.d(TAG, "captureLatestClipboard: 일반 텍스트 → addText() 호출")
            dataRepository.addText(normalized, source = "clipboard_tile")
            lastSavedText = normalized
            lastSaveTime = now
            Log.d(TAG, "captureLatestClipboard: 텍스트 저장 완료 → TEXT_SAVED")
            ClipboardCaptureResult(
                isSuccess = true,
                savedCount = 1,
                message = "Saved clipboard text",
                type = CaptureResultType.TEXT_SAVED
            )
        }
    }
}