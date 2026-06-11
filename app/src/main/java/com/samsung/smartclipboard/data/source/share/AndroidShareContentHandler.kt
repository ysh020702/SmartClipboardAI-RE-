package com.samsung.smartclipboard.data.source.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.samsung.smartclipboard.di.IoDispatcher
import com.samsung.smartclipboard.domain.repository.DataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject

class AndroidShareContentHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataRepository: DataRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ShareContentHandler {

    override suspend fun saveFromIntent(intent: Intent): ShareSaveResult {
        return withContext(ioDispatcher) {
            when (intent.action) {
                Intent.ACTION_SEND -> handleSend(intent)
                Intent.ACTION_SEND_MULTIPLE -> handleSendMultiple(intent)
                else -> ShareSaveResult(
                    savedCount = 0,
                    message = "Unsupported intent action",
                    isSuccess = false
                )
            }
        }
    }

    private suspend fun handleSend(intent: Intent): ShareSaveResult {
        val type = intent.type
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        val title = intent.getStringExtra(Intent.EXTRA_TITLE)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
        // HTML 텍스트도 확인 (브라우저 공유 시 URL 포함)
        val htmlText = intent.getStringExtra(Intent.EXTRA_HTML_TEXT)

        // Try text first
        if (!text.isNullOrBlank()) {
            return saveTextOrLink(text, title, htmlText)
        }

        // Try stream/file
        val uri = getStreamUri(intent)
        if (uri != null) {
            return saveStream(uri, type)
        }

        return ShareSaveResult(
            savedCount = 0,
            message = "No supported shared content found",
            isSuccess = false
        )
    }

    private suspend fun handleSendMultiple(intent: Intent): ShareSaveResult {
        val uris = getStreamUris(intent)
        val type = intent.type

        if (uris.isNullOrEmpty()) {
            // Try text as fallback
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
            val htmlText = intent.getStringExtra(Intent.EXTRA_HTML_TEXT)
            if (!text.isNullOrBlank()) {
                val title = intent.getStringExtra(Intent.EXTRA_TITLE)
                    ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                return saveTextOrLink(text, title, htmlText)
            }
            return ShareSaveResult(
                savedCount = 0,
                message = "No supported shared content found",
                isSuccess = false
            )
        }

        var savedCount = 0
        val errors = mutableListOf<String>()

        for ((index, uri) in uris.withIndex()) {
            try {
                val resolvedType = type ?: context.contentResolver.getType(uri)
                val localUri = copyToInternalStorage(uri, resolvedType, index)
                val displayName = getDisplayName(uri)
                dataRepository.addMedia(localUri, resolvedType, source = "share")
                savedCount++
            } catch (e: SecurityException) {
                errors.add("Permission denied for item ${index + 1}")
            } catch (e: IOException) {
                errors.add("Failed to copy item ${index + 1}")
            }
        }

        return if (savedCount > 0) {
            ShareSaveResult(
                savedCount = savedCount,
                message = "Saved $savedCount item(s)" +
                    if (errors.isNotEmpty()) ". ${errors.size} failed." else "",
                isSuccess = true
            )
        } else {
            ShareSaveResult(
                savedCount = 0,
                message = "Failed to save items: ${errors.joinToString("; ")}",
                isSuccess = false
            )
        }
    }

    private suspend fun saveTextOrLink(text: String, title: String?, htmlText: String? = null): ShareSaveResult {
        val trimmedText = text.trim()
        
        // 1. HTML 텍스트에서 URL 추출 시도 (브라우저 공유 시)
        val htmlUrl = extractUrlFromHtml(htmlText)
        if (htmlUrl != null) {
            dataRepository.addLink(htmlUrl, title, source = "share")
            return ShareSaveResult(
                savedCount = 1,
                message = "Link saved",
                isSuccess = true
            )
        }
        
        // 2. 텍스트에서 URL 추출 시도
        val extractedUrl = extractUrl(trimmedText)
        
        // 3. URL 로 판단할 조건:
        // - 추출된 URL 이 있고 텍스트가 짧으면 (200 자 이내)
        // - "출처:" 패턴이 있으면
        // - URL 이 텍스트 시작 부분에 있으면
        
        val isShortText = trimmedText.length <= 200
        val hasUrlAtStart = trimmedText.startsWith("http://") || 
                           trimmedText.startsWith("https://") || 
                           trimmedText.startsWith("www.")
        val hasSourcePattern = trimmedText.contains(Regex("^출처 [:\\s]*", RegexOption.IGNORE_CASE))
        val hasUrlPattern = extractedUrl != null
        
        // URL 이 있고 (짧거나 출처 패턴이면) 링크로 저장
        val shouldSaveAsLink = extractedUrl != null && (isShortText || hasUrlAtStart || hasSourcePattern)
        
        if (shouldSaveAsLink) {
            dataRepository.addLink(extractedUrl, title, source = "share")
            return ShareSaveResult(
                savedCount = 1,
                message = "Link saved",
                isSuccess = true
            )
        }

        // 그 외에는 텍스트로 저장
        dataRepository.addText(text, source = "share")
        return ShareSaveResult(
            savedCount = 1,
            message = "Text saved",
            isSuccess = true
        )
    }

