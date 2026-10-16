package com.geminiflow.app.domain.auth

import com.geminiflow.app.domain.model.auth.GeminiTokens

/**
 * 動態授權權杖生命週期管理器介面。
 * 負責維護短期協定權杖快取，並透過單飛併發控制保證多協程安全調度。
 */
interface TokenManager {
    /**
     * 獲取當前有效的 Gemini 權杖。快取命中時直接回傳；無快取時發起單飛換證。
     */
    suspend fun getValidTokens(): GeminiTokens

    /**
     * 強制重新整理並向服務端獲取全新權杖（通常在 401/403 時觸發）。
     */
    suspend fun forceRefreshToken(): GeminiTokens

    /**
     * 主動清除記憶體中的權杖快取。
     */
    fun invalidateCache()
}
