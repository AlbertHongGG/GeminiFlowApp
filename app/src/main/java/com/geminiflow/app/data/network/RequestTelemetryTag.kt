package com.geminiflow.app.data.network

import java.util.UUID

/**
 * 記憶體級請求遙測標籤。
 * 透過 OkHttp 內部在體傳遞機制 Request.tag(RequestTelemetryTag::class.java) 攜帶，
 * 100% 不接觸 HTTP 傳輸線路，完全免疫字元集限制，符合 RFC 7230 協定規範。
 */
data class RequestTelemetryTag(
    val promptSummary: String? = null,
    val source: String = "API Client",
    val requestId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis()
)
