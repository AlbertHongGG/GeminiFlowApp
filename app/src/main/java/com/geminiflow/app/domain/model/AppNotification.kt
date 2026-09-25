package com.geminiflow.app.domain.model

import java.util.UUID

/**
 * 系統通知型別。
 */
enum class NotificationType {
    SUCCESS,
    ERROR,
    INFO,
    WARNING
}

/**
 * 應用程式全域通知資料模型。
 */
data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: NotificationType,
    val durationMs: Long = 4000L,
    val timestamp: Long = System.currentTimeMillis()
)
