package com.geminiflow.app.data.auth.keepalive

import android.util.Log
import com.geminiflow.app.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 長效 Session 自動保活管理器。
 * 在服務運行期間定期向 Gemini 首頁觸發輕量探針，
 * 更新與校驗授權權杖並防止長期無調用導致的 Session 凍結超時。
 */
class SessionKeepAliveManager(
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val intervalMillis: Long = TimeUnit.HOURS.toMillis(2)
) {
    companion object {
        private const val TAG = "SessionKeepAliveManager"
    }

    private var keepAliveJob: Job? = null

    fun start() {
        if (keepAliveJob?.isActive == true) return

        keepAliveJob = scope.launch {
            Log.i(TAG, "SessionKeepAliveManager started with interval ${intervalMillis}ms")
            while (isActive) {
                delay(intervalMillis)
                if (authRepository.isAuthenticated.value) {
                    try {
                        Log.d(TAG, "Triggering periodic session keep-alive ping...")
                        authRepository.forceRefreshToken()
                    } catch (e: Exception) {
                        Log.w(TAG, "Session keep-alive ping encountered non-fatal error: ${e.message}")
                    }
                }
            }
        }
    }

    fun stop() {
        keepAliveJob?.cancel()
        keepAliveJob = null
        Log.i(TAG, "SessionKeepAliveManager stopped")
    }
}
