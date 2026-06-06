package com.samsung.smartclipboard.data.source.clipboard

import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidClipboardDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : ClipboardDataSource {

    override suspend fun getLatestText(): String? {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return null
            val clip = cm.primaryClip ?: return null
            if (clip.itemCount == 0) return null

            // Try to get plain text from each item
            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i) ?: continue
                // Try plain text first
                val text = item.text?.toString()?.trim()
                if (!text.isNullOrBlank()) return text
                // Try coerceToText
                try {
                    val coerced = item.coerceToText(context).toString().trim()
                    if (!coerced.isNullOrBlank()) return coerced
                } catch (_: Exception) {
                    // ignore
                }
            }
            null
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }
}