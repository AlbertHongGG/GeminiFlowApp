package com.geminiflow.app.server.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequestDto(
    val prompt: String,
    @SerialName("system_prompt") val systemPrompt: String? = null,
    val model: String? = null,
    val language: String = "zh-TW",
    val images: List<String> = emptyList(), // Base64 編碼字串或 Data URI
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("auto_refresh_cookies") val autoRefreshCookies: Boolean = true
)

@Serializable
data class ChatResponseDto(
    val text: String,
    val images: List<String> = emptyList()
)

@Serializable
data class ErrorResponseDto(
    val error: String
)

@Serializable
data class HealthResponseDto(
    val ok: Boolean = true
)
