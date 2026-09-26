package com.geminiflow.app.presentation.features.cacheviewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.domain.model.cache.CachedImageItem
import com.geminiflow.app.domain.model.cache.CachedImageMonthGroup
import com.geminiflow.app.presentation.notification.NotificationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class ImageCacheViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GeminiFlowApplication
    private val imageRepository = app.imageRepository
    private val mediaExporter = app.mediaExporter

    private val _uiState = MutableStateFlow(ImageCacheViewerUiState())
    val uiState: StateFlow<ImageCacheViewerUiState> = _uiState.asStateFlow()

    init {
        loadImages()
        observeCacheEvents()
    }

    private fun observeCacheEvents() {
        imageRepository.cacheInvalidationEvents
            .onEach { loadImages() }
            .launchIn(viewModelScope)
    }

    fun loadImages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val images = imageRepository.getAllCachedImages()

            val groups = images
                .groupBy { it.yearMonthKey }
                .map { (key, items) ->
                    CachedImageMonthGroup(
                        yearMonthKey = key,
                        yearMonthDisplay = items.firstOrNull()?.yearMonthDisplay ?: key,
                        totalSizeBytes = items.sumOf { it.sizeBytes },
                        items = items
                    )
                }
                .sortedByDescending { it.yearMonthKey }

            val totalBytes = images.sumOf { it.sizeBytes }
            val formattedSize = formatBytes(totalBytes)

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    monthGroups = groups,
                    totalCount = images.size,
                    totalSizeBytes = totalBytes,
                    totalSizeFormatted = formattedSize
                )
            }
        }
    }

    fun deleteImage(item: CachedImageItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            val success = imageRepository.deleteCachedImage(item.filename)
            if (success) {
                NotificationController.showSuccess("已刪除該快取圖片")
            } else {
                NotificationController.showError("刪除圖片失敗")
            }
            _uiState.update { it.copy(isDeleting = false) }
        }
    }

    fun downloadImage(item: CachedImageItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = mediaExporter.exportImageToGallery(item.file, item.filename)
            result.fold(
                onSuccess = {
                    NotificationController.showSuccess("已成功將圖片儲存至相簿")
                },
                onFailure = { error ->
                    NotificationController.showError("儲存失敗: ${error.message ?: "未知錯誤"}")
                }
            )
            _uiState.update { it.copy(isExporting = false) }
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
