package com.samsung.smartclipboard.data.source.media

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import com.samsung.smartclipboard.data.source.period.CollectionPeriodManager
import com.samsung.smartclipboard.di.IoDispatcher
import com.samsung.smartclipboard.domain.model.DataItemType
import com.samsung.smartclipboard.domain.repository.DataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

//스크린샷을 읽어오는 로직을 담당합니다.
@Singleton
class ScreenshotImportHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataRepository: DataRepository,
    private val collectionPeriodManager: CollectionPeriodManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun importRecentScreenshots(): MediaImportResult {
        return withContext(ioDispatcher) {
            try {
                if (!hasImageReadPermission()) {
                    return@withContext MediaImportResult(
                        isSuccess = false,
                        importedCount = 0,
                        scannedCount = 0,
                        message = "Media permission required"
                    )
                }

                val allItems = fetchRecentScreenshot( limit = 500 )  //500개의 이미지 중 스크린샷만 가져옵니다.
                if (allItems.isEmpty()) {
                    return@withContext MediaImportResult(
                        isSuccess = false,
                        importedCount = 0,
                        scannedCount = 0,
                        message = "No recent media found"
                    )
                }

                // 수집 기간 필터링 적용
                val periodFilteredItems = allItems.filter { item ->
                    collectionPeriodManager.isWithinPeriod(item.createdAt)
                }

                val screenshots = periodFilteredItems.filter { it.isScreenshot }
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


    //이미지를 가져올 권한이 있는지 확인합니다.
    private fun hasImageReadPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // API 34+: either READ_MEDIA_IMAGES or READ_MEDIA_VISUAL_USER_SELECTED
                context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
                        PackageManager.PERMISSION_GRANTED ||
                        context.checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ==
                        PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // API 33
                context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
                        PackageManager.PERMISSION_GRANTED
            }
            else -> {
                // API 32 and below
                context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private suspend fun fetchRecentScreenshot(limit: Int = 500): List<MediaStoreItem> {
        if (!hasImageReadPermission()) return emptyList()

        return try {
            val items = mutableListOf<MediaStoreItem>()
            val projection = mutableListOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )
            val hasRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            if (hasRelativePath) {
                projection.add(MediaStore.Images.Media.RELATIVE_PATH)
            }

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val pathCol = if (hasRelativePath) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else {
                    -1
                }

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(nameCol) ?: ""
                    val mimeType = cursor.getString(mimeCol)
                    val bucketName = cursor.getString(bucketCol) ?: ""
                    val relativePath = if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""
                    val dateTaken = cursor.getLong(takenCol)
                    val dateAdded = cursor.getLong(addedCol)

                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val createdAt = when {
                        dateTaken > 0 -> dateTaken
                        dateAdded > 0 -> dateAdded * 1000L
                        else -> System.currentTimeMillis()
                    }

                    val isScreenshot = displayName.contains("screenshot", ignoreCase = true) ||
                            bucketName.contains("screenshot", ignoreCase = true) ||
                            relativePath.contains("screenshot", ignoreCase = true) ||
                            (displayName.contains("screen", ignoreCase = true) &&
                                    displayName.contains("shot", ignoreCase = true))

                    items.add(
                        MediaStoreItem(
                            uri = uri,
                            mimeType = mimeType,
                            createdAt = createdAt,
                            displayName = displayName,
                            relativePath = relativePath,
                            bucketName = bucketName,
                            isScreenshot = isScreenshot
                        )
                    )
                    count++
                }
            }
            items
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: RuntimeException) {
            emptyList()
        }
    }

    companion object {
        data class MediaStoreItem(
            val uri: String,
            val mimeType: String?,
            val createdAt: Long,
            val displayName: String?,
            val relativePath: String?,
            val bucketName: String?,
            val isScreenshot: Boolean
        )
        data class MediaImportResult(
            val isSuccess: Boolean,
            val importedCount: Int,
            val scannedCount: Int,
            val message: String
        )
    }
}
