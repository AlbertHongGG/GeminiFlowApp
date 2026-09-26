package com.geminiflow.app.service

import android.app.Notification
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

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            Log.i(TAG, "Received ACTION_STOP. Stopping server and service...")
            stopServer()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val host = intent?.getStringExtra(EXTRA_HOST) ?: "127.0.0.1"
        val port = intent?.getIntExtra(EXTRA_PORT, 5000) ?: 5000

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

        startServer(host, port)

        return START_STICKY
    }

    private fun startServer(host: String, port: Int) {
        val app = application as GeminiFlowApplication
        if (!app.ktorServer.isRunning) {
            try {
                app.ktorServer.start(host, port)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting server from service: ${e.message}", e)
            }
        }
    }

    private fun stopServer() {
        val app = application as GeminiFlowApplication
        app.ktorServer.stop()
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
            acquire(24 * 60 * 60 * 1000L) // 24 hours max
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
        stopServer()
        releaseWakeLock()
        super.onDestroy()
        Log.i(TAG, "GeminiForegroundService destroyed.")
    }
}
