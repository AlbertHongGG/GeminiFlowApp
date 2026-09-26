package com.geminiflow.app.domain.model.log

import java.util.UUID

data class TrafficLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val statusCode: Int,
    val durationMs: Long,
    val clientIp: String = "127.0.0.1",
    val promptSummary: String? = null,
    val responseSummary: String? = null,
    val isError: Boolean = statusCode >= 400
)
