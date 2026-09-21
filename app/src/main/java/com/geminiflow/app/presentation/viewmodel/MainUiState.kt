package com.geminiflow.app.presentation.viewmodel

data class MainUiState(
    val isServerRunning: Boolean = false,
    val serverHost: String = "127.0.0.1",
    val serverPort: Int = 5000,
    val isAuthenticated: Boolean = false,
    val isBatteryUnrestricted: Boolean = false,
    val totalRequests: Long = 0,
    val activeConnections: Int = 0,
    val serverErrorMessage: String? = null,
    val isTesting: Boolean = false,
    val testPrompt: String = "",
    val testResponseText: String = "",
    val testResponseImages: List<String> = emptyList(),
    val autoStartOnBoot: Boolean = false,
    val oemTips: String = ""
)
