package com.geminiflow.app.domain.server

import com.geminiflow.app.domain.model.server.ServerState
import kotlinx.coroutines.flow.StateFlow

/**
 * 伺服器統一控制協調器介面（Server Controller）
 * 作為領域層與表現層/廣播層之間的抽象控制邊界：
 * 1. 統一管理伺服器守護行程的啟動與終止意向。
 * 2. 提供與底層單一真實來源（SSOT）同步的狀態流。
 * 3. 隔離 Android 平台前台服務（Service Intent）實作細節，避免 UI 與背景任務混淆。
 */
interface ServerController {
    /**
     * 伺服器狀態流（SSOT）
     */
    val state: StateFlow<ServerState>

    /**
     * 啟動伺服器守護行程
     *
     * @param host 監聽位址，預設 "127.0.0.1"
     * @param port 監聽通訊埠，預設 5000
     */
    fun start(host: String = "127.0.0.1", port: Int = 5000)

    /**
     * 終止伺服器守護行程
     */
    fun stop()
}
