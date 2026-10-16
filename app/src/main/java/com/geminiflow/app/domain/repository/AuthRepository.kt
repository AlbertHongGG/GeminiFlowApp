package com.geminiflow.app.domain.repository

import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.auth.SessionState
import kotlinx.coroutines.flow.StateFlow

/**
 * 領域鑑權門面介面。
 * 統一管理 Google Session 狀態與對話權杖獲取。
 */
interface AuthRepository {
    /**
     * 強型別工作階段狀態（單一事實來源）。
     */
    val sessionState: StateFlow<SessionState>

    /**
     * 是否處於已登入授權狀態（便利屬性，等價於 sessionState is Authenticated）。
     */
    val isAuthenticated: StateFlow<Boolean>

    /**
     * 獲取有效的協定對話權杖。
     */
    suspend fun getValidTokens(): GeminiTokens

    /**
     * 強制重新整理權杖（401/403 原地自愈時使用）。
     */
    suspend fun forceRefreshToken(): GeminiTokens

    /**
     * 確保權杖有效，支援強制重整。
     */
    suspend fun ensureValidTokens(forceRefresh: Boolean = false): GeminiTokens

    /**
     * 獲取本機存儲之 Google Session Cookie 映射表。
     */
    suspend fun getGoogleCookies(): Map<String, String>

    /**
     * 儲存從 WebView 獲取的 Cookie 字串。
     */
    suspend fun saveCookies(cookiesHeader: String)

    /**
     * 清空本機登入憑證並標記為未登入。
     */
    suspend fun clearAuth()
}
