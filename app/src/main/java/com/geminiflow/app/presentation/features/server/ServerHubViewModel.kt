package com.geminiflow.app.presentation.features.server

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.domain.model.log.TrafficFilter
import com.geminiflow.app.domain.model.server.ServerState
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

/**
 * 伺服器中心 ViewModel（ServerHubViewModel）
 * 完全對接 ServerManager 領域服務，支援非同步 IO 調度與五態狀態機。
 */
class ServerHubViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GeminiFlowApplication
    private val serverManager = app.serverManager
    private val trafficLogManager = app.trafficLogManager
    private val imageRepository = app.imageRepository
    private val authRepository = app.authRepository

    private val _uiState = MutableStateFlow(ServerHubUiState())
    val uiState: StateFlow<ServerHubUiState> = _uiState.asStateFlow()

    private var uptimeTickerJob: Job? = null

    init {
        loadConfig()
        observeServerState()
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

    private fun observeServerState() {
        viewModelScope.launch {
            serverManager.state.collect { state ->
                _uiState.update {
                    it.copy(
                        serverState = state,
                        serverHost = state.host,
                        serverPort = state.port,
                        totalRequests = (state as? ServerState.Running)?.totalRequests ?: it.totalRequests,
                        activeConnections = (state as? ServerState.Running)?.activeConnections ?: 0
                    )
                }

                if (state is ServerState.Running) {
                    startUptimeTicker(state.startTime)
                } else if (state is ServerState.Stopped || state is ServerState.Failed) {
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

    fun toggleServer() {
        val currentState = _uiState.value.serverState

        // 防連點機制：若伺服器正處於過渡中（Starting 或 Stopping），忽略本次點擊
        if (currentState.isTransitioning) {
            return
        }

        viewModelScope.launch {
            if (currentState.isRunning) {
                // 停止服務：委託 ServerManager 非同步停止引擎，前台服務感知 Stopped 狀態後自動終止
                serverManager.stopServer()
            } else {
                if (!authRepository.isAuthenticated.value) {
                    NotificationController.showWarning("尚未登入 Google 憑證，請至設定完成授權")
                    return@launch
                }
                // 啟動服務：喚醒前台保活服務並由 ServerManager 非同步啟動引擎
                GeminiForegroundService.startService(
                    app,
                    currentState.host,
                    currentState.port
                )
                serverManager.startServer(
                    currentState.host,
                    currentState.port
                )
            }
        }
    }

    fun setTrafficFilter(filter: TrafficFilter) {
        _uiState.update { it.copy(trafficFilter = filter) }
    }

    fun clearTrafficLogs() {
        trafficLogManager.clear()
    }
}
