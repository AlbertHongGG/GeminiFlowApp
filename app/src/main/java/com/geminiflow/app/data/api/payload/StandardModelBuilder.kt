package com.geminiflow.app.data.api.payload

import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.chat.ChatRequest
import org.json.JSONArray

class StandardModelBuilder(
    request: ChatRequest,
    tokens: GeminiTokens,
    uploads: List<Pair<String, String>> = emptyList(),
    conversationIds: List<String> = emptyList()
) : BasePayloadBuilder(request, tokens, uploads, conversationIds) {

    override fun buildInnerRequest(
        cId: String,
        rId: String,
        rcId: String,
        p79: Int,
        p80: Int
    ): JSONArray {
        val imageList = JSONArray()
        for ((uploadRef, imageName) in uploads) {
            val refArr = JSONArray().apply {
                put(uploadRef)
                put(1)
            }
            val item = JSONArray().apply {
                put(refArr)
                put(imageName)
            }
            imageList.put(item)
        }

        val part0 = JSONArray().apply {
            put(prompt)
            put(0)
            put(JSONObjectCompat.NULL)
            put(imageList)
            put(JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put(0)
        }

        val part1 = JSONArray().apply {
            put(request.language)
        }

        val part2 = JSONArray().apply {
            put(cId)
            put(rId)
            put(rcId)
            put(JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put("")
        }

        val part6 = JSONArray().apply {
            put(1)
        }

        return JSONArray().apply {
            put(part0)
            put(part1)
            put(part2)
            put(JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put(part6)
            put(0)
            put(JSONArray())
            put(JSONArray())
            put(1)
            put(0)
        }
    }
}