    /**
     * HTML 텍스트에서 href URL 을 추출합니다.
     */
    private fun extractUrlFromHtml(htmlText: String?): String? {
        if (htmlText.isNullOrBlank()) return null
        
        // HTML href 에서 URL 추출: href="..." 또는 href='...'
        val hrefPattern = Regex("href=[\"'](https?://[^\"']+)[\"']")
        val match = hrefPattern.find(htmlText)
        return match?.groupValues?.get(1)
    }

    /**
     * 텍스트에서 URL 을 추출합니다.
     * @param text 추출할 텍스트
     * @return URL 이 발견되면 URL 문자열, 아니면 null
     */
    private fun extractUrl(text: String): String? {
        // URL 패턴: http://, https://, www. 로 시작
        val urlPattern = Regex("(https?://|www\\.)[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
        
        // "출처: ~" 패턴에서 URL 추출
        val sourcePattern = Regex("출처 [:\\s]*(https?://|www\\.)[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", RegexOption.IGNORE_CASE)
        
        // 먼저 출처 패턴 확인
        val sourceMatch = sourcePattern.find(text)
        if (sourceMatch != null) {
            val url = sourceMatch.groupValues[0]
                .replace(Regex("^출처 [:\\s]*", RegexOption.IGNORE_CASE), "")
                .trim()
            return if (url.startsWith("www.")) "https://$url" else url
        }
        
        // 일반 URL 패턴 확인
        val match = urlPattern.find(text)
        return match?.value?.let { url ->
            if (url.startsWith("www.")) {
                "https://$url"
            } else {
                url
            }
        }
    }

    private suspend fun saveStream(uri: Uri, type: String?): ShareSaveResult {
        return try {
            val resolvedType = type ?: context.contentResolver.getType(uri)
            val localUri = copyToInternalStorage(uri, resolvedType, 0)
            dataRepository.addMedia(localUri, resolvedType, source = "share")
            ShareSaveResult(
                savedCount = 1,
                message = "File saved",
                isSuccess = true
            )
        } catch (e: SecurityException) {
            ShareSaveResult(
                savedCount = 0,
                message = "Permission denied accessing shared content",
                isSuccess = false
            )
        } catch (e: IOException) {
            ShareSaveResult(
                savedCount = 0,
                message = "Failed to save file: ${e.message}",
                isSuccess = false
            )
        }
    }

    private fun getStreamUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun getStreamUris(intent: Intent): List<Uri>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun copyToInternalStorage(uri: Uri, mimeType: String?, index: Int): String {
        val dir = File(context.filesDir, "shared_content")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val extension = inferExtension(mimeType, uri)
        val safeName = generateSafeFileName(uri, index, extension)
        val targetFile = File(dir, safeName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Cannot open input stream for $uri")

        return Uri.fromFile(targetFile).toString()
    }

    private fun inferExtension(mimeType: String?, uri: Uri): String {
        // Try from MIME type first
        if (mimeType != null) {
            when {
                mimeType.startsWith("image/") -> {
                    val sub = mimeType.substringAfter("image/")
                    return if (sub in listOf("jpeg", "jpg")) ".jpg"
                    else ".$sub"
                }
                mimeType == "application/pdf" -> return ".pdf"
                mimeType.startsWith("video/") -> return ".${mimeType.substringAfter("video/")}"
                mimeType.startsWith("audio/") -> return ".${mimeType.substringAfter("audio/")}"
            }
        }

        // Try from URI display name
        val displayName = getDisplayName(uri)
        val dotIndex = displayName?.lastIndexOf('.')
        if (dotIndex != null && dotIndex >= 0) {
            return displayName.substring(dotIndex)
        }

        return ".bin"
    }

    private fun generateSafeFileName(uri: Uri, index: Int, extension: String): String {
        val base = getDisplayName(uri)
            ?.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            ?.take(50)
            ?: "shared"

        val hash = MessageDigest.getInstance("MD5")
            .digest(uri.toString().toByteArray())
            .take(4)
            .joinToString("") { "%02x".format(it) }

        val timestamp = System.currentTimeMillis()
        return "${timestamp}_${hash}_${base}${extension}"
    }

    private fun getDisplayName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        return name
    }
}
