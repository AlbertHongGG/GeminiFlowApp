package com.geminiflow.app.domain.telemetry.classifier

import com.geminiflow.app.domain.model.common.AttestationException
import com.geminiflow.app.domain.model.common.AuthenticationRequiredException
import com.geminiflow.app.domain.model.common.ChallengeFetchException
import com.geminiflow.app.domain.model.common.ImageDownloadException
import com.geminiflow.app.domain.model.common.NetworkException
import com.geminiflow.app.domain.model.common.PayloadException
import com.geminiflow.app.domain.model.common.RateLimitException
import com.geminiflow.app.domain.model.common.TokenExpiredException
import com.geminiflow.app.domain.model.common.VmExecutionException
import com.geminiflow.app.domain.telemetry.model.ServerRequestErrorAttribution
import kotlinx.serialization.SerializationException

/**
 * 外部請求錯誤診斷與歸因器
 * 純領域服務（Domain Service），負責精確判定異常歸屬責任方（ClientFault vs ServerFault）。
 */
object ServerRequestErrorClassifier {

    fun classify(
        throwable: Throwable?,
        manualStatusCode: Int? = null,
        fallbackMessage: String? = null
    ): ServerRequestErrorAttribution {
        val message = throwable?.message ?: fallbackMessage ?: "未知錯誤"

        // 1. 若手動指定 HTTP 狀態碼，優先根據標準 HTTP 狀態碼規範進行主要分類
        if (manualStatusCode != null) {
            when {
                manualStatusCode == 400 -> {
                    return ServerRequestErrorAttribution.ClientFault(
                        statusCode = 400,
                        summary = message.ifBlank { "請求格式無效 (Bad Request)" },
                        diagnosticAction = "請檢查呼叫端的請求格式、JSON 語法與必要欄位 (如 model、prompt)"
                    )
                }
                manualStatusCode == 401 -> {
                    return ServerRequestErrorAttribution.ServerFault(
                        statusCode = 401,
                        summary = message.ifBlank { "未授權 (Unauthorized)" },
                        diagnosticAction = "Google 登入憑證過期或未登入，請開啟 GeminiFlowApp 完成 Google 授權"
                    )
                }
                manualStatusCode == 404 -> {
                    return ServerRequestErrorAttribution.ClientFault(
                        statusCode = 404,
                        summary = message.ifBlank { "找不到請求端點 (Not Found)" },
                        diagnosticAction = "請檢查呼叫端請求的 URL 路徑是否正確 (例如 /stream 或 /chat)"
                    )
                }
                manualStatusCode == 422 -> {
                    return ServerRequestErrorAttribution.ClientFault(
                        statusCode = 422,
                        summary = message.ifBlank { "內容無法處理 (Unprocessable Entity)" },
                        diagnosticAction = "請檢查呼叫端傳入之圖片 base64 或參數格式是否符合規範"
                    )
                }
                manualStatusCode == 429 -> {
                    return ServerRequestErrorAttribution.ServerFault(
                        statusCode = 429,
                        summary = message.ifBlank { "頻率限制 (Too Many Requests)" },
                        diagnosticAction = "觸發 Google API 頻率限制，請暫緩呼叫頻率並稍候重試"
                    )
                }
                manualStatusCode in 400..499 -> {
                    return ServerRequestErrorAttribution.ClientFault(
                        statusCode = manualStatusCode,
                        summary = message.ifBlank { "呼叫端請求錯誤 (HTTP $manualStatusCode)" },
                        diagnosticAction = "請確認呼叫端的請求參數與標頭格式"
                    )
                }
                manualStatusCode in 500..599 -> {
                    return ServerRequestErrorAttribution.ServerFault(
                        statusCode = manualStatusCode,
                        summary = message.ifBlank { "服務端內部異常 (HTTP $manualStatusCode)" },
                        diagnosticAction = "請開啟 GeminiFlowApp 檢視 API 請求日誌或確認伺服器運作狀態"
                    )
                }
            }
        }

        // 2. 針對領域例外 (Domain Exceptions) 與底層例外進行模式比對
        return when (throwable) {
            is AuthenticationRequiredException -> {
                ServerRequestErrorAttribution.ServerFault(
                    statusCode = 401,
                    summary = throwable.message ?: "未授權：缺少 Google 登入憑證",
                    diagnosticAction = "請開啟 GeminiFlowApp 重新登入 Google 帳號"
                )
            }
            is TokenExpiredException -> {
                ServerRequestErrorAttribution.ServerFault(
                    statusCode = 401,
                    summary = throwable.message ?: "未授權：Gemini Token 已過期",
                    diagnosticAction = "請開啟 GeminiFlowApp 重新整理登入憑證"
                )
            }
            is com.geminiflow.app.domain.model.common.TokenExtractionException -> {
                ServerRequestErrorAttribution.ServerFault(
                    statusCode = 502,
                    summary = throwable.message ?: "無法從 Gemini 首頁解析授權權杖",
                    diagnosticAction = "請檢查網路連線或確認 Google 服務是否正常"
                )
            }
            is RateLimitException -> {
                ServerRequestErrorAttribution.ServerFault(
                    statusCode = 429,
                    summary = throwable.message ?: "觸發 Google API 頻率限制",
                    diagnosticAction = "請求頻率過高，請降低呼叫速率並稍後再試"
                )
            }
            is NetworkException -> {
                ServerRequestErrorAttribution.ServerFault(
                    statusCode = 502,
                    summary = throwable.message ?: "上游網路連線異常",
                    diagnosticAction = "請檢查設備網路連線或代理設定，確認能否連線至 Google 服務"
                )
            }
            is PayloadException -> {
                ServerRequestErrorAttribution.ClientFault(
                    statusCode = 422,
                    summary = throwable.message ?: "請求內容錯誤",
                    diagnosticAction = "請檢查呼叫端的參數規格 (例如 prompt 是否為空、圖片格式是否有效)"
                )
            }
            is AttestationException,
            is ChallengeFetchException,
            is VmExecutionException -> {
                ServerRequestErrorAttribution.ServerFault(
                    statusCode = 500,
                    summary = "設備認證簽章失敗: ${throwable.message}",
                    diagnosticAction = "請檢查系統環境與 Google 授權完整性"
                )
            }
            is ImageDownloadException -> {
                ServerRequestErrorAttribution.ServerFault(
                    statusCode = 502,
                    summary = "圖片資源處理失敗: ${throwable.message}",
                    diagnosticAction = "請確認上傳或引用的圖片資源是否可正常訪問"
                )
            }
            is SerializationException -> {
                ServerRequestErrorAttribution.ClientFault(
                    statusCode = 400,
                    summary = "JSON 格式解析失敗: ${throwable.message}",
                    diagnosticAction = "請檢查呼叫端發送的 JSON 格式與各欄位資料型態"
                )
            }
            is IllegalArgumentException -> {
                ServerRequestErrorAttribution.ClientFault(
                    statusCode = 400,
                    summary = throwable.message ?: "無效的參數",
                    diagnosticAction = "請檢查呼叫端傳入之各項參數是否完整且正確"
                )
            }
            else -> {
                // 檢查是否為 JSON 解析相關訊息
                if (message.contains("JsonConvertException", ignoreCase = true) ||
                    message.contains("deserialize", ignoreCase = true) ||
                    message.contains("Illegal input", ignoreCase = true)
                ) {
                    ServerRequestErrorAttribution.ClientFault(
                        statusCode = 400,
                        summary = "請求內容無法解析為合法的 JSON 結構",
                        diagnosticAction = "請確保 Content-Type 為 application/json 且 Body 符合 ChatRequestDto 格式"
                    )
                } else {
                    ServerRequestErrorAttribution.ServerFault(
                        statusCode = 500,
                        summary = message.ifBlank { "伺服器未預期異常" },
                        diagnosticAction = "請至 GeminiFlowApp 檢視 API 請求日誌並回報問題"
                    )
                }
            }
        }
    }
}
