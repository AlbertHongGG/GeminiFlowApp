package com.geminiflow.app.presentation.features.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.domain.model.gallery.GalleryMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GalleryPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GeminiFlowApplication
    private val galleryRepository = app.galleryRepository

    private val _uiState = MutableStateFlow(GalleryPickerUiState())
    val uiState: StateFlow<GalleryPickerUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    // 調整為 48 筆（3 欄網格整除 16 列），避免單批過載並大幅降低記憶體尖峰
    private val pageSize = 48
    private val monthFormat = SimpleDateFormat("M月 yyyy", Locale.CHINESE)

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasPermission = isGranted, permissionRequested = true) }
        if (isGranted && _uiState.value.allAssets.isEmpty()) {
            loadInitial()
        }
    }

    fun loadInitial() {
        if (_uiState.value.isLoading) return
        currentOffset = 0
        _uiState.update {
            it.copy(
                isLoading = true,
                isLoadingMore = false,
                hasMore = true,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            fetchPage(isInitial = true)
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore || !state.hasPermission) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            fetchPage(isInitial = false)
        }
    }

    private suspend fun fetchPage(isInitial: Boolean) {
        try {
            val assets = galleryRepository.getMediaPaged(offset = currentOffset, limit = pageSize)

            if (assets.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, isLoadingMore = false, hasMore = false) }
                return
            }

            // 將耗時的月份格式化與集合組織移至背景執行緒 (Dispatchers.Default)，避免阻塞主線程
            val (newAllAssets, newGroupedAssets, newGroupKeys) = withContext(Dispatchers.Default) {
                val currentAll = if (isInitial) emptyList() else _uiState.value.allAssets
                val combinedAssets = currentAll + assets

                val grouped = LinkedHashMap<String, MutableList<GalleryMediaItem>>()
                val keys = if (isInitial) mutableListOf() else _uiState.value.groupKeys.toMutableList()

                if (!isInitial) {
                    val currentGrouped = _uiState.value.groupedAssets
                    for (k in keys) {
                        grouped[k] = currentGrouped[k]?.toMutableList() ?: mutableListOf()
                    }
                }

                for (asset in assets) {
                    val key = monthFormat.format(Date(asset.dateAddedSec * 1000L))
                    val list = grouped.getOrPut(key) {
                        keys.add(key)
                        mutableListOf()
                    }
                    list.add(asset)
                }

                Triple(combinedAssets, grouped as Map<String, List<GalleryMediaItem>>, keys as List<String>)
            }

            currentOffset += assets.size

            _uiState.update {
                it.copy(
                    allAssets = newAllAssets,
                    groupedAssets = newGroupedAssets,
                    groupKeys = newGroupKeys,
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = assets.size == pageSize
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = "無法讀取相簿：${e.message}"
                )
            }
        }
    }

    fun toggleSelection(item: GalleryMediaItem, maxItems: Int = 4) {
        val current = _uiState.value.selectedItems.toMutableList()
        val exists = current.any { it.id == item.id }

        if (exists) {
            current.removeAll { it.id == item.id }
        } else {
            if (maxItems == 1) {
                current.clear()
                current.add(item)
            } else if (current.size < maxItems) {
                current.add(item)
            }
        }

        val newSelectedIds = current.map { it.id }.toSet()
        _uiState.update {
            it.copy(
                selectedItems = current,
                selectedIds = newSelectedIds
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedItems = emptyList(),
                selectedIds = emptySet()
            )
        }
    }
}
