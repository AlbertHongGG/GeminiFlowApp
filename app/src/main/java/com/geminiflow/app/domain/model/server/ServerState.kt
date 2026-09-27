package com.geminiflow.app.domain.model.server

/**
 * 伺服器五態非同步狀態機（Server State Machine）
 * 徹底揚棄傳統二元 isRunning 設計，完整支援過渡態（Starting / Stopping）與失敗態。
 */
sealed interface ServerState {
    val isRunning: Boolean
        get() = this is Running

    val isTransitioning: Boolean
        get() = this is Starting || this is Stopping

    val host: String
        get() = when (this) {
            is Starting -> host
            is Running -> host
            is Failed -> host
            is Stopped, Stopping -> "127.0.0.1"
        }

    val port: Int
        get() = when (this) {
            is Starting -> port
            is Running -> port
            is Failed -> port
            is Stopped, Stopping -> 5000
        }

    data object Stopped : ServerState

    data class Starting(
        override val host: String,
        override val port: Int
    ) : ServerState

    data class Running(
        override val host: String,
        override val port: Int,
        val startTime: Long,
        val totalRequests: Long = 0,
        val activeConnections: Int = 0
    ) : ServerState

    data object Stopping : ServerState

    data class Failed(
        val error: String,
        override val host: String = "127.0.0.1",
        override val port: Int = 5000
    ) : ServerState
}
