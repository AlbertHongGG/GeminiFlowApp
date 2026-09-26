package com.geminiflow.app.data.api.payload

import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.chat.ChatRequest
import org.json.JSONArray
import java.util.UUID
import kotlin.random.Random

abstract class BasePayloadBuilder(
    protected val request: ChatRequest,
    protected val tokens: GeminiTokens,
    protected val uploads: List<Pair<String, String>> = emptyList(),
    protected val conversationIds: List<String> = emptyList()
) {
    val reqUuid: String = UUID.randomUUID().toString().uppercase()
    val extUuid: String = UUID.randomUUID().toString().uppercase()

    val prompt: String = if (!request.systemPrompt.isNullOrBlank()) {
        "System:\n${request.systemPrompt}\n\nUser:\n${request.prompt}"
    } else {
        request.prompt
    }

    fun buildParams(blParam: String = GeminiConfig.REQUEST_BL_PARAM): Map<String, String> {
        return mapOf(
            "bl" to blParam,
            "hl" to request.language,
            "_reqid" to Random.nextInt(1111, 9999).toString(),
            "rt" to "c",
            "f.sid" to (tokens.sid ?: "")
        )
    }

    fun buildPayload(modelHeadersMap: Map<String, Map<String, String>> = GeminiConfig.MODEL_HEADERS): Map<String, String> {
        val baseHeaders = modelHeadersMap[request.model]
        var p79 = 1
        var p80 = 1

        val jspbHeader = baseHeaders?.get("x-goog-ext-525001261-jspb")
        if (jspbHeader != null) {
            try {
                val arr = JSONArray(jspbHeader)
                if (arr.length() >= 16) {
                    p79 = arr.optInt(14, 1)
                    p80 = arr.optInt(15, 1)
                }
            } catch (_: Exception) {
            }
        }

        var cId = ""
        var rId = ""
        var rcId = ""
        if (conversationIds.size >= 2) {
            cId = conversationIds[0]
            rId = conversationIds[1]
            if (conversationIds.size >= 3) {
                rcId = conversationIds[2]
            }
        }

        val inner = buildInnerRequest(cId, rId, rcId, p79, p80)

        val outer = JSONArray().apply {
            put(JSONObjectCompat.NULL)
            put(inner.toString())
        }

        return mapOf(
            "at" to tokens.snlm0e,
            "f.req" to outer.toString()
        )
    }

    fun buildHeaders(baseModelHeaders: Map<String, Map<String, String>> = GeminiConfig.MODEL_HEADERS): Map<String, String> {
        val baseHeaders = baseModelHeaders[request.model] ?: emptyMap()
        val headers = HashMap(baseHeaders)

        val jspb = headers["x-goog-ext-525001261-jspb"]
        if (jspb != null) {
            headers["x-goog-ext-525001261-jspb"] = jspb.replace("{ext_uuid}", extUuid)
        }

        return enrichHeaders(headers)
    }

    abstract fun buildInnerRequest(cId: String, rId: String, rcId: String, p79: Int, p80: Int): JSONArray

    open fun enrichHeaders(headers: MutableMap<String, String>): Map<String, String> {
        return headers
    }
}

object JSONObjectCompat {
    val NULL: Any = org.json.JSONObject.NULL
}
