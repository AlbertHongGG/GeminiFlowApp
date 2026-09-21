package com.geminiflow.app.domain.model

data class ServerStatus(
    val isRunning: Boolean = false,
    val host: String = "127.0.0.1",
    val port: Int = 5000,
    val totalRequests: Long = 0,
    val activeConnections: Int = 0,
    val errorMessage: String? = null
)
