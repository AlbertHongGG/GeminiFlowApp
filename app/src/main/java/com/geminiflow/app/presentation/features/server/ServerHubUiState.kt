package com.geminiflow.app.presentation.features.server

import com.geminiflow.app.domain.model.log.TrafficFilter
import com.geminiflow.app.domain.model.log.TrafficLog
import com.geminiflow.app.domain.model.server.ServerState

/**
 * 伺服器中心 UI 狀態
 * 以 serverState: ServerState 為唯一真實來源（SSOT），消滅分散布林變數。
 */
data class ServerHubUiState(
    val serverState: ServerState = ServerState.Stopped,
    val serverHost: String = "127.0.0.1",
    val serverPort: Int = 5000,
    val uptimeFormatted: String = "00:00:00",
    val totalRequests: Long = 0,
    val activeConnections: Int = 0,
    val cacheFilesCount: Int = 0,
    val cacheSizeBytes: Long = 0L,
    val trafficLogs: List<TrafficLog> = emptyList(),
    val trafficFilter: TrafficFilter = TrafficFilter.ALL
) {
    val isServerRunning: Boolean
        get() = serverState.isRunning

    val isServerTransitioning: Boolean
        get() = serverState.isTransitioning

    val serverErrorMessage: String?
        get() = (serverState as? ServerState.Failed)?.error

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
