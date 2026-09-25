package com.geminiflow.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * API 請求日誌實體資料模型。
 */
@Serializable
data class ApiLogItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val statusCode: Int = 200,
    val source: String = "ProxyServer",
    val requestJson: String? = null,
    val responseJson: String? = null,
    val isError: Boolean = statusCode >= 400
)
