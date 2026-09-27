package com.geminiflow.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.MainActivity
import com.geminiflow.app.R
import com.geminiflow.app.domain.model.server.ServerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 響應式本地伺服器前台保活守護進程（Reactive Server Foreground Daemon）
 * 職責純化：
 * 1. 僅負責 Android 前台通知與 WakeLock 保活，不介入伺服器的阻塞啟停。
 * 2. 主執行緒 0ms 負擔：所有操作皆由協程非同步調度至 Dispatchers.IO。
 * 3. 響應式監聽 ServerManager.state：當伺服器停止或異常時自動卸載通知與自我銷毀。
 */
class GeminiForegroundService : Service() {

    companion object {
        private const val TAG = "GeminiForegroundService"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.geminiflow.app.action.START_SERVICE"
        const val ACTION_STOP = "com.geminiflow.app.action.STOP_SERVICE"

        const val EXTRA_HOST = "extra_host"
        const val EXTRA_PORT = "extra_port"

        fun startService(context: Context, host: String = "127.0.0.1", port: Int = 5000) {
            val intent = Intent(context, GeminiForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_HOST, host)
                putExtra(EXTRA_PORT, port)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, GeminiForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        observeServerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val app = application as GeminiFlowApplication

        if (action == ACTION_STOP) {
            Log.i(TAG, "Received ACTION_STOP. Delegating stop to ServerManager on Dispatchers.IO...")
            serviceScope.launch(Dispatchers.IO) {
                app.serverManager.stopServer()
            }
            return START_NOT_STICKY
        }

        val host = intent?.getStringExtra(EXTRA_HOST) ?: "127.0.0.1"
        val port = intent?.getIntExtra(EXTRA_PORT, 5000) ?: 5000

        // 立即展示前台通知（符合 Android 8.0+ 規範，避免 ANR）
        val notification = buildNotification(host, port)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    private fun observeServerState() {
        val app = application as GeminiFlowApplication
        serviceScope.launch {
            app.serverManager.state.collect { state ->
                when (state) {
                    is ServerState.Stopped, is ServerState.Failed -> {
                        Log.i(TAG, "ServerState is $state. Tearing down foreground notification and stopping service.")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    is ServerState.Running -> {
                        val notification = buildNotification(state.host, state.port)
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIFICATION_ID, notification)
                    }
                    is ServerState.Starting, is ServerState.Stopping -> {
                        // 過渡期間維持當前前台狀態
                    }
                }
            }
        }
    }

    private fun buildNotification(host: String, port: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GeminiForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = getString(R.string.service_running_desc, host, port)

        return NotificationCompat.Builder(this, GeminiFlowApplication.CHANNEL_ID)
            .setContentTitle(getString(R.string.service_running_title))
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_stop_service),
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GeminiFlowApp:ServerWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(24 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceScope.cancel()
        Log.i(TAG, "GeminiForegroundService destroyed cleanly.")
    }
}
