package com.geminiflow.app.data.attestation

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.await
import androidx.javascriptengine.JavaScriptSandbox
import com.geminiflow.app.domain.attestation.TokenSigner
import com.geminiflow.app.domain.model.attestation.BotguardToken
import com.geminiflow.app.domain.model.common.VmExecutionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 基於 AndroidX JavaScriptSandbox (Google 原生 V8) 的 Botguard 簽章器實作。
 * 執行於獨立沙盒進程，具備環境樁類比、微任務主動排空與挑戰過期自動癒合重試能力。
 */
class AndroidV8TokenSigner(
    private val context: Context,
    private val waaClient: WaaClient
) : TokenSigner {

    companion object {
        private const val TAG = "AndroidV8TokenSigner"
    }

    private val secureRandom = SecureRandom()

    override suspend fun sign(effectivePrompt: String, conversationId: String?): BotguardToken? = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()

        if (!JavaScriptSandbox.isSupported()) {
            Log.w(TAG, "當前裝置系統環境不支援 JavaScriptSandbox，跳過 Botguard 簽章 (優雅降級)")
            return@withContext null
        }

        // 1. 生成 16 位元組隨機鹽值 (32 位 hex)
        val saltBytes = ByteArray(16).also { secureRandom.nextBytes(it) }
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }

        // 2. 計算內容綁定雜湊: qh = SHA256(effectivePrompt + saltHex)
        val toHash = (effectivePrompt + saltHex).toByteArray(Charsets.UTF_8)
        val queryHash = MessageDigest.getInstance("SHA-256").digest(toHash)
            .joinToString("") { "%02x".format(it) }

        val cid = conversationId ?: ""

        try {
            // 3. 獲取挑戰 (自快取或向 WAA 請求)
            val challenge = waaClient.getChallenge()

            // 4. 在 V8 執行並產生權杖 (附帶挑戰過期自動癒合)
            val tokenStr = try {
                executeInV8(challenge, queryHash, cid)
            } catch (e: Exception) {
                Log.w(TAG, "初始 Botguard 執行失敗 (${e.message})，正在拉取全新挑戰重試...", e)
                waaClient.invalidateCache()
                val freshChallenge = waaClient.getChallenge()
                executeInV8(freshChallenge, queryHash, cid)
            }

            val elapsedMs = System.currentTimeMillis() - t0
            Log.d(TAG, "Botguard Token 透過 Android V8 生成成功 (耗時 ${elapsedMs}ms, 長度 ${tokenStr.length})")

            BotguardToken(
                token = tokenStr,
                salt = saltHex,
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Botguard 簽章發生例外，啟用優雅降級: ${e.message}", e)
            null
        }
    }

    private suspend fun executeInV8(challenge: WaaClient.Challenge, queryHash: String, cid: String): String {
        val sandbox = JavaScriptSandbox.createConnectedInstanceAsync(context).await()
        val isolate = sandbox.createIsolate()

        try {
            // 1. 注入環境樁
            isolate.evaluateJavaScriptAsync(BotguardEnvironment.STUBS).await()

            // 2. 注入 Botguard 腳本
            isolate.evaluateJavaScriptAsync(challenge.scriptCode).await()

            // 3. 注入 Bytecode
            val quotedBytecode = JSONObject.quote(challenge.bytecode)
            isolate.evaluateJavaScriptAsync("window.__raw_bytecode = $quotedBytecode;").await()

            // 4. 實例化 Botguard VM 並排空定時器
            val initCode = """
                (function() {
                    window.__bg_ready = false;
                    window.__bg_instance = window.botguard.bg(window.__raw_bytecode, function() {
                        window.__bg_ready = true;
                    });
                    __drainTimers();
                })()
            """.trimIndent()
            isolate.evaluateJavaScriptAsync(initCode).await()

            // 5. 調用 invoke 產生權杖
            val quotedQh = JSONObject.quote(queryHash)
            val quotedCid = JSONObject.quote(cid)

            val invokeCode = """
                (function() {
                    var bgObj = window.__bg_instance;
                    if (!bgObj || typeof bgObj.invoke !== 'function') {
                        return "ERR:NoBgInstance";
                    }
                    __drainTimers();
                    var qhObj = [{ qh: $quotedQh, cid: $quotedCid }];
                    var token = null;
                    var res = bgObj.invoke(function(asyncToken) {
                        token = asyncToken;
                    }, false, qhObj);
                    __drainTimers();
                    return token || res;
                })()
            """.trimIndent()

            val rawResult = isolate.evaluateJavaScriptAsync(invokeCode).await()
            val cleaned = rawResult.trim().removeSurrounding("\"")

            if (cleaned.startsWith("ERR:") || cleaned.startsWith("FNL")) {
                throw VmExecutionException("Botguard VM 執行失敗狀態: $cleaned")
            }

            if (!cleaned.startsWith("!")) {
                throw VmExecutionException("Botguard 產出非合法權杖 (缺少 '!' 前綴): ${cleaned.take(30)}")
            }

            return cleaned
        } finally {
            try {
                isolate.close()
                sandbox.close()
            } catch (_: Exception) {
            }
        }
    }
}
