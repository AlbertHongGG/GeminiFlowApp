package com.geminiflow.app.domain.telemetry.model

/**
 * 外部請求錯誤責任歸因模型
 * 用於明確診斷錯誤是呼叫端 (ClientFault) 還是 GeminiFlowApp/上游服務 (ServerFault)。
 */
sealed class ServerRequestErrorAttribution {
    abstract val statusCode: Int
    abstract val summary: String
    abstract val diagnosticAction: String
    abstract val isClientFault: Boolean

    /**
     * 呼叫端問題 (4xx)：例如缺少必要欄位、JSON 格式錯誤、參數不合法
     */
    data class ClientFault(
        override val statusCode: Int,
        override val summary: String,
        override val diagnosticAction: String = "請檢查呼叫端的請求格式、JSON 語意或必要欄位"
    ) : ServerRequestErrorAttribution() {
        override val isClientFault: Boolean = true
    }

    /**
     * 服務端問題 (401, 429, 5xx, 網路異常)：例如 Google 憑證過期、連線超時、Gemini 雲端異常
     */
    data class ServerFault(
        override val statusCode: Int,
        override val summary: String,
        override val diagnosticAction: String
    ) : ServerRequestErrorAttribution() {
        override val isClientFault: Boolean = false
    }
}
