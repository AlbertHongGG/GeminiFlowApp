package com.geminiflow.app.domain.model

import java.util.UUID

/**
 * TrafficLog: Represents a single HTTP request processed by KtorLocalServer.
 */
data class TrafficLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,           // "GET", "POST"
    val path: String,             // "/chat", "/stream", "/health", "/images/..."
    val statusCode: Int,          // 200, 400, 401, 500
    val durationMs: Long,         // Processing time in milliseconds
    val clientIp: String = "127.0.0.1",
    val promptSummary: String? = null,
    val responseSummary: String? = null,
    val isError: Boolean = statusCode >= 400
)
