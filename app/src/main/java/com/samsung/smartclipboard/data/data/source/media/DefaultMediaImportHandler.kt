package com.samsung.smartclipboard.data.source.media

import com.samsung.smartclipboard.di.IoDispatcher
import com.samsung.smartclipboard.domain.model.DataItemType
import com.samsung.smartclipboard.domain.repository.DataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMediaImportHandler @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val dataRepository: DataRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MediaImportHandler {

    override suspend fun importRecentScreenshots(): MediaImportResult {
        return withContext(ioDispatcher) {
            try {
                if (!mediaStoreDataSource.hasImageReadPermission()) {
                    return@withContext MediaImportResult(
                        isSuccess = false,
                        importedCount = 0,
                        scannedCount = 0,
                        message = "Media permission required"
                    )
                }

                val allItems = mediaStoreDataSource.fetchRecentMedia(limit = 100)
                if (allItems.isEmpty()) {
                    return@withContext MediaImportResult(
                        isSuccess = false,
                        importedCount = 0,
                        scannedCount = 0,
                        message = "No recent media found"
                    )
                }

                val screenshots = allItems.filter { it.isScreenshot }
                if (screenshots.isEmpty()) {
                    return@withContext MediaImportResult(
                        isSuccess = true,
                        importedCount = 0,
                        scannedCount = allItems.size,
                        message = "No screenshots found"
                    )
                }

                // Check existing items to avoid duplicates
                val existingItems = dataRepository.observeItems().first()
                val existingByUri = existingItems.associateBy { it.content }
                val existingUris = existingByUri.keys

                screenshots.forEach { item ->
                    val existing = existingByUri[item.uri]
                    if (
                        existing?.type == DataItemType.SCREENSHOT &&
                        existing.createdAt != item.createdAt
                    ) {
                        dataRepository.updateScreenshotTimestamp(
                            uri = item.uri,
                            createdAt = item.createdAt
                        )
                    }
                }

                val newScreenshots = screenshots.filter { it.uri !in existingUris }
                    .take(30)

                if (newScreenshots.isEmpty()) {
                    return@withContext MediaImportResult(
                        isSuccess = true,
                        importedCount = 0,
                        scannedCount = allItems.size,
                        message = "No new screenshots — all previously imported"
                    )
                }

                for (item in newScreenshots) {
                    dataRepository.addScreenshot(
                        uri = item.uri,
                        title = item.displayName,
                        mimeType = item.mimeType,
                        source = "mediastore_screenshot",
                        createdAt = item.createdAt
                    )
                }

                MediaImportResult(
                    isSuccess = true,
                    importedCount = newScreenshots.size,
                    scannedCount = allItems.size,
                    message = "Imported ${newScreenshots.size} screenshots"
                )
            } catch (e: SecurityException) {
                MediaImportResult(
                    isSuccess = false,
                    importedCount = 0,
                    scannedCount = 0,
                    message = "Permission denied"
                )
            } catch (e: RuntimeException) {
                MediaImportResult(
                    isSuccess = false,
                    importedCount = 0,
                    scannedCount = 0,
                    message = "Failed to scan screenshots"
                )
            }
        }
    }
}
