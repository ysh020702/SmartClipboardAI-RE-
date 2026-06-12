package com.samsung.smartclipboard.domain.model

import org.json.JSONObject

data class LinkMetadata(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val textContent: String? = null,
)

object LinkMetadataCodec {
    private const val TYPE = "smartclipboard.link_metadata.v1"
    private const val KEY_TYPE = "_type"
    private const val KEY_TITLE = "title"
    private const val KEY_DESCRIPTION = "description"
    private const val KEY_IMAGE_URL = "imageUrl"
    private const val KEY_TEXT_CONTENT = "textContent"

    fun encode(metadata: LinkMetadata): String? {
        if (
            metadata.title.isNullOrBlank() &&
            metadata.description.isNullOrBlank() &&
            metadata.imageUrl.isNullOrBlank() &&
            metadata.textContent.isNullOrBlank()
        ) {
            return null
        }

        return JSONObject()
            .put(KEY_TYPE, TYPE)
            .putClean(KEY_TITLE, metadata.title)
            .putClean(KEY_DESCRIPTION, metadata.description)
            .putClean(KEY_IMAGE_URL, metadata.imageUrl)
            .putClean(KEY_TEXT_CONTENT, metadata.textContent)
            .toString()
    }

    fun decode(raw: String?): LinkMetadata? {
        if (raw.isNullOrBlank()) return null

        return runCatching {
            val json = JSONObject(raw)
            if (json.optString(KEY_TYPE) != TYPE) return@runCatching null

            LinkMetadata(
                title = json.optCleanString(KEY_TITLE),
                description = json.optCleanString(KEY_DESCRIPTION),
                imageUrl = json.optCleanString(KEY_IMAGE_URL),
                textContent = json.optCleanString(KEY_TEXT_CONTENT),
            )
        }.getOrNull()
    }

    fun displayText(raw: String?): String? {
        val metadata = decode(raw) ?: return null
        return metadata.description
            ?: metadata.textContent
            ?: metadata.title
    }

    fun analysisText(raw: String?): String? {
        val metadata = decode(raw) ?: return null
        return metadata.textContent
            ?: metadata.description
            ?: metadata.title
    }

    private fun JSONObject.putClean(key: String, value: String?): JSONObject {
        val cleaned = value?.trim()?.takeIf { it.isNotBlank() }
        if (cleaned != null) put(key, cleaned)
        return this
    }

    private fun JSONObject.optCleanString(key: String): String? {
        return optString(key)
            .trim()
            .takeIf { it.isNotBlank() }
    }
}
