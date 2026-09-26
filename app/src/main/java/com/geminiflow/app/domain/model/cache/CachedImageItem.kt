package com.geminiflow.app.domain.model.cache

import java.io.File

data class CachedImageItem(
    val filename: String,
    val file: File,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val yearMonthKey: String,
    val yearMonthDisplay: String,
    val formattedDate: String,
    val modelName: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val format: ImageFormat = ImageFormat.UNKNOWN
)

data class CachedImageMonthGroup(
    val yearMonthKey: String,
    val yearMonthDisplay: String,
    val totalSizeBytes: Long,
    val items: List<CachedImageItem>
) {
    val itemCount: Int get() = items.size
}
