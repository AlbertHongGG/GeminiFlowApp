package com.geminiflow.app.presentation.navigation.model

/**
 * 應用程式全域導航路由階層模型。
 * 遵循不可變資料結構與強型別契約，保證導航狀態的確定性與槽位持久化。
 */
sealed interface AppRoute {
    val key: String

    /** 主控台核心主畫面 (ServerHubScreen) */
    data object Main : AppRoute {
        override val key: String = "route_main"
    }

    /** AI 沙盒延伸介面 (PlaygroundScreen) */
    data object Sandbox : AppRoute {
        override val key: String = "route_sandbox"
    }

    /** 系統設定延伸介面 (SettingsPage) */
    data object Settings : AppRoute {
        override val key: String = "route_settings"
    }

    /** Google 帳號認證登入頁面 */
    data object GoogleAuth : AppRoute {
        override val key: String = "route_google_auth"
    }

    /** 系統通知日誌畫面 */
    data object NotificationLogs : AppRoute {
        override val key: String = "route_notification_logs"
    }

    /** API 請求日誌畫面 */
    data object AiLogs : AppRoute {
        override val key: String = "route_ai_logs"
    }

    /** API 請求日誌詳細資料畫面 */
    data class ApiLogDetail(val rawJson: String) : AppRoute {
        override val key: String = "route_api_log_detail_${rawJson.hashCode()}"
    }
}
