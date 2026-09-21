package com.geminiflow.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        const val PREF_KEY_AUTO_START = "auto_start_on_boot"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            val prefs = context.getSharedPreferences("gemini_flow_settings", Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean(PREF_KEY_AUTO_START, false)

            if (autoStart) {
                Log.i(TAG, "Device booted and auto-start is enabled. Starting GeminiFlow service...")
                val host = prefs.getString("server_host", "127.0.0.1") ?: "127.0.0.1"
                val port = prefs.getInt("server_port", 5000)
                GeminiForegroundService.startService(context, host, port)
            }
        }
    }
}
