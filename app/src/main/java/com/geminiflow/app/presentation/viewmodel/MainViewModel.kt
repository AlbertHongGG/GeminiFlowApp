package com.geminiflow.app.presentation.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.domain.model.ChatRequest
import com.geminiflow.app.domain.model.PlaygroundChatMessage
import com.geminiflow.app.domain.model.TrafficFilter
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
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                isGenerating = true,
                promptInput = "",
                playgroundMessages = it.playgroundMessages + userMessage + assistantMessage
            )
        }

        playgroundJob = viewModelScope.launch {
            val textBuilder = StringBuilder()
            val images = mutableListOf<String>()

            try {
                val req = ChatRequest(
                    prompt = prompt,
                    model = _uiState.value.selectedModel
                )

                streamChatUseCase(req).collect { chunk ->
                    if (!chunk.text.isNullOrEmpty()) {
                        textBuilder.append(chunk.text)
                    }
                    if (!chunk.imageLocalPath.isNullOrEmpty()) {
                        images.add(chunk.imageLocalPath!!)
                    } else if (!chunk.imageUrl.isNullOrEmpty()) {
                        images.add(chunk.imageUrl!!)
                    }

                    _uiState.update { state ->
                        val updated = state.playgroundMessages.map {
                            if (it.id == assistantMessage.id) {
                                it.copy(
                                    text = textBuilder.toString(),
                                    images = ArrayList(images),
                                    isStreaming = true
                                )
                            } else it
                        }
                        state.copy(playgroundMessages = updated)
                    }
                }
            } catch (e: Exception) {
                textBuilder.append("\n[生成失敗: ${e.message}]")
            } finally {
                _uiState.update { state ->
                    val updated = state.playgroundMessages.map {
                        if (it.id == assistantMessage.id) {
                            it.copy(
                                text = textBuilder.toString(),
                                images = ArrayList(images),
                                isStreaming = false
                            )
                        } else it
                    }
                    state.copy(isGenerating = false, playgroundMessages = updated)
                }
                refreshCacheStats()
            }
        }
    }
}
