package com.geminiflow.app.presentation.features.server

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.domain.model.log.TrafficFilter
import com.geminiflow.app.presentation.notification.NotificationController
import com.geminiflow.app.service.GeminiForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ServerHubViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GeminiFlowApplication
    private val ktorServer = app.ktorServer
    private val trafficLogManager = app.trafficLogManager
    private val imageRepository = app.imageRepository
    private val authRepository = app.authRepository

    private val _uiState = MutableStateFlow(ServerHubUiState())
    val uiState: StateFlow<ServerHubUiState> = _uiState.asStateFlow()

    private var uptimeTickerJob: Job? = null

    init {
        loadConfig()
        observeServerStatus()
        observeTrafficLogs()
        observeCacheEvents()
        refreshCacheStats()
    }

    private fun observeCacheEvents() {
        imageRepository.cacheInvalidationEvents
            .onEach { refreshCacheStats() }
            .launchIn(viewModelScope)
    }

    private fun loadConfig() {
        val prefs = app.getSharedPreferences("gemini_flow_settings", Context.MODE_PRIVATE)
        val host = prefs.getString("server_host", "127.0.0.1") ?: "127.0.0.1"
        val port = prefs.getInt("server_port", 5000)
        _uiState.update { it.copy(serverHost = host, serverPort = port) }
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

    private fun observeTrafficLogs() {
        viewModelScope.launch {
            trafficLogManager.logs.collect { logs ->
                _uiState.update { it.copy(trafficLogs = logs) }
            }
        }
    }

    fun refreshCacheStats() {
        val (count, bytes) = imageRepository.getCacheStats()
        _uiState.update { it.copy(cacheFilesCount = count, cacheSizeBytes = bytes) }
    }

    fun toggleServer(context: Context) {
        val currentState = _uiState.value
        if (currentState.isServerRunning) {
            GeminiForegroundService.stopService(context)
        } else {
            if (!authRepository.isAuthenticated.value) {
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

    fun setTrafficFilter(filter: TrafficFilter) {
        _uiState.update { it.copy(trafficFilter = filter) }
    }

    fun clearTrafficLogs() {
        trafficLogManager.clear()
    }
}
