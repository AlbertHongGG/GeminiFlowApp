package com.geminiflow.app.service

import android.content.Context
import com.geminiflow.app.domain.model.server.ServerState
import com.geminiflow.app.domain.server.ServerController
import com.geminiflow.app.domain.server.ServerManager
import kotlinx.coroutines.flow.StateFlow

/**
 * Android 平台伺服器控制協調器實作
 * 負責將領域意圖轉換為 Android 前台守護進程（Foreground Service）指令。
 */
class AndroidServerController(
    private val context: Context,
    private val serverManager: ServerManager
) : ServerController {

    override val state: StateFlow<ServerState> = serverManager.state

    override fun start(host: String, port: Int) {
        GeminiForegroundService.startService(context, host, port)
    }

    override fun stop() {
        GeminiForegroundService.stopService(context)
    }
}
