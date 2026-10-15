package com.geminiflow.app.presentation.features.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.domain.model.gallery.GalleryMediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GalleryPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GeminiFlowApplication
    private val galleryRepository = app.galleryRepository

    private val _uiState = MutableStateFlow(GalleryPickerUiState())
    val uiState: StateFlow<GalleryPickerUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val pageSize = 90
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
        if (state.isLoading || !state.hasMore || !state.hasPermission) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            fetchPage(isInitial = false)
        }
    }

    private suspend fun fetchPage(isInitial: Boolean) {
        try {
            val assets = galleryRepository.getMediaPaged(offset = currentOffset, limit = pageSize)

            if (assets.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, hasMore = false) }
                return
            }

            val newAllAssets = if (isInitial) assets else _uiState.value.allAssets + assets
            val newGroupedAssets = if (isInitial) {
                mutableMapOf<String, MutableList<GalleryMediaItem>>()
            } else {
                _uiState.value.groupedAssets.mapValues { it.value.toMutableList() }.toMutableMap()
            }
            val newGroupKeys = if (isInitial) mutableListOf<String>() else _uiState.value.groupKeys.toMutableList()

            for (asset in assets) {
                val key = monthFormat.format(Date(asset.dateAddedSec * 1000L))
                if (!newGroupedAssets.containsKey(key)) {
                    newGroupedAssets[key] = mutableListOf()
                    newGroupKeys.add(key)
                }
                newGroupedAssets[key]?.add(asset)
            }

            currentOffset += assets.size

            _uiState.update {
                it.copy(
                    allAssets = newAllAssets,
                    groupedAssets = newGroupedAssets,
                    groupKeys = newGroupKeys,
                    isLoading = false,
                    hasMore = assets.size == pageSize
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
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

        _uiState.update { it.copy(selectedItems = current) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItems = emptyList()) }
    }
}
