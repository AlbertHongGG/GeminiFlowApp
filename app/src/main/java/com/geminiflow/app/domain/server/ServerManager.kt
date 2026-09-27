package com.geminiflow.app.domain.server

import com.geminiflow.app.domain.model.server.ServerState
import kotlinx.coroutines.flow.StateFlow

/**
 * 領域核心層：本地伺服器控制器介面（Server Manager）
 * 提供非同步協程操作與響應式單一真實來源（SSOT）狀態流。
 * 所有啟停方法保證在背景執行緒中非阻塞調度。
 */
interface ServerManager {
    /**
     * 伺服器狀態流（單一真實來源）
     */
    val state: StateFlow<ServerState>

    /**
     * 非同步啟動伺服器（切換至 Dispatchers.IO 執行，主執行緒 0ms 負擔）
     */
    suspend fun startServer(host: String = "127.0.0.1", port: Int = 5000): Result<Unit>

    /**
     * 非同步停止伺服器（切換至 Dispatchers.IO 執行，主執行緒 0ms 負擔）
     */
    suspend fun stopServer(): Result<Unit>
}
