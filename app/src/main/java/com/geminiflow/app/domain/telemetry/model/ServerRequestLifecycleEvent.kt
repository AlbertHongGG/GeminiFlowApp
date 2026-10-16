package com.geminiflow.app.domain.telemetry.model

/**
 * 伺服器外部請求的生命週期遙測事件（純領域模型）
 * 完全解耦於 Android 原生通知，代表外部客戶端發起之 API 請求生命週期各個階段。
 */
sealed class ServerRequestLifecycleEvent {
    abstract val requestId: String
    abstract val endpoint: String

    data class Received(
        override val requestId: String,
        override val endpoint: String,
        val model: String?,
        val promptSnippet: String?,
        val clientIp: String
    ) : ServerRequestLifecycleEvent()

    data class Streaming(
        override val requestId: String,
        override val endpoint: String,
        val charCount: Int,
        val chunkIndex: Int,
        val elapsedMs: Long
    ) : ServerRequestLifecycleEvent()

    data class Completed(
        override val requestId: String,
        override val endpoint: String,
        val totalChars: Int,
        val durationMs: Long,
        val statusCode: Int = 200
    ) : ServerRequestLifecycleEvent()

    data class Failed(
        override val requestId: String,
        override val endpoint: String,
        val errorAttribution: ServerRequestErrorAttribution,
        val durationMs: Long
    ) : ServerRequestLifecycleEvent()
}
