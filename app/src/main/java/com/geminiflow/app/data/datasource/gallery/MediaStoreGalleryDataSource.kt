package com.geminiflow.app.data.datasource.gallery

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.geminiflow.app.domain.model.gallery.GalleryMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreGalleryDataSource(
    private val context: Context
) {
    companion object {
        private const val TAG = "MediaStoreGalleryDS"
    }

    suspend fun queryMedia(offset: Int, limit: Int): List<GalleryMediaItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<GalleryMediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE
        )

        val contentResolver: ContentResolver = context.contentResolver

        try {
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val queryArgs = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Images.Media.DATE_ADDED)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                }
                contentResolver.query(collection, projection, queryArgs, null)
            } else {
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"
                contentResolver.query(collection, projection, null, null, sortOrder)
            }

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dateColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val name = c.getString(nameColumn) ?: "IMG_$id.jpg"
                    val mime = c.getString(mimeColumn) ?: "image/jpeg"
                    val dateAdded = c.getLong(dateColumn)
                    val size = c.getLong(sizeColumn)
                    val uri = ContentUris.withAppendedId(collection, id)

                    result.add(
                        GalleryMediaItem(
                            id = id,
                            uri = uri,
                            displayName = name,
                            mimeType = mime,
                            dateAddedSec = dateAdded,
                            size = size
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied accessing MediaStore: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore images: ${e.message}", e)
        }

        result
    }
}
