package com.geminiflow.app.data.auth.token

import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.common.TokenExtractionException

/**
 * 遠端 Gemini 首頁權杖抽取器介面。
 * 專責從 Gemini SPA 應用取得動態協定權杖 (SNlM0e, FdrFJe, cfb2h)。
 */
interface TokenExtractor {
    /**
     * 攜帶給定 Cookie 向 Gemini 服務端抽取即時授權權杖。
     * @throws TokenExtractionException 當網路連線異常或無法從 HTML 解析權杖時拋出
     */
    @Throws(TokenExtractionException::class)
    suspend fun extractTokens(cookies: Map<String, String>): GeminiTokens
}
