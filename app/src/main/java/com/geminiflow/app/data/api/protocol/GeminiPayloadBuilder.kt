package com.geminiflow.app.data.api.protocol

import com.geminiflow.app.domain.model.chat.ModelSpec
import org.json.JSONArray
import org.json.JSONObject

class GeminiPayloadBuilder(
    private val prompt: String,
    private val language: String,
    private val modelSpec: ModelSpec,
    private val reqUuid: String,
    private val uploads: List<Pair<String, String>> = emptyList(),
    private val conversationId: String? = null,
    private val continuationToken: String? = null,
    private val turnIndex: Int = 0,
    private val botguardToken: String? = null,
    private val botguardSalt: String? = null
) {
    companion object {
        const val ARRAY_LENGTH = 99

        const val IDX_PROMPT_BLOCK = 0
        const val IDX_LANGUAGE = 1
        const val IDX_BOTGUARD_TOKEN = 3
        const val IDX_BOTGUARD_SALT = 4
        const val IDX_TURN_INDEX = 17
        const val IDX_CAPABILITIES = 30
        const val IDX_REQ_UUID = 59
        const val IDX_CONVERSATION_CONTEXT = 71
        const val IDX_MODEL_ID = 79
        const val IDX_THINKING_LEVEL = 80
        const val IDX_NEW_SESSION_FLAG = 96
    }

    private fun buildConversationContext(): Any {
        if (turnIndex == 0 || conversationId.isNullOrBlank()) {
            return JSONObject.NULL
        }
        return JSONArray().apply {
            put(conversationId)
            for (i in 1..6) {
                put(JSONObject.NULL)
            }
            put(continuationToken ?: JSONObject.NULL)
        }
    }

    fun buildInnerArray(): JSONArray {
        val arr = ArrayList<Any?>(ARRAY_LENGTH).apply {
            for (i in 0 until ARRAY_LENGTH) add(null)
        }

        // 0: Prompt & Images
        val imageList = if (uploads.isNotEmpty()) {
            JSONArray().apply {
                for ((ref, name) in uploads) {
                    put(JSONArray().put(JSONArray().put(ref).put(1)).put(name))
                }
            }
        } else JSONObject.NULL

        arr[IDX_PROMPT_BLOCK] = JSONArray().apply {
            put(prompt)
            put(0)
            put(JSONObject.NULL)
            put(imageList)
            put(JSONObject.NULL)
            put(JSONObject.NULL)
            put(0)
        }

        // 1: Language
        arr[IDX_LANGUAGE] = JSONArray().put(language)

        // 2: Legacy conversation IDs abandoned - remains null/JSONObject.NULL

        // 3 & 4: Botguard attestation token and random salt
        if (botguardToken != null) {
            arr[IDX_BOTGUARD_TOKEN] = botguardToken
        }
        if (botguardSalt != null) {
            arr[IDX_BOTGUARD_SALT] = botguardSalt
        }

        // Static flags
        arr[6] = JSONArray().put(0)
        arr[7] = 1
        arr[10] = 1
        arr[11] = 0

        // 17: Turn Index [[0]], [[1]], etc.
        arr[IDX_TURN_INDEX] = JSONArray().put(JSONArray().put(turnIndex))
        arr[18] = 0
        arr[27] = 1

        // 30: Capabilities
        arr[IDX_CAPABILITIES] = JSONArray().put(4).put(16).put(19)
        arr[41] = JSONArray().put(1)
        arr[49] = 0
        arr[53] = 0

        // 59: Request UUID matching Header x-goog-ext-525005358-jspb
        arr[IDX_REQ_UUID] = reqUuid
        arr[61] = JSONArray()
        arr[67] = 0
        arr[68] = 1

        // 71: Conversation continuation context [conv_id, null*6, continuation_token]
        arr[IDX_CONVERSATION_CONTEXT] = buildConversationContext()

        // 79 & 80: Model routing ID and Thinking level
        arr[IDX_MODEL_ID] = modelSpec.modelId
        arr[IDX_THINKING_LEVEL] = modelSpec.thinkingLevel

        // Terminal flags
        arr[91] = 1
        // 96: 0 for new session (turn 0), 1 for continuing session (turn > 0)
        arr[IDX_NEW_SESSION_FLAG] = if (turnIndex == 0) 0 else 1
        arr[98] = 1

        val jsonArray = JSONArray()
        for (item in arr) {
            jsonArray.put(item ?: JSONObject.NULL)
        }
        return jsonArray
    }

    fun buildPostData(atToken: String): Map<String, String> {
        val inner = buildInnerArray()
        val outer = JSONArray().apply {
            put(JSONObject.NULL)
            put(inner.toString())
        }
        return mapOf(
            "at" to atToken,
            "f.req" to outer.toString()
        )
    }
}
