package com.geminiflow.app.service

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

class BatteryOptimizationHelper(private val context: Context) {

    companion object {
        private const val TAG = "BatteryOptimization"
    }

    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Log.w(TAG, "Direct ignore battery optimization intent failed, falling back to settings", e)
                    openBatterySettings(activity)
                }
            }
        }
    }

    fun openBatterySettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            } else {
                openAppDetailsSettings(context)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open battery optimization settings, opening app details", e)
            openAppDetailsSettings(context)
        }
    }

    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app details settings: ${e.message}", e)
        }
    }

    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER.lowercase()
    }

    fun openOemAutoStartSettings(context: Context): Boolean {
        val brand = getDeviceManufacturer()
        val intents = mutableListOf<Intent>()

        when {
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                    )
                )
            }
            brand.contains("samsung") -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.lool",
                            "com.samsung.android.sm.ui.battery.BatteryActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.sm",
                            "com.samsung.android.sm.ui.battery.BatteryActivity"
                        )
                    )
                )
            }
            brand.contains("huawei") || brand.contains("honor") -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.optimize.process.ProtectActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                        )
                    )
                )
            }
            brand.contains("oppo") || brand.contains("realme") || brand.contains("oneplus") -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.oplus.safecenter",
                            "com.oplus.safecenter.permission.startup.StartupAppListActivity"
                        )
                    )
                )
            }
            brand.contains("vivo") || brand.contains("iqoo") -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.iqoo.secure",
                            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.vivo.permissionmanager",
                            "com.vivo.permissionmanager.activity.PurviewTabActivity"
                        )
                    )
                )
            }
        }

        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                try {
                    context.startActivity(intent)
                    return true
                } catch (_: Exception) {
                }
            }
        }

        // Fallback: open general app details
        openAppDetailsSettings(context)
        return false
    }

    fun getOemGuidanceTips(): String {
        val brand = getDeviceManufacturer()
        return when {
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") ->
                "小米/紅米裝置 (HyperOS / MIUI)：\n1. 請至「應用程式資訊」開啟「自動啟動」\n2. 電池省電策略設置為「無限制」\n3. 多工任務介面下拉鎖定 GeminiFlow 卡片。"
            brand.contains("samsung") ->
                "三星裝置 (OneUI)：\n1. 請至「設定 > 電池與裝置維護 > 電池」\n2. 點擊「背景用量限制」，將 GeminiFlow 加入「永不休眠應用程式」\n3. 應用程式資訊的電池設定改為「不受限制」。"
            brand.contains("huawei") || brand.contains("honor") ->
                "華為/榮耀裝置 (EMUI / HarmonyOS)：\n1. 請至「應用程式啟動管理」將 GeminiFlow 設為「手動管理」\n2. 允許「自啟動」、「關聯啟動」與「後台活動」。"
            brand.contains("oppo") || brand.contains("realme") || brand.contains("oneplus") ->
                "OPPO / Realme / 一加 (ColorOS / OxygenOS)：\n1. 請至「應用程式管理 > 自啟動」允許 GeminiFlow\n2. 電池最佳化改為「不最佳化」\n3. 多工卡片點擊右上角選擇「鎖定」。"
            brand.contains("vivo") || brand.contains("iqoo") ->
                "vivo / iQOO (OriginOS / FuntouchOS)：\n1. 請至「i管家 > 權限管理 > 自啟動」勾選允許\n2. 在「高後台耗電」中允許 GeminiFlow 持續運行。"
            else ->
                "通用 Android 建議：\n1. 電池最佳化設置為「無限制 (Unrestricted)」\n2. 允許「後台數據」與「無限制數據使用」\n3. 在最近任務清單中鎖定此應用程式卡片。"
        }
    }
}
