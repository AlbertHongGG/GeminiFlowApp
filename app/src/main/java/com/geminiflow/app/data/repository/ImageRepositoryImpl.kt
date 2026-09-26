package com.geminiflow.app.data.repository

import android.content.Context
import android.util.Log
import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.data.network.WebkitCookieJar
import com.geminiflow.app.domain.model.common.ImageDownloadException
import com.geminiflow.app.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ImageRepositoryImpl(
    private val context: Context,
    cookieJar: CookieJar = WebkitCookieJar(),
    customClient: OkHttpClient? = null,
    imagesDirProvider: (() -> File)? = null
) : ImageRepository {

    companion object {
        private const val TAG = "ImageRepositoryImpl"
        private const val IMAGES_DIR_NAME = "images"
    }

    private val imagesDir: File by lazy {
        imagesDirProvider?.invoke() ?: File(context.filesDir, IMAGES_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    private val downloadClient: OkHttpClient = customClient ?: OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun downloadImage(url: String, modelName: String): File = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeModel = modelName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val filename = "${timeStamp}_${safeModel}_generated.png"
        val targetFile = File(imagesDir, filename)
        val tempFile = File(imagesDir, "$filename.tmp")

        if (url.startsWith("data:image/")) {
            try {
                val base64Data = url.substringAfter("base64,")
                val decodedBytes = java.util.Base64.getDecoder().decode(base64Data)
                targetFile.writeBytes(decodedBytes)
                return@withContext targetFile
            } catch (e: Exception) {
                Log.e(TAG, "Base64 decode failed: ${e.message}", e)
                throw ImageDownloadException("Base64 圖片解碼失敗: ${e.message}", e)
            }
        }

        val safeUrl = if (url.startsWith("http://")) {
            "https://" + url.removePrefix("http://")
        } else {
            url
        }

        val request = Request.Builder()
            .url(safeUrl)
            .header("User-Agent", GeminiConfig.DEFAULT_USER_AGENT)
            .header("Referer", GeminiConfig.GEMINI_BASE_URL)
            .header("Origin", GeminiConfig.GEMINI_BASE_URL.removeSuffix("/"))
            .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .header("Sec-Fetch-Site", "cross-site")
            .header("Sec-Fetch-Mode", "no-cors")
            .header("Sec-Fetch-Dest", "image")
            .get()
            .build()

        try {
            val response = downloadClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                throw ImageDownloadException("Google CDN 影像下載失敗，伺服器回傳 HTTP $code: $safeUrl")
            }

            val body = response.body ?: throw ImageDownloadException("Google CDN 回傳資料為空")
            
            FileOutputStream(tempFile).use { output ->
                body.byteStream().copyTo(output)
            }
            response.close()

            if (tempFile.length() <= 0L) {
                tempFile.delete()
                throw ImageDownloadException("下載的影像檔案長度為 0 位元組")
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            targetFile
        } catch (e: Exception) {
            tempFile.delete()
            throw if (e is ImageDownloadException) e else ImageDownloadException("影像下載管線異常: ${e.message}", e)
        }
    }

    override fun getCacheStats(): Pair<Int, Long> {
        val files = imagesDir.listFiles()?.filter { it.isFile } ?: emptyList()
        val count = files.size
        val bytes = files.sumOf { it.length() }
        return Pair(count, bytes)
    }

    override fun getImageFile(filename: String): File? {
        val safeFilename = File(filename).name
        val file = File(imagesDir, safeFilename)
        return if (file.exists() && file.isFile && file.length() > 0) file else null
    }

    override suspend fun clearOldImages(maxAgeMillis: Long) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            imagesDir.listFiles()?.forEach { file ->
                if (file.isFile && (now - file.lastModified() > maxAgeMillis)) {
                    file.delete()
                }
            }
        }
    }
}
