package com.geminiflow.app.presentation.features.gallery

import com.geminiflow.app.domain.model.gallery.GalleryMediaItem

data class GalleryPickerUiState(
    val hasPermission: Boolean = false,
    val permissionRequested: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val allAssets: List<GalleryMediaItem> = emptyList(),
    val groupedAssets: Map<String, List<GalleryMediaItem>> = emptyMap(),
    val groupKeys: List<String> = emptyList(),
    val selectedItems: List<GalleryMediaItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val errorMessage: String? = null
)
