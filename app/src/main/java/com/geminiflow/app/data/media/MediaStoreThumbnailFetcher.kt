package com.geminiflow.app.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 原生 MediaStore 縮圖 Fetcher。
 * 攔截系統相簿 content://media/external/images/media/{id} 請求，
 * 優先使用 Android 10+ 原生 ContentResolver.loadThumbnail 或舊版 MediaStore.Images.Thumbnails，
 * 避免加載解碼動輒 20MB-50MP 的全畫素原圖，達成微秒級極速縮圖加載。
 */
class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val targetWidth = options.size.width.pxOrElse { 360 }.coerceAtLeast(100)
            val targetHeight = options.size.height.pxOrElse { 360 }.coerceAtLeast(100)

            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(data, Size(targetWidth, targetHeight), null)
            } else {
                val id = ContentUris.parseId(data)
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.getThumbnail(
                    contentResolver,
                    id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null
                )
            }

            if (bitmap != null) {
                DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            } else {
                null
            }
        } catch (e: Exception) {
            // 出現任何異常（如權限或毀損檔案）皆優雅降級走 Coil 預設解碼管線
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isMediaStoreImageUri(data)) return null
            return MediaStoreThumbnailFetcher(context, data, options)
        }

        private fun isMediaStoreImageUri(uri: Uri): Boolean {
            val scheme = uri.scheme
            val authority = uri.authority
            val path = uri.path
            return scheme == ContentResolver.SCHEME_CONTENT &&
                    authority == MediaStore.AUTHORITY &&
                    path?.contains("images", ignoreCase = true) == true
        }
    }
}
