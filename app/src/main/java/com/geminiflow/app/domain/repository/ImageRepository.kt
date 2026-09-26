package com.geminiflow.app.domain.repository

import java.io.File

interface ImageRepository {
    suspend fun downloadImage(url: String, modelName: String): File
    fun getImageFile(filename: String): File?
    suspend fun clearOldImages(maxAgeMillis: Long)
    fun getCacheStats(): Pair<Int, Long>
}
