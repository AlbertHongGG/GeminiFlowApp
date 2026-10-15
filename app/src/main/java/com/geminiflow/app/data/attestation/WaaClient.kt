package com.geminiflow.app.data.attestation

import android.util.Log
import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.domain.model.common.ChallengeFetchException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray

/**
 * Google Web Attestation (WAA) API 客戶端。
 * 負責向 Google 端點獲取動態 Botguard 挑戰與虛擬機位元組碼，並提供智慧快取。
 */
class WaaClient(
    private val client: OkHttpClient,
    private val cacheTtlMillis: Long = 600_000L // 10 分鐘快取 (Google 挑戰一般有效 15 分鐘)
) {
    companion object {
        private const val TAG = "WaaClient"
        private val JSON_PROTO_MEDIA_TYPE = "application/json+protobuf".toMediaType()
    }

    data class Challenge(
        val bytecode: String,
        val scriptCode: String
    )

    private var cachedBytecode: String? = null
    private var cachedScriptCode: String? = null
    private var cachedTime: Long = 0L

    val isCacheValid: Boolean
        get() = cachedBytecode != null &&
                cachedScriptCode != null &&
                (System.currentTimeMillis() - cachedTime < cacheTtlMillis)

    suspend fun getChallenge(): Challenge = withContext(Dispatchers.IO) {
        if (isCacheValid) {
            return@withContext Challenge(
                bytecode = cachedBytecode!!,
                scriptCode = cachedScriptCode!!
            )
        }

        val fresh = fetchFreshChallenge()
        cachedBytecode = fresh.bytecode
        cachedScriptCode = fresh.scriptCode
        cachedTime = System.currentTimeMillis()
        Log.d(TAG, "成功獲取並快取 Google WAA Botguard 挑戰 (TTL: ${cacheTtlMillis / 1000}s)")
        fresh
    }

    fun invalidateCache() {
        cachedBytecode = null
        cachedScriptCode = null
        cachedTime = 0L
    }

    private fun fetchFreshChallenge(): Challenge {
        try {
            val bodyJson = JSONArray().put(GeminiConfig.GEMINI_WAA_SERVICE_KEY).toString()
            val requestBody = bodyJson.toRequestBody(JSON_PROTO_MEDIA_TYPE)

            val request = Request.Builder()
                .url(GeminiConfig.WAA_CREATE_URL)
                .addHeader("x-goog-api-key", GeminiConfig.WAA_API_KEY)
                .addHeader("Content-Type", "application/json+protobuf")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorSnippet = response.body?.string()?.take(200) ?: ""
                response.close()
                throw ChallengeFetchException("WAA Create 回傳錯誤 HTTP ${response.code}: $errorSnippet")
            }

            val responseText = response.body?.string()
                ?: throw ChallengeFetchException("WAA 回應內容為空")
            response.close()

            val jsonArray = JSONArray(responseText)
            if (jsonArray.length() == 0) {
                throw ChallengeFetchException("WAA 回應格式非預期 (空陣列)")
            }

            val challengeArray = jsonArray.optJSONArray(0)
                ?: throw ChallengeFetchException("WAA 回應格式非預期 (缺少 challenge 陣列)")

            if (challengeArray.length() < 5 || challengeArray.isNull(4)) {
                throw ChallengeFetchException("WAA 回傳無效的 Challenge 資料包 (缺少 bytecode)")
            }

            val bytecode = challengeArray.getString(4)
            val subArray2 = challengeArray.optJSONArray(2)
            val scriptPath = if (subArray2 != null && subArray2.length() > 3) {
                subArray2.optString(3, null)
            } else null

            if (scriptPath.isNullOrBlank()) {
                throw ChallengeFetchException("WAA 回應缺少腳本下載路徑")
            }

            val scriptUrl = if (scriptPath.startsWith("//")) "https:$scriptPath" else scriptPath
            val scriptReq = Request.Builder().url(scriptUrl).get().build()
            val scriptResp = client.newCall(scriptReq).execute()

            if (!scriptResp.isSuccessful) {
                val code = scriptResp.code
                scriptResp.close()
                throw ChallengeFetchException("無法自 $scriptUrl 下載 Botguard 腳本: HTTP $code")
            }

            val scriptCode = scriptResp.body?.string()
                ?: throw ChallengeFetchException("Botguard 腳本內容為空")
            scriptResp.close()

            return Challenge(bytecode = bytecode, scriptCode = scriptCode)
        } catch (e: ChallengeFetchException) {
            throw e
        } catch (e: Exception) {
            throw ChallengeFetchException("與 Google WAA 通訊時發生異常: ${e.message}", e)
        }
    }
}
