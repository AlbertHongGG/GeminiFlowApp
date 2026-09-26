package com.geminiflow.app.domain.repository

import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.cache.CachedImageItem
import com.geminiflow.app.domain.model.export.ImageExportMetadata
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

interface ImageRepository {
    val cacheInvalidationEvents: SharedFlow<Unit>

    /**
     * 下載高解析度原圖（嚴格透過 Google c8o8Fe RPC 專屬導出管線）。
     * 導出失敗或憑證缺失時顯性拋出 ImageDownloadException，絕不掩蓋或降級。
     */
    suspend fun downloadExportedImage(
        metadata: ImageExportMetadata,
        modelName: String,
        tokens: GeminiTokens,
        cookies: Map<String, String>
    ): File

    /**
     * 下載直接圖片 URL（適用於外部或一般 CDN 圖檔）。
     */
    suspend fun downloadImage(
        url: String,
        modelName: String
    ): File

    fun getImageFile(filename: String): File?
    suspend fun getAllCachedImages(): List<CachedImageItem>
    suspend fun deleteCachedImage(filename: String): Boolean
    suspend fun clearOldImages(maxAgeMillis: Long)
    fun getCacheStats(): Pair<Int, Long>
}
