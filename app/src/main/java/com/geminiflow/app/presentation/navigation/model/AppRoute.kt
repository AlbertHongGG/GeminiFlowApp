package com.geminiflow.app.presentation.navigation.model

/**
 * 應用程式全域導航路由階層模型。
 * 遵循不可變資料結構與強型別契約，保證導航狀態的確定性與槽位持久化。
 */
sealed interface AppRoute {
    val key: String

    /** 核心主畫面（包含三大底部分頁：儀表板、沙盒、設定） */
    data object Main : AppRoute {
        override val key: String = "route_main"
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
