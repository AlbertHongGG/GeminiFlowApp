package com.geminiflow.app.data.api

import com.geminiflow.app.domain.model.export.ImageExportMetadata
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini SSE 串流結構化生圖節點抽取器。
 * 負責從解析後的 responsePart JSON 語法樹中精準提取生圖中繼區塊（img_block）
 * 與會話三元組（c_..., r_..., rc_...），建立不可變的 ImageExportMetadata 數值物件。
 */
class GeminiImageBlockExtractor {

    fun extract(responsePart: JSONArray): ImageExportMetadata? {
        val imgBlock = findImageBlock(responsePart) ?: return null
        val imageId = imgBlock.optString(8, "")
        if (!imageId.startsWith("im_")) return null

        val previewUrl = extractPreviewUrl(imgBlock) ?: return null
        val conversationId = extractConversationId(responsePart) ?: return null
        val responseId = extractResponseId(responsePart) ?: return null
        val choiceId = extractChoiceId(responsePart) ?: return null

        return try {
            ImageExportMetadata(
                conversationId = conversationId,
                responseId = responseId,
                choiceId = choiceId,
                imageId = imageId,
                imageBlockJson = imgBlock.toString(),
                previewUrl = previewUrl
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun findImageBlock(root: JSONArray): JSONArray? {
        // 優先快速路徑：inner[4][0][12][7][0][0]
        try {
            val part4 = root.optJSONArray(4)
            val part4_0 = part4?.optJSONArray(0)
            val part4_0_12 = part4_0?.optJSONArray(12)
            val part4_0_12_7 = part4_0_12?.optJSONArray(7)
            val part4_0_12_7_0 = part4_0_12_7?.optJSONArray(0)
            val candidate = part4_0_12_7_0?.optJSONArray(0)
            if (candidate != null && candidate.length() >= 9 && candidate.optString(8, "").startsWith("im_")) {
                return candidate
            }
        } catch (_: Exception) {
        }

        // 語法樹搜尋：遍歷尋找具備 [8].startsWith("im_") 特徵之節點
        val holder = arrayOfNulls<JSONArray>(1)
        searchImageBlockNode(root, holder)
        return holder[0]
    }

    private fun searchImageBlockNode(element: Any?, holder: Array<JSONArray?>) {
        if (holder[0] != null) return
        when (element) {
            is JSONArray -> {
                if (element.length() >= 9) {
                    val idStr = element.optString(8, "")
                    if (idStr.startsWith("im_")) {
                        holder[0] = element
                        return
                    }
                }
                for (i in 0 until element.length()) {
                    searchImageBlockNode(element.opt(i), holder)
                    if (holder[0] != null) return
                }
            }
            is JSONObject -> {
                val keys = element.keys()
                while (keys.hasNext()) {
                    searchImageBlockNode(element.opt(keys.next()), holder)
                    if (holder[0] != null) return
                }
            }
        }
    }

    private fun extractPreviewUrl(imgBlock: JSONArray): String? {
        val urls = mutableListOf<String>()
        collectStrings(imgBlock, urls)
        return urls.firstOrNull { it.contains("lh3.googleusercontent.com/gg-dl/") }
            ?: urls.firstOrNull { it.contains("lh3.googleusercontent.com/gg/") }
            ?: urls.firstOrNull { it.contains("googleusercontent.com/") }
    }

    private fun extractConversationId(root: JSONArray): String? {
        val part1 = root.optJSONArray(1)
        if (part1 != null && part1.length() > 0) {
            val c = part1.optString(0, "")
            if (c.startsWith("c_")) return c
        }
        val all = mutableListOf<String>()
        collectStrings(root, all)
        return all.firstOrNull { it.startsWith("c_") }
    }

    private fun extractResponseId(root: JSONArray): String? {
        val part1 = root.optJSONArray(1)
        if (part1 != null && part1.length() > 1) {
            val r = part1.optString(1, "")
            if (r.startsWith("r_")) return r
        }
        val all = mutableListOf<String>()
        collectStrings(root, all)
        return all.firstOrNull { it.startsWith("r_") }
    }

    private fun extractChoiceId(root: JSONArray): String? {
        val part4 = root.optJSONArray(4)
        val part4_0 = part4?.optJSONArray(0)
        val candidate = part4_0?.optString(0, "")
        if (candidate != null && candidate.startsWith("rc_")) {
            return candidate
        }
        val all = mutableListOf<String>()
        collectStrings(root, all)
        return all.firstOrNull { it.startsWith("rc_") }
    }

    private fun collectStrings(element: Any?, results: MutableList<String>) {
        when (element) {
            is String -> {
                if (element.isNotEmpty()) results.add(element)
            }
            is JSONArray -> {
                for (i in 0 until element.length()) {
                    collectStrings(element.opt(i), results)
                }
            }
            is JSONObject -> {
                val keys = element.keys()
                while (keys.hasNext()) {
                    collectStrings(element.opt(keys.next()), results)
                }
            }
        }
    }
}
