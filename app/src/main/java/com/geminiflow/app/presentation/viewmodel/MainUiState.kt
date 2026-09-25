package com.geminiflow.app.presentation.viewmodel

import com.geminiflow.app.domain.model.PlaygroundChatMessage
import com.geminiflow.app.domain.model.TrafficFilter
import com.geminiflow.app.domain.model.TrafficLog

import com.geminiflow.app.presentation.components.AppTab
import com.geminiflow.app.presentation.navigation.model.AppRoute

data class MainUiState(
    // 全域導航與分頁狀態 (Single Source of Truth)
    val activeTab: AppTab = AppTab.DASHBOARD,
    val backStack: List<AppRoute> = listOf(AppRoute.Main),

    // 伺服器核心狀態
    val isServerRunning: Boolean = false,
    val serverHost: String = "127.0.0.1",
    val serverPort: Int = 5000,
    val serverStartTime: Long? = null,
    val uptimeFormatted: String = "00:00:00",
    val totalRequests: Long = 0,
    val activeConnections: Int = 0,
    val serverErrorMessage: String? = null,

    // 帳號認證與電源安全
    val isAuthenticated: Boolean = false,
    val isBatteryUnrestricted: Boolean = false,
    val autoStartOnBoot: Boolean = false,

    // 快取與磁碟空間
    val cacheFilesCount: Int = 0,
    val cacheSizeBytes: Long = 0L,

    // 網路流量與日誌監控
    val trafficLogs: List<TrafficLog> = emptyList(),
    val trafficFilter: TrafficFilter = TrafficFilter.ALL,
    val isNotificationLoggingEnabled: Boolean = true,
    val isApiLoggingEnabled: Boolean = true,

    // 互動沙盒
    val selectedModel: String = "gemini-3-pro",
    val promptInput: String = "請用繁體中文自我介紹，並告訴我你支援什麼功能。",
    val playgroundMessages: List<PlaygroundChatMessage> = emptyList(),
    val isGenerating: Boolean = false
) {
    val cacheSizeFormatted: String
        get() = when {
            cacheSizeBytes < 1024 -> "$cacheSizeBytes B"
            cacheSizeBytes < 1024 * 1024 -> String.format("%.1f KB", cacheSizeBytes / 1024.0)
            else -> String.format("%.1f MB", cacheSizeBytes / (1024.0 * 1024.0))
        }

    val filteredLogs: List<TrafficLog>
        get() = when (trafficFilter) {
            TrafficFilter.ALL -> trafficLogs
            TrafficFilter.STREAM_ONLY -> trafficLogs.filter { it.path.contains("/stream") }
            TrafficFilter.ERROR_ONLY -> trafficLogs.filter { it.isError }
        }
}
