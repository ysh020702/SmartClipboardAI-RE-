package com.samsung.smartclipboard.gemini

import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.DataItemType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object GeminiUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun contentPreview(item: DataItem, length: Int): String {
        return when (item.type) {
            DataItemType.FILE -> {
                listOfNotNull(item.title, item.source, item.mimeType)
                    .joinToString(" / ")
                    .take(length)
            }
            else -> item.effectiveContent.take(length)
        }
    }

    fun formatDate(epochMillis: Long): String {
        return try {
            dateFormat.format(Date(epochMillis))
        } catch (_: Exception) {
            epochMillis.toString()
        }
    }

    fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun extractJsonObject(raw: String): String? {
        val cleanedText = raw.replace(Regex("```json\\s*"), "").replace(Regex("```\\s*"), "")
        val start = cleanedText.indexOf('{')
        val end = cleanedText.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return cleanedText.substring(start, end + 1).trim()
        }
        return null
    }
}