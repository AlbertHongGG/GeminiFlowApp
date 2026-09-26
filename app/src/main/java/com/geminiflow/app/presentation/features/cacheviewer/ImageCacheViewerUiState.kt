package com.geminiflow.app.presentation.features.cacheviewer

import com.geminiflow.app.domain.model.cache.CachedImageItem
import com.geminiflow.app.domain.model.cache.CachedImageMonthGroup

data class ImageCacheViewerUiState(
    val isLoading: Boolean = true,
    val monthGroups: List<CachedImageMonthGroup> = emptyList(),
    val totalCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val totalSizeFormatted: String = "0 B",
    val isExporting: Boolean = false,
    val isDeleting: Boolean = false
)
