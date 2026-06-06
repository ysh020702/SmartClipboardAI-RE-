package com.samsung.smartclipboard.data.source.media

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidMediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaStoreDataSource {

    override fun hasImageReadPermission(): Boolean {
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

    override suspend fun fetchRecentMedia(limit: Int): List<MediaStoreItem> {
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
}