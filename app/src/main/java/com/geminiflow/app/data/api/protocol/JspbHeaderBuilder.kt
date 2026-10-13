package com.geminiflow.app.data.api.protocol

import com.geminiflow.app.domain.model.chat.ModelSpec
import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

class JspbHeaderBuilder(
    private val modelSpec: ModelSpec,
    private val sessionUuid: String,
    private val reqUuid: String,
    private val serverBl: String
) {
    companion object {
        val CAPABILITIES = listOf(4, 5, 6, 8, 16, 19, 20, 4, 5, 6, 8, 16, 19, 20)
    }

    fun build525001261(): String {
        val arr = ArrayList<Any?>(20).apply {
            for (i in 0 until 20) add(null)
        }
        arr[0] = 1
        arr[4] = modelSpec.modelHash
        arr[7] = 0
        arr[8] = JSONArray(CAPABILITIES)
        arr[11] = 2
        arr[14] = modelSpec.modelId
        arr[15] = modelSpec.thinkingLevel
        arr[16] = sessionUuid
        arr[17] = JSONArray().put(serverBl)

        // Emulate high-resolution client clock pair [[deltaSec, deltaNano], [epochSec, epochNano]]
        val nowMillis = System.currentTimeMillis()
        val sec = nowMillis / 1000
        val nsec = ((nowMillis % 1000) * 1_000_000).toInt()
        val random = Random()
        val deltaPair = JSONArray().put(random.nextInt(5) + 1).put(random.nextInt(900_000_000) + 100_000_000)
        val nowPair = JSONArray().put(sec).put(nsec)
        arr[19] = JSONArray().put(deltaPair).put(nowPair)

        val jsonArray = JSONArray()
        for (item in arr) {
            jsonArray.put(item ?: JSONObject.NULL)
        }
        return jsonArray.toString()
    }

    fun build525005358(): String {
        return JSONArray().apply {
            put(reqUuid)
            put(1)
        }.toString()
    }

    fun buildAllHeaders(): Map<String, String> {
        return mapOf(
            "x-goog-ext-525001261-jspb" to build525001261(),
            "x-goog-ext-525005358-jspb" to build525005358(),
            "x-goog-ext-73010989-jspb" to "[0]",
            "x-goog-ext-73010990-jspb" to "[0,0,0]"
        )
    }
}
