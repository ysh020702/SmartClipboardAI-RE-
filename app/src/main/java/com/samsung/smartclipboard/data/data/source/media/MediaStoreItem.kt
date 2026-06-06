package com.samsung.smartclipboard.data.source.media

data class MediaStoreItem(
    val uri: String,
    val mimeType: String?,
    val createdAt: Long,
    val displayName: String?,
    val relativePath: String?,
    val bucketName: String?,
    val isScreenshot: Boolean
)