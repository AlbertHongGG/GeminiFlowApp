package com.geminiflow.app.presentation.features.settings

data class SettingsUiState(
    val isAuthenticated: Boolean = false,
    val isBatteryUnrestricted: Boolean = false,
    val autoStartOnBoot: Boolean = false,
    val serverHost: String = "127.0.0.1",
    val serverPort: Int = 5000,
    val cacheFilesCount: Int = 0,
    val cacheSizeBytes: Long = 0L,
    val isNotificationLoggingEnabled: Boolean = true,
    val isApiLoggingEnabled: Boolean = true
) {
    val cacheSizeFormatted: String
        get() = when {
            cacheSizeBytes < 1024 -> "$cacheSizeBytes B"
            cacheSizeBytes < 1024 * 1024 -> String.format("%.1f KB", cacheSizeBytes / 1024.0)
            else -> String.format("%.1f MB", cacheSizeBytes / (1024.0 * 1024.0))
        }
}
