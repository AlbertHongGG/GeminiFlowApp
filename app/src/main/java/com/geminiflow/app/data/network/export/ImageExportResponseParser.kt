package com.geminiflow.app.data.network.export

import com.geminiflow.app.domain.model.common.ImageDownloadException
import org.json.JSONArray

/**
 * c8o8Fe RPC 回應解析器。
 * 負責解析 Batchexecute 傳回的 wrb.fr 協定，偵測伺服器端錯誤碼並提取 2816x1536 高解析度網址。
 */
class ImageExportResponseParser {

    companion object {
        private const val RPC_ID = "c8o8Fe"
        private val PARAM_REGEX = Regex("""=[^=]*$""")
    }

    /**
     * 解析伺服器回應字串。
     * 若伺服器回報錯誤，精準拋出 ImageDownloadException；成功時回傳標準化附帶 =s0 的原圖網址。
     */
    fun parse(responseBody: String): String {
        val lines = responseBody.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("[[") || !trimmed.contains("\"wrb.fr\"") || !trimmed.contains("\"$RPC_ID\"")) {
                continue
            }

            val envelope = try {
                JSONArray(trimmed)
            } catch (_: Exception) {
                continue
            }

            for (i in 0 until envelope.length()) {
                val item = envelope.optJSONArray(i) ?: continue
                if (item.length() < 3) continue
                if (item.optString(0) != "wrb.fr" || item.optString(1) != RPC_ID) continue

                // 檢查伺服器端錯誤代碼 (通常位於 index 5)
                if (item.length() > 5 && !item.isNull(5)) {
                    val errorObj = item.opt(5)
                    throw ImageDownloadException("Google c8o8Fe 原圖導出失敗，伺服器回傳錯誤: $errorObj")
                }

                val payloadStr = item.optString(2, null)
                if (payloadStr.isNullOrBlank()) {
                    throw ImageDownloadException("Google c8o8Fe 回應中無有效內容 (payload 為空)")
                }

                val urlArray = try {
                    JSONArray(payloadStr)
                } catch (e: Exception) {
                    throw ImageDownloadException("解析 c8o8Fe 網址陣列失敗: ${e.message}", e)
                }

                if (urlArray.length() == 0) {
                    throw ImageDownloadException("c8o8Fe 網址陣列為空")
                }

                val rawUrl = urlArray.optString(0, "")
                if (rawUrl.isBlank()) {
                    throw ImageDownloadException("c8o8Fe 第一個元素網址為空")
                }

                // 套用 =s0 保證輸出 2816x1536 無損高解析度
                return normalizeToFullResolution(rawUrl)
            }
        }

        throw ImageDownloadException("未能在伺服器回應中找到有效的 wrb.fr c8o8Fe 資料區塊")
    }

    private fun normalizeToFullResolution(url: String): String {
        val trimmed = url.trim()
        val parts = trimmed.split("?", limit = 2)
        val base = parts[0]
        val query = if (parts.size > 1) "?" + parts[1] else ""

        val rewrittenBase = if (base.contains("=")) {
            base.replace(PARAM_REGEX, "=s0")
        } else {
            "$base=s0"
        }

        return rewrittenBase + query
    }
}
