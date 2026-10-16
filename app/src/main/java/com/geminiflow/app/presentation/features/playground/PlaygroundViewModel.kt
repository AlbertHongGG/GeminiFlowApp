package com.geminiflow.app.presentation.features.playground

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.model.chat.MediaAsset
import com.geminiflow.app.domain.model.chat.MessageDeliveryState
import com.geminiflow.app.domain.model.chat.ModelFamily
import com.geminiflow.app.domain.model.chat.PlaygroundChatMessage
import com.geminiflow.app.domain.model.chat.ThinkingLevel
import com.geminiflow.app.domain.model.gallery.GalleryMediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.geminiflow.app.domain.model.log.ApiLogRecord
import com.geminiflow.app.domain.model.log.ApiLogRequest
import com.geminiflow.app.domain.model.log.ApiLogResponse
import com.geminiflow.app.presentation.features.playground.streaming.StreamingTypewriterEngine

class PlaygroundViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PlaygroundViewModel"
    }

    private val app = application as GeminiFlowApplication
    private val streamChatUseCase = app.streamChatUseCase
    private val processSelectedImagesUseCase = app.processSelectedImagesUseCase
    private val imageRepository = app.imageRepository
    private val authRepository = app.authRepository
    private val apiLogManager = app.apiLogManager

    private val _uiState = MutableStateFlow(PlaygroundUiState())
    val uiState: StateFlow<PlaygroundUiState> = _uiState.asStateFlow()

    private var playgroundJob: Job? = null
    private var activeTypewriter: StreamingTypewriterEngine? = null

    fun selectFamily(family: ModelFamily) {
        _uiState.update { it.copy(selectedFamily = family) }
    }

    fun selectModel(model: String) {
        selectFamily(ModelFamily.fromModelName(model))
    }

    fun selectThinking(thinking: ThinkingLevel) {
        _uiState.update { it.copy(selectedThinking = thinking) }
    }

    fun updatePromptInput(text: String) {
        _uiState.update { it.copy(promptInput = text) }
    }

    fun addSelectedImages(items: List<GalleryMediaItem>) {
        _uiState.update { state ->
            val existingIds = state.selectedImages.map { it.id }.toSet()
            val newItems = items.filter { it.id !in existingIds }
            state.copy(selectedImages = state.selectedImages + newItems)
        }
    }

    fun removeSelectedImage(item: GalleryMediaItem) {
        _uiState.update { state ->
            state.copy(selectedImages = state.selectedImages.filterNot { it.id == item.id })
        }
    }

    fun clearSelectedImages() {
        _uiState.update { it.copy(selectedImages = emptyList()) }
    }

    fun clearPlaygroundChat() {
        _uiState.update { it.copy(playgroundMessages = emptyList()) }
    }

    fun cancelPlaygroundGeneration() {
        activeTypewriter?.cancel()
        activeTypewriter = null
        playgroundJob?.cancel()
        playgroundJob = null
        _uiState.update { state ->
            val updated = state.playgroundMessages.map {
                if (it.isStreaming) it.copy(isStreaming = false, text = it.text + " [已中斷]") else it
            }
            state.copy(isGenerating = false, playgroundMessages = updated)
        }
    }

    fun sendPlaygroundPrompt() {
        val prompt = _uiState.value.promptInput.trim()
        val imagesToSend = _uiState.value.selectedImages
        if ((prompt.isBlank() && imagesToSend.isEmpty()) || _uiState.value.isGenerating) return

        val userMessage = PlaygroundChatMessage(
            isUser = true,
            text = prompt,
            userImageUris = imagesToSend.map { it.uri }
        )
        val assistantMessage = PlaygroundChatMessage(
            isUser = false,
            text = "",
            isStreaming = true,
            deliveryState = MessageDeliveryState.STREAMING
        )

        _uiState.update {
            it.copy(
                isGenerating = true,
                promptInput = "",
                selectedImages = emptyList(),
                playgroundMessages = it.playgroundMessages + userMessage + assistantMessage
            )
        }

        val startTime = System.currentTimeMillis()
        playgroundJob = viewModelScope.launch {
            val mediaAssets = mutableListOf<MediaAsset>()
            var generationError: String? = null

            // 啟動自適應流式平滑打字機排程器（16ms 週期，最高 60 FPS 逐字輸出）
            val typewriter = StreamingTypewriterEngine(
                scope = viewModelScope,
                baseDelayMs = 16L,
                onTextEmitted = { currentSmoothText ->
                    _uiState.update { state ->
                        val updated = state.playgroundMessages.map {
                            if (it.id == assistantMessage.id) {
                                it.copy(
                                    text = currentSmoothText,
                                    mediaAssets = ArrayList(mediaAssets),
                                    isStreaming = true,
                                    deliveryState = MessageDeliveryState.STREAMING
                                )
                            } else it
                        }
                        state.copy(playgroundMessages = updated)
                    }
                }
            )
            activeTypewriter = typewriter

            try {
                // 安全異步壓縮圖片為 ImagePayload (避免 OOM)
                val imagePayloads = if (imagesToSend.isNotEmpty()) {
                    processSelectedImagesUseCase(imagesToSend.map { it.uri })
                } else {
                    emptyList()
                }

                val effectivePrompt = if (prompt.isBlank() && imagePayloads.isNotEmpty()) {
                    "請分析這張圖片的內容。"
                } else {
                    prompt
                }

                val req = ChatRequest(
                    prompt = effectivePrompt,
                    images = imagePayloads,
                    model = _uiState.value.selectedModel,
                    thinking = _uiState.value.selectedThinking
                )

                streamChatUseCase(req).collect { chunk ->
                    if (!chunk.text.isNullOrEmpty()) {
                        typewriter.enqueue(chunk.text)
                    }

                    if (chunk.mediaAsset != null) {
                        val asset = chunk.mediaAsset
                        val existingIndex = mediaAssets.indexOfFirst { it.id == asset.id }
                        if (existingIndex >= 0) {
                            mediaAssets[existingIndex] = asset
                        } else {
                            mediaAssets.add(asset)
                        }

                        _uiState.update { state ->
                            val updated = state.playgroundMessages.map {
                                if (it.id == assistantMessage.id) {
                                    it.copy(
                                        mediaAssets = ArrayList(mediaAssets)
                                    )
                                } else it
                            }
                            state.copy(playgroundMessages = updated)
                        }
                    }
                }

                // 標記網路串流結束，等待打字機平滑排程將緩衝區字元全數打完
                typewriter.complete()
                typewriter.awaitCompletion()
            } catch (e: Exception) {
                generationError = e.message ?: "連線或生成失敗"
                Log.e(TAG, "Playground generation failed: $generationError", e)
                typewriter.cancel()
            } finally {
                val durationMs = System.currentTimeMillis() - startTime
                val finalResponseText = typewriter.getEmittedText()
                activeTypewriter = null

                try {
                    val readyImages = mediaAssets.filterIsInstance<MediaAsset.LocalReady>().map { it.localFile.name }
                    apiLogManager.logInteraction(
                        ApiLogRecord(
                            timestamp = "",
                            agentName = "Playground (${_uiState.value.selectedModel})",
                            durationMs = durationMs,
                            request = ApiLogRequest(
                                model = _uiState.value.selectedModel,
                                prompt = prompt
                            ),
                            response = ApiLogResponse(
                                text = finalResponseText.takeIf { it.isNotBlank() },
                                images = readyImages.takeIf { it.isNotEmpty() },
                                error = generationError
                            )
                        )
                    )
                } catch (logEx: Exception) {
                    Log.w(TAG, "Failed to record sandbox API log: ${logEx.message}")
                }

                val anyFailed = mediaAssets.any { it is MediaAsset.Failed }
                val anyReady = mediaAssets.any { it is MediaAsset.LocalReady }
                val finalState = when {
                    generationError != null -> MessageDeliveryState.FAILED
                    finalResponseText.isEmpty() && mediaAssets.isEmpty() -> MessageDeliveryState.FAILED
                    finalResponseText.isEmpty() && anyFailed && !anyReady -> MessageDeliveryState.FAILED
                    else -> MessageDeliveryState.SUCCESS
                }
                val finalErrMsg = generationError ?: if (finalResponseText.isEmpty() && mediaAssets.isEmpty()) {
                    "伺服器未回傳有效輸出或圖片資源取得失敗"
                } else if (finalResponseText.isEmpty() && anyFailed && !anyReady) {
                    mediaAssets.filterIsInstance<MediaAsset.Failed>().firstOrNull()?.errorMessage ?: "圖片下載失敗"
                } else null

                _uiState.update { state ->
                    val updated = state.playgroundMessages.map {
                        if (it.id == assistantMessage.id) {
                            it.copy(
                                text = finalResponseText,
                                mediaAssets = ArrayList(mediaAssets),
                                isStreaming = false,
                                deliveryState = finalState,
                                errorMessage = finalErrMsg
                            )
                        } else it
                    }
                    state.copy(isGenerating = false, playgroundMessages = updated)
                }
            }
        }
    }

    fun retryMediaDownload(messageId: String, assetId: String) {
        val targetMessage = _uiState.value.playgroundMessages.firstOrNull { it.id == messageId } ?: return
        val targetAsset = targetMessage.mediaAssets.firstOrNull { it.id == assetId } ?: return
        val rawUrl = targetAsset.rawUrl
        val targetMetadata = (targetAsset as? MediaAsset.Failed)?.exportMetadata

        viewModelScope.launch {
            _uiState.update { state ->
                val updated = state.playgroundMessages.map { msg ->
                    if (msg.id == messageId) {
                        val updatedAssets = msg.mediaAssets.map { asset ->
                            if (asset.id == assetId) {
                                MediaAsset.Downloading(id = assetId, rawUrl = rawUrl, exportMetadata = targetMetadata)
                            } else asset
                        }
                        msg.copy(mediaAssets = updatedAssets)
                    } else msg
                }
                state.copy(playgroundMessages = updated)
            }

            try {
                val downloadedFile = if (targetMetadata != null) {
                    val tokens = authRepository.ensureValidTokens()
                    val cookies = authRepository.getGoogleCookies()
                    imageRepository.downloadExportedImage(
                        metadata = targetMetadata,
                        modelName = _uiState.value.selectedModel,
                        tokens = tokens,
                        cookies = cookies
                    )
                } else {
                    imageRepository.downloadImage(
                        url = rawUrl,
                        modelName = _uiState.value.selectedModel
                    )
                }
                _uiState.update { state ->
                    val updated = state.playgroundMessages.map { msg ->
                        if (msg.id == messageId) {
                            val updatedAssets = msg.mediaAssets.map { asset ->
                                if (asset.id == assetId) {
                                    MediaAsset.LocalReady(id = assetId, rawUrl = rawUrl, localFile = downloadedFile)
                                } else asset
                            }
                            msg.copy(
                                mediaAssets = updatedAssets,
                                deliveryState = MessageDeliveryState.SUCCESS,
                                errorMessage = null
                            )
                        } else msg
                    }
                    state.copy(playgroundMessages = updated)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Retry download failed for $rawUrl: ${e.message}", e)
                _uiState.update { state ->
                    val updated = state.playgroundMessages.map { msg ->
                        if (msg.id == messageId) {
                            val updatedAssets = msg.mediaAssets.map { asset ->
                                if (asset.id == assetId) {
                                    MediaAsset.Failed(
                                        id = assetId,
                                        rawUrl = rawUrl,
                                        errorMessage = e.message ?: "重試下載失敗",
                                        exportMetadata = targetMetadata
                                    )
                                } else asset
                            }
                            msg.copy(mediaAssets = updatedAssets)
                        } else msg
                    }
                    state.copy(playgroundMessages = updated)
                }
            }
        }
    }
}
