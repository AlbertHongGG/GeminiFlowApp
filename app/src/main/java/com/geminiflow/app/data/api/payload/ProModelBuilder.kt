package com.geminiflow.app.data.api.payload

import com.geminiflow.app.domain.model.ChatRequest
import com.geminiflow.app.domain.model.GeminiTokens
import org.json.JSONArray

class ProModelBuilder(
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

        val arr = ArrayList<Any?>(97)
        for (i in 0 until 97) {
            arr.add(JSONObjectCompat.NULL)
        }

        arr[0] = part0
        arr[1] = part1
        arr[2] = part2
        arr[6] = JSONArray().apply { put(0) }
        arr[7] = 1
        arr[10] = 1
        arr[11] = 0
        arr[17] = JSONArray().apply {
            put(JSONArray().apply { put(if (p80 == 2) 1 else 0) })
        }
        arr[18] = 0
        arr[27] = 1
        arr[30] = JSONArray().apply { put(4) }
        arr[41] = JSONArray().apply { put(1) }
        arr[53] = 0
        arr[59] = reqUuid
        arr[61] = JSONArray().apply { put(1) }
        arr[67] = 0
        arr[68] = 1
        arr[79] = p79
        arr[80] = p80
        arr[91] = 0
        arr[96] = 0

        val result = JSONArray()
        for (item in arr) {
            result.put(item ?: JSONObjectCompat.NULL)
        }
        return result
    }

    override fun enrichHeaders(headers: MutableMap<String, String>): Map<String, String> {
        val extra = JSONArray().apply {
            put(reqUuid)
            put(1)
        }
        headers["x-goog-ext-525005358-jspb"] = extra.toString()
        return headers
    }
}
