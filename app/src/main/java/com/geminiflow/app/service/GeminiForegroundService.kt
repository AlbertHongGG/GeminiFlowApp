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
import kotlinx.coroutines.withContext

/**
 * 現代化本地伺服器前台保活守護進程（Server Daemon Host）
 *
 * 架構職責（Execution Plane）：
 * 1. 唯一宿主權威（Host Authority）：作為 Ktor 伺服器在 Android 系統層級的真實執行主體，
 *    伺服器生命週期由本守護進程專屬協程管理，徹底脫離 Activity / ViewModel 的臨時生命週期。
 * 2. 5 秒 ANR 免疫：onStartCommand 進入瞬間立即展示 Foreground Notification。
 * 3. 自我修復（Self-Healing）：使用 START_STICKY，在系統遭遇極端記憶體壓力暫時回收後，
 *    由 Android 系統在資源釋放時自動拉起重啟，持久維持 Port 5000 監聽。
 * 4. 零競爭狀態同步：Service 主動發起與調度啟停，廢除初始 Stopped 狀態下的自殺錯誤。
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
        Log.i(TAG, "GeminiForegroundService onCreate: Initializing daemon host...")
        acquireWakeLock()
        observeServerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val app = application as GeminiFlowApplication

        if (action == ACTION_STOP) {
            Log.i(TAG, "Received ACTION_STOP. Gracefully shutting down Ktor server and daemon...")
            serviceScope.launch(Dispatchers.IO) {
                app.serverManager.stopServer()
                withContext(Dispatchers.Main) {
                    app.serverRequestStatusNotifier.clear()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    Log.i(TAG, "Server stopped and daemon self-terminated cleanly.")
                }
            }
            return START_NOT_STICKY
        }

        // ACTION_START 或系統殺死後依據 START_STICKY 自動拉起（intent 為 null）
        val prefs = getSharedPreferences("gemini_flow_settings", Context.MODE_PRIVATE)
        val host = intent?.getStringExtra(EXTRA_HOST)
            ?: prefs.getString("server_host", "127.0.0.1")
            ?: "127.0.0.1"
        val port = intent?.getIntExtra(EXTRA_PORT, -1)?.takeIf { it > 0 }
            ?: prefs.getInt("server_port", 5000)

        // 立即展示前台通知（符合 Android 8.0+ 規範，杜絕 5 秒 ANR）
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

        // 由前台服務協程作為真實宿主，非同步調度 Ktor 啟動
        Log.i(TAG, "GeminiForegroundService: Hosting and launching Ktor server on http://$host:$port...")
        serviceScope.launch(Dispatchers.IO) {
            val result = app.serverManager.startServer(host, port)
            if (result.isFailure) {
                Log.e(TAG, "Failed to start Ktor server via Daemon: ${result.exceptionOrNull()?.message}")
            } else {
                Log.i(TAG, "Ktor server successfully started and bound to http://$host:$port under daemon protection.")
            }
        }

        return START_STICKY
    }

    private fun observeServerState() {
        val app = application as GeminiFlowApplication
        serviceScope.launch {
            app.serverManager.state.collect { state ->
                when (state) {
                    is ServerState.Running -> {
                        val notification = buildNotification(state.host, state.port)
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIFICATION_ID, notification)
                    }
                    is ServerState.Failed -> {
                        Log.w(TAG, "ServerState reported failure: ${state.error}")
                        val notification = buildFailedNotification(state.error)
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIFICATION_ID, notification)
                    }
                    is ServerState.Starting, is ServerState.Stopping, is ServerState.Stopped -> {
                        // 正常狀態轉變維持當前前景守護，停止流程由 ACTION_STOP 專責管線處理
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

    private fun buildFailedNotification(errorMessage: String): Notification {
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

        return NotificationCompat.Builder(this, GeminiFlowApplication.CHANNEL_ID)
            .setContentTitle("伺服器異常中斷")
            .setContentText(errorMessage)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_stop_service),
                stopPendingIntent
            )
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        val app = application as? GeminiFlowApplication
        runCatching {
            app?.serverRequestStatusNotifier?.clear()
        }
        // 防禦性清理：若服務意外被系統終止且伺服器仍在運行，關閉伺服器
        if (app?.serverManager?.state?.value is ServerState.Running) {
            CoroutineScope(Dispatchers.IO).launch {
                app.serverManager.stopServer()
            }
        }
        serviceScope.cancel()
        Log.i(TAG, "GeminiForegroundService destroyed cleanly.")
    }
}
