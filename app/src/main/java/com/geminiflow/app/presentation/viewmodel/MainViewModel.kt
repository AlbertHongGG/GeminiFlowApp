package com.geminiflow.app.presentation.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.domain.model.ChatRequest
import com.geminiflow.app.service.BootReceiver
import com.geminiflow.app.service.GeminiForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val app = GeminiFlowApplication.instance
    private val authRepo = app.authRepository
    private val ktorServer = app.ktorServer
    private val batteryHelper = app.batteryOptimizationHelper
    private val streamChatUseCase = app.streamChatUseCase

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeServerStatus()
        observeAuthStatus()
        refreshBatteryStatus()
    }

    private fun loadSettings() {
        val prefs = app.getSharedPreferences("gemini_flow_settings", Context.MODE_PRIVATE)
        val host = prefs.getString("server_host", "127.0.0.1") ?: "127.0.0.1"
        val port = prefs.getInt("server_port", 5000)
        val autoStart = prefs.getBoolean(BootReceiver.PREF_KEY_AUTO_START, false)
        val oemTips = batteryHelper.getOemGuidanceTips()

        _uiState.update {
            it.copy(
                serverHost = host,
                serverPort = port,
                autoStartOnBoot = autoStart,
                oemTips = oemTips
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
                        totalRequests = status.totalRequests,
                        activeConnections = status.activeConnections,
                        serverErrorMessage = status.errorMessage
                    )
                }
            }
        }
    }

    private fun observeAuthStatus() {
        viewModelScope.launch {
            authRepo.isAuthenticated.collect { auth ->
                _uiState.update {
                    it.copy(isAuthenticated = auth)
                }
            }
        }
    }

    fun refreshBatteryStatus() {
        val unrestricted = batteryHelper.isIgnoringBatteryOptimizations()
        _uiState.update { it.copy(isBatteryUnrestricted = unrestricted) }
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

    fun openOemAutoStartSettings(context: Context) {
        batteryHelper.openOemAutoStartSettings(context)
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

    fun runTestPrompt(prompt: String) {
        if (prompt.isBlank()) return

        _uiState.update {
            it.copy(
                isTesting = true,
                testPrompt = prompt,
                testResponseText = "",
                testResponseImages = emptyList()
            )
        }

        viewModelScope.launch {
            try {
                val req = ChatRequest(
                    prompt = prompt,
                    model = "gemini-3-pro"
                )
                val textBuilder = StringBuilder()
                val images = mutableListOf<String>()

                streamChatUseCase(req).collect { chunk ->
                    if (!chunk.text.isNullOrEmpty()) {
                        textBuilder.append(chunk.text)
                        _uiState.update { it.copy(testResponseText = textBuilder.toString()) }
                    }
                    if (!chunk.imageLocalPath.isNullOrEmpty()) {
                        images.add(chunk.imageLocalPath!!)
                        _uiState.update { it.copy(testResponseImages = images) }
                    } else if (!chunk.imageUrl.isNullOrEmpty()) {
                        images.add(chunk.imageUrl!!)
                        _uiState.update { it.copy(testResponseImages = images) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(testResponseText = "測試請求發生錯誤: ${e.message}")
                }
            } finally {
                _uiState.update { it.copy(isTesting = false) }
            }
        }
    }
}
