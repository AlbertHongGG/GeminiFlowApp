package com.geminiflow.app.presentation.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.domain.model.ChatRequest
import com.geminiflow.app.domain.model.MediaAsset
import com.geminiflow.app.domain.model.MessageDeliveryState
import com.geminiflow.app.domain.model.PlaygroundChatMessage
import com.geminiflow.app.domain.model.TrafficFilter
import com.geminiflow.app.presentation.notification.NotificationController
import com.geminiflow.app.service.BootReceiver
import com.geminiflow.app.service.GeminiForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val app = GeminiFlowApplication.instance
    private val authRepo = app.authRepository
    private val ktorServer = app.ktorServer
    private val batteryHelper = app.batteryOptimizationHelper
    private val streamChatUseCase = app.streamChatUseCase
    private val trafficLogManager = app.trafficLogManager
    private val notificationLogManager = app.notificationLogManager
    private val apiLogManager = app.apiLogManager
    private val imageStorageManager = app.imageStorageManager

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var uptimeTickerJob: Job? = null
    private var playgroundJob: Job? = null

    init {
        loadSettings()
        observeServerStatus()
        observeAuthStatus()
        observeTrafficLogs()
        refreshBatteryStatus()
        refreshCacheStats()
    }

    private fun loadSettings() {
        val prefs = app.getSharedPreferences("gemini_flow_settings", Context.MODE_PRIVATE)
        val host = prefs.getString("server_host", "127.0.0.1") ?: "127.0.0.1"
        val port = prefs.getInt("server_port", 5000)
        val autoStart = prefs.getBoolean(BootReceiver.PREF_KEY_AUTO_START, false)
        val notifLogging = notificationLogManager.isLoggingEnabled()
        val apiLogging = apiLogManager.isLoggingEnabled()

        _uiState.update {
            it.copy(
                serverHost = host,
                serverPort = port,
                autoStartOnBoot = autoStart,
                isNotificationLoggingEnabled = notifLogging,
                isApiLoggingEnabled = apiLogging
            )
        }
    }

    private fun observeServerStatus() {
        viewModelScope.launch {
            ktorServer.status.collect { status ->
                _uiState.update {
                    it.copy(
                        isServerRunning = status.isRunning,
                        serverHost = status.host,
                        serverPort = status.port,
                        serverStartTime = status.startTime,
                        totalRequests = status.totalRequests,
                        activeConnections = status.activeConnections,
                        serverErrorMessage = status.errorMessage
                    )
                }

                if (status.isRunning && status.startTime != null) {
                    startUptimeTicker(status.startTime)
                } else {
                    stopUptimeTicker()
                }
            }
        }
    }

    private fun startUptimeTicker(startTime: Long) {
        uptimeTickerJob?.cancel()
        uptimeTickerJob = viewModelScope.launch {
            while (isActive) {
                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
                val hours = elapsedSeconds / 3600
                val minutes = (elapsedSeconds % 3600) / 60
                val seconds = elapsedSeconds % 60
                val formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                _uiState.update { it.copy(uptimeFormatted = formatted) }
                delay(1000)
            }
        }
    }

    private fun stopUptimeTicker() {
        uptimeTickerJob?.cancel()
        uptimeTickerJob = null
        _uiState.update { it.copy(uptimeFormatted = "00:00:00") }
    }

    private fun observeAuthStatus() {
        viewModelScope.launch {
            authRepo.isAuthenticated.collect { auth ->
                _uiState.update { it.copy(isAuthenticated = auth) }
            }
        }
    }

    private fun observeTrafficLogs() {
        viewModelScope.launch {
            trafficLogManager.logs.collect { logs ->
                _uiState.update { it.copy(trafficLogs = logs) }
            }
        }
    }


    // 導航核心控制方法 (SSOT)
    fun selectTab(tab: com.geminiflow.app.presentation.navigation.model.AppTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun pushRoute(route: com.geminiflow.app.presentation.navigation.model.AppRoute) {
        _uiState.update { current ->
            current.copy(backStack = current.backStack + route)
        }
    }

    fun popRoute() {
        _uiState.update { current ->
            if (current.backStack.size > 1) {
                current.copy(backStack = current.backStack.dropLast(1))
            } else current
        }
    }

    fun setNotificationLoggingEnabled(enabled: Boolean) {
        notificationLogManager.setLoggingEnabled(enabled)
        _uiState.update { it.copy(isNotificationLoggingEnabled = enabled) }
    }

    fun setApiLoggingEnabled(enabled: Boolean) {
        apiLogManager.setLoggingEnabled(enabled)
        _uiState.update { it.copy(isApiLoggingEnabled = enabled) }
    }

    fun refreshBatteryStatus() {
        val unrestricted = batteryHelper.isIgnoringBatteryOptimizations()
        _uiState.update { it.copy(isBatteryUnrestricted = unrestricted) }
    }

    fun refreshCacheStats() {
        val (count, bytes) = imageStorageManager.getCacheStats()
        _uiState.update {
            it.copy(
                cacheFilesCount = count,
                cacheSizeBytes = bytes
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            imageStorageManager.clearOldImages(0)
            refreshCacheStats()
        }
    }

    fun toggleServer(context: Context) {
        val currentState = _uiState.value
        if (currentState.isServerRunning) {
            GeminiForegroundService.stopService(context)
        } else {
            if (!currentState.isAuthenticated) {
                NotificationController.showWarning("尚未登入 Google 憑證，請至設定完成授權")
                return
            }
            GeminiForegroundService.startService(
                context,
                currentState.serverHost,
                currentState.serverPort
            )
        }
    }

    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        batteryHelper.requestIgnoreBatteryOptimizations(activity)
    }

    fun openBatterySettings(context: Context) {
        batteryHelper.openBatterySettings(context)
    }

    fun openAppDetailsSettings(context: Context) {
        batteryHelper.openAppDetailsSettings(context)
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        val prefs = app.getSharedPreferences("gemini_flow_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(BootReceiver.PREF_KEY_AUTO_START, enabled).apply()
        _uiState.update { it.copy(autoStartOnBoot = enabled) }
    }

    fun updateServerConfig(host: String, port: Int) {
        val prefs = app.getSharedPreferences("gemini_flow_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("server_host", host)
            .putInt("server_port", port)
            .apply()
        _uiState.update { it.copy(serverHost = host, serverPort = port) }
    }

    fun clearAuth() {
        viewModelScope.launch {
            authRepo.clearAuth()
        }
    }

    fun setTrafficFilter(filter: TrafficFilter) {
        _uiState.update { it.copy(trafficFilter = filter) }
    }

    fun clearTrafficLogs() {
        trafficLogManager.clear()
    }

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
                    val reqJson = org.json.JSONObject().apply {
                        put("prompt", prompt)
                        put("model", _uiState.value.selectedModel)
                    }
                    val mediaJsonArray = org.json.JSONArray()
                    for (asset in mediaAssets) {
                        val obj = org.json.JSONObject().apply {
                            put("id", asset.id)
                            put("rawUrl", asset.rawUrl)
                            when (asset) {
                                is MediaAsset.LocalReady -> {
                                    put("status", "SUCCESS")
                                    put("localFilePath", asset.localFile.absolutePath)
                                    put("sizeBytes", asset.sizeBytes)
                                }
                                is MediaAsset.Downloading -> {
                                    put("status", "DOWNLOADING")
                                }
                                is MediaAsset.Failed -> {
                                    put("status", "FAILED")
                                    put("error", asset.errorMessage)
                                }
                            }
                        }
                        mediaJsonArray.put(obj)
                    }
                    val respJson = org.json.JSONObject().apply {
                        put("text", finalResponseText)
                        put("mediaAssets", mediaJsonArray)
                        if (generationError != null) put("error", generationError)
                    }
                    apiLogManager.logInteraction(
                        agentName = "Playground (${_uiState.value.selectedModel})",
                        durationMs = durationMs,
                        request = reqJson,
                        response = respJson
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
                refreshCacheStats()
            }
        }
    }

    fun retryMediaDownload(messageId: String, assetId: String) {
        val targetMessage = _uiState.value.playgroundMessages.firstOrNull { it.id == messageId } ?: return
        val targetAsset = targetMessage.mediaAssets.firstOrNull { it.id == assetId } ?: return
        val rawUrl = targetAsset.rawUrl

        viewModelScope.launch {
            // 1. 設置為 Downloading 狀態
            _uiState.update { state ->
                val updated = state.playgroundMessages.map { msg ->
                    if (msg.id == messageId) {
                        val updatedAssets = msg.mediaAssets.map { asset ->
                            if (asset.id == assetId) {
                                MediaAsset.Downloading(id = assetId, rawUrl = rawUrl)
                            } else asset
                        }
                        msg.copy(mediaAssets = updatedAssets)
                    } else msg
                }
                state.copy(playgroundMessages = updated)
            }

            // 2. 重新調用 imageRepository.downloadImage
            try {
                val downloadedFile = app.imageStorageManager.downloadImage(rawUrl, _uiState.value.selectedModel)
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
                                    MediaAsset.Failed(id = assetId, rawUrl = rawUrl, errorMessage = e.message ?: "重試下載失敗")
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
