package com.geminiflow.app.domain.model

import java.util.UUID

/**
 * 本地伺服器處理之單筆 HTTP 請求日誌記錄。
 */
data class TrafficLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,           // "GET", "POST"
    val path: String,             // "/chat", "/stream", "/health", "/images/..."
    val statusCode: Int,          // 200, 400, 401, 500
    val durationMs: Long,         // 處理耗時（毫秒）
    val clientIp: String = "127.0.0.1",
    val promptSummary: String? = null,
    val responseSummary: String? = null,
    val isError: Boolean = statusCode >= 400
)
