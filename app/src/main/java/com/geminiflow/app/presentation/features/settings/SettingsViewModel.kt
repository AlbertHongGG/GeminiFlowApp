package com.geminiflow.app.presentation.features.settings

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.service.BootReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GeminiFlowApplication
    private val authRepo = app.authRepository
    private val imageRepository = app.imageRepository
    private val notificationLogManager = app.notificationLogManager
    private val apiLogManager = app.apiLogManager
    private val batteryHelper = app.batteryOptimizationHelper

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeAuthStatus()
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

    private fun observeAuthStatus() {
        viewModelScope.launch {
            authRepo.isAuthenticated.collect { auth ->
                _uiState.update { it.copy(isAuthenticated = auth) }
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
        val (count, bytes) = imageRepository.getCacheStats()
        _uiState.update {
            it.copy(
                cacheFilesCount = count,
                cacheSizeBytes = bytes
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            imageRepository.clearOldImages(0)
            refreshCacheStats()
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
}
