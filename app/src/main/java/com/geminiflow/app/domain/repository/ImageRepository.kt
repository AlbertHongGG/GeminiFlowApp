package com.geminiflow.app.domain.repository

import com.geminiflow.app.domain.model.cache.CachedImageItem
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

interface ImageRepository {
    val cacheInvalidationEvents: SharedFlow<Unit>
    suspend fun downloadImage(url: String, modelName: String): File
    fun getImageFile(filename: String): File?
    suspend fun getAllCachedImages(): List<CachedImageItem>
    suspend fun deleteCachedImage(filename: String): Boolean
    suspend fun clearOldImages(maxAgeMillis: Long)
    fun getCacheStats(): Pair<Int, Long>
}
