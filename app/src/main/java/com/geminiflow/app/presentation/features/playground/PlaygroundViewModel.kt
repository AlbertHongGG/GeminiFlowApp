package com.geminiflow.app.presentation.features.playground

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.model.chat.MediaAsset
import com.geminiflow.app.domain.model.chat.MessageDeliveryState
import com.geminiflow.app.domain.model.chat.PlaygroundChatMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.geminiflow.app.domain.model.log.ApiLogRecord
import com.geminiflow.app.domain.model.log.ApiLogRequest
import com.geminiflow.app.domain.model.log.ApiLogResponse

class PlaygroundViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PlaygroundViewModel"
    }

    private val app = application as GeminiFlowApplication
    private val streamChatUseCase = app.streamChatUseCase
    private val imageRepository = app.imageRepository
    private val authRepository = app.authRepository
    private val apiLogManager = app.apiLogManager

    private val _uiState = MutableStateFlow(PlaygroundUiState())
    val uiState: StateFlow<PlaygroundUiState> = _uiState.asStateFlow()

    private var playgroundJob: Job? = null

    fun selectModel(model: String) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun updatePromptInput(text: String) {
        _uiState.update { it.copy(promptInput = text) }
    }

    fun clearPlaygroundChat() {
        _uiState.update { it.copy(playgroundMessages = emptyList()) }
    }

    fun cancelPlaygroundGeneration() {
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
        if (prompt.isBlank() || _uiState.value.isGenerating) return

        val userMessage = PlaygroundChatMessage(
            isUser = true,
            text = prompt
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
                playgroundMessages = it.playgroundMessages + userMessage + assistantMessage
            )
        }

        val startTime = System.currentTimeMillis()
        playgroundJob = viewModelScope.launch {
            val textBuilder = StringBuilder()
            val mediaAssets = mutableListOf<MediaAsset>()
            var generationError: String? = null

            try {
                val req = ChatRequest(
                    prompt = prompt,
                    model = _uiState.value.selectedModel
                )

                streamChatUseCase(req).collect { chunk ->
                    if (!chunk.text.isNullOrEmpty()) {
                        textBuilder.append(chunk.text)
                    }

                    if (chunk.mediaAsset != null) {
                        val asset = chunk.mediaAsset
                        val existingIndex = mediaAssets.indexOfFirst { it.id == asset.id }
                        if (existingIndex >= 0) {
                            mediaAssets[existingIndex] = asset
                        } else {
                            mediaAssets.add(asset)
                        }
                    }

                    _uiState.update { state ->
                        val updated = state.playgroundMessages.map {
                            if (it.id == assistantMessage.id) {
                                it.copy(
                                    text = textBuilder.toString(),
                                    mediaAssets = ArrayList(mediaAssets),
                                    isStreaming = true,
                                    deliveryState = MessageDeliveryState.STREAMING
                                )
                            } else it
                        }
                        state.copy(playgroundMessages = updated)
                    }
                }
            } catch (e: Exception) {
                generationError = e.message ?: "連線或生成失敗"
                Log.e(TAG, "Playground generation failed: $generationError", e)
            } finally {
                val durationMs = System.currentTimeMillis() - startTime
                val finalResponseText = textBuilder.toString()

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
                                text = textBuilder.toString(),
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
