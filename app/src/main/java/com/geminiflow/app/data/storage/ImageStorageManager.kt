package com.geminiflow.app.data.storage

import android.content.Context
import android.util.Log
import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.data.auth.CookieManagerHelper
import com.geminiflow.app.domain.model.ImageDownloadException
import com.geminiflow.app.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageStorageManager(
    private val context: Context,
    private val client: OkHttpClient,
    private val cookieHelper: CookieManagerHelper
) : ImageRepository {

    companion object {
        private const val TAG = "ImageStorageManager"
        private const val IMAGES_DIR_NAME = "images"
    }

    private val imagesDir: File by lazy {
        File(context.filesDir, IMAGES_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    override suspend fun downloadImage(url: String, modelName: String): File = withContext(Dispatchers.IO) {
        val cookies = cookieHelper.getGoogleCookies()
        val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeModel = modelName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val filename = "${timeStamp}_${safeModel}_generated.png"
        val targetFile = File(imagesDir, filename)

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", GeminiConfig.DEFAULT_USER_AGENT)
            .header("Cookie", cookieHeader)
            .header("Referer", GeminiConfig.GEMINI_BASE_URL)
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                throw ImageDownloadException("圖片下載失敗，伺服器回傳 HTTP $code: $url")
            }

            val body = response.body ?: throw ImageDownloadException("下載的圖片資料為空")
            FileOutputStream(targetFile).use { output ->
                body.byteStream().copyTo(output)
            }
            response.close()

            Log.i(TAG, "Image successfully saved to ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Download image failed: ${e.message}", e)
            throw if (e is ImageDownloadException) e else ImageDownloadException("圖片儲存異常: ${e.message}", e)
        }
    }

    override fun getImageFile(filename: String): File? {
        val safeFilename = File(filename).name
        val file = File(imagesDir, safeFilename)
        return if (file.exists() && file.isFile) file else null
    }

    override suspend fun clearOldImages(maxAgeMillis: Long) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            imagesDir.listFiles()?.forEach { file ->
                if (now - file.lastModified() > maxAgeMillis) {
                    file.delete()
                }
            }
        }
    }

    fun getAllImages(): List<File> {
        return imagesDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
