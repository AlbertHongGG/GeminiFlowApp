package com.geminiflow.app.domain.repository

import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.export.ImageExportMetadata

/**
 * 影像超高解析度原圖導出服務契約。
 * 負責透過 c8o8Fe RPC 向 Google 雲端叢集要求生成無損超高解析度母圖（2816x1536）。
 */
interface ImageExportService {
    /**
     * 執行 Gemini c8o8Fe 超高解析度原圖導出 RPC。
     * @param metadata 圖片導出中繼資料物件（內含會話三元組與生圖區塊結構）
     * @param tokens Google 帳號授權 Token (snlm0e, sid)
     * @param cookies HTTP 授權 Cookies
     * @return 成功返回超高解析度圖片直接下載網址（含 =s0），失敗返回 Result.failure
     */
    suspend fun exportHighResolutionImageUrl(
        metadata: ImageExportMetadata,
        tokens: GeminiTokens,
        cookies: Map<String, String>
    ): Result<String>
}
