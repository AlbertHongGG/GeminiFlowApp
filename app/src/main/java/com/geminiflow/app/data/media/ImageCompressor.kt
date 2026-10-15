package com.geminiflow.app.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.geminiflow.app.domain.model.chat.ImagePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ImageCompressor(
    private val context: Context,
    private val maxDimension: Int = 2048,
    private val quality: Int = 85
) {
    companion object {
        private const val TAG = "ImageCompressor"
    }

    suspend fun compressUri(uri: Uri): ImagePayload? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. 取得檔案名稱
            val fileName = queryDisplayName(uri) ?: "image_${System.currentTimeMillis()}.jpg"

            // 2. 取得 EXIF 旋轉角度
            val rotationDegrees = getExifOrientation(uri)

            // 3. 讀取寬高以計算 inSampleSize
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.w(TAG, "Failed to decode image bounds for uri: $uri")
                return@withContext null
            }

            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, maxDimension)
            options.inJustDecodeBounds = false

            // 4. 解碼實際 Bitmap
            val decodedBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext null

            // 5. 套用旋轉校正（若有需要）
            val finalBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(
                    decodedBitmap, 0, 0,
                    decodedBitmap.width, decodedBitmap.height,
                    matrix, true
                )
                if (rotated != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                rotated
            } else {
                decodedBitmap
            }

            // 6. 壓縮為 JPEG ByteArray
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            finalBitmap.recycle()

            ImagePayload(
                data = bytes,
                filename = if (fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true)) {
                    fileName
                } else {
                    "${fileName.substringBeforeLast('.')}.jpg"
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image from uri: $uri", e)
            null
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getExifOrientation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        val maxSide = maxOf(width, height)
        while (maxSide / inSampleSize > maxDim) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
