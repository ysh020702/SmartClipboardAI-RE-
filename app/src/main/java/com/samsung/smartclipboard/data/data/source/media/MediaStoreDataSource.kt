package com.samsung.smartclipboard.data.source.media

interface MediaStoreDataSource {
    fun hasImageReadPermission(): Boolean
    suspend fun fetchRecentMedia(limit: Int = 100): List<MediaStoreItem>
}