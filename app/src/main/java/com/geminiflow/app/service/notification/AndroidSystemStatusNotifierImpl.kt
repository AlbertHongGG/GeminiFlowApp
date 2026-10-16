package com.geminiflow.app.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.geminiflow.app.MainActivity
import com.geminiflow.app.R
import com.geminiflow.app.domain.telemetry.model.ServerRequestLifecycleEvent
import com.geminiflow.app.domain.telemetry.notifier.ServerRequestStatusNotifier

/**
 * Android 系統狀態列除錯通知實作
 * 專責管理 Android NotificationManager，與 App 內部 UI Toast (NotificationController) 完全解耦。
 * 隸屬於 service 系統服務組件群，維護與 Android 系統框架之一致性。
 */
class AndroidSystemStatusNotifierImpl(
    private val context: Context,
    private val isEnabledProvider: () -> Boolean,
    private val throttler: StreamingNotificationThrottler = StreamingNotificationThrottler(400L)
) : ServerRequestStatusNotifier {

    companion object {
        private const val TAG = "AndroidSystemStatusNotifier"
        const val CHANNEL_ID = "gemini_flow_system_request_status_channel"
        const val NOTIFICATION_ID = 2001
        const val PREFS_NAME = "gemini_flow_settings"
        const val PREF_KEY_SYSTEM_REQUEST_STATUS_NOTIFICATION_ENABLED = "pref_system_request_status_notification_enabled"
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "系統請求狀態列通知 (Request Status Bar)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "即時在 Android 狀態列顯示外部呼叫生命週期與錯誤責任歸因"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun notifyReceived(event: ServerRequestLifecycleEvent.Received) {
        if (!isEnabledProvider()) return

        try {
            val title = "[GeminiFlow] 接收請求: ${event.endpoint}"
            val modelText = event.model ?: "未指定模型"
            val text = "模型: $modelText | 來源: ${event.clientIp}"
            val bigText = "端點: ${event.endpoint}\n模型: $modelText\n來源 IP: ${event.clientIp}\nPrompt: ${event.promptSnippet ?: "無"}"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setContentIntent(createContentIntent())
                .setOngoing(false)
                .setAutoCancel(true)
                .setProgress(0, 0, true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notifyReceived: ${e.message}", e)
        }
    }

    override fun notifyStreaming(event: ServerRequestLifecycleEvent.Streaming) {
        if (!isEnabledProvider()) return
        if (!throttler.shouldEmit(event.requestId)) return

        try {
            val elapsedSec = String.format("%.1f", event.elapsedMs / 1000.0)
            val title = "[GeminiFlow] 串流處理中 (${event.endpoint})"
            val text = "已接收: ${event.charCount} 字 | 耗時: ${elapsedSec}s"
            val bigText = "端點: ${event.endpoint}\n已生成字數: ${event.charCount} 字\n傳輸分塊: ${event.chunkIndex}\n累計耗時: ${event.elapsedMs}ms"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setContentIntent(createContentIntent())
                .setOngoing(false)
                .setAutoCancel(true)
                .setProgress(0, 0, true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notifyStreaming: ${e.message}", e)
        }
    }

    override fun notifyCompleted(event: ServerRequestLifecycleEvent.Completed) {
        throttler.release(event.requestId)
        if (!isEnabledProvider()) return

        try {
            val title = "[GeminiFlow] 傳輸完畢 (${event.statusCode} OK)"
            val text = "端點: ${event.endpoint} | 共 ${event.totalChars} 字 | 耗時: ${event.durationMs}ms"
            val bigText = "端點: ${event.endpoint}\n狀態: ${event.statusCode} OK\n總字數: ${event.totalChars} 字\n總耗時: ${event.durationMs}ms"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setContentIntent(createContentIntent())
                .setOngoing(false)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notifyCompleted: ${e.message}", e)
        }
    }

    override fun notifyFailed(event: ServerRequestLifecycleEvent.Failed) {
        throttler.release(event.requestId)
        if (!isEnabledProvider()) return

        try {
            val attribution = event.errorAttribution
            val faultTag = if (attribution.isClientFault) {
                "【呼叫端問題 (${attribution.statusCode})】"
            } else {
                "【GeminiFlow 服務端問題 (${attribution.statusCode})】"
            }

            val title = "[GeminiFlow] $faultTag"
            val text = attribution.summary
            val bigText = "端點: ${event.endpoint} (耗時 ${event.durationMs}ms)\n原因: ${attribution.summary}\n\n💡 診斷指引:\n${attribution.diagnosticAction}"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setContentIntent(createContentIntent())
                .setOngoing(false)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notifyFailed: ${e.message}", e)
        }
    }

    override fun clear(requestId: String?) {
        try {
            if (requestId != null) {
                throttler.release(requestId)
            } else {
                throttler.clear()
            }
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear status notification: ${e.message}", e)
        }
    }
}
