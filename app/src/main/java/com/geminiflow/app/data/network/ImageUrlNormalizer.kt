package com.geminiflow.app.data.network

/**
 * 圖片網址規格化與 CDN 參數轉換契約。
 * 負責將各模型供應商返回的預覽或預設圖片網址轉換為原始未壓縮無損高解析度之直接下載網址。
 */
interface ImageUrlNormalizer {
    /**
     * 對輸入網址進行正規劃處理。
     * 若為支援的 CDN 網址，將調整參數為最高解析度原圖；否則原樣返回。
     */
    fun normalize(rawUrl: String): String
}

/**
 * Google FIFE (Frontend Image Feature Engine) CDN 原圖網址轉換器。
 * 針對 googleusercontent.com / lh3.googleusercontent.com 網址：
 * 將尾端參數轉換為 `=s0-d` (s0 = 解除尺寸限制輸出原始解析度, -d = 下載無損圖檔)。
 */
class GoogleCdnUrlNormalizer : ImageUrlNormalizer {

    companion object {
        private val GOOGLE_CDN_PARAM_REGEX = Regex("""=[^=]*$""")
    }

    override fun normalize(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("data:image/")) {
            return trimmed
        }

        val safeUrl = if (trimmed.startsWith("http://")) {
            "https://" + trimmed.removePrefix("http://")
        } else {
            trimmed
        }

        if (!safeUrl.contains("googleusercontent.com")) {
            return safeUrl
        }

        val parts = safeUrl.split("?", limit = 2)
        val base = parts[0]
        val query = if (parts.size > 1) "?" + parts[1] else ""

        val rewrittenBase = if (base.contains("=")) {
            base.replace(GOOGLE_CDN_PARAM_REGEX, "=s0-d")
        } else {
            "$base=s0-d"
        }

        return rewrittenBase + query
    }
}
