package com.geminiflow.app.data.api

import com.geminiflow.app.data.api.filter.GeminiThoughtFilter
import com.geminiflow.app.data.api.filter.ThoughtFilter
import com.geminiflow.app.domain.model.export.ImageExportMetadata
import org.json.JSONArray

/**
 * Gemini 串流回應解析引擎。
 * 負責從 Batchexecute 原始回應中安全提取純淨正文、過濾思考推理歷程、偵測會話 ID 與提取生圖導出中繼資料。
 */
class GeminiStreamParser(
    private val thoughtFilter: ThoughtFilter = GeminiThoughtFilter(),
    private val imageBlockExtractor: GeminiImageBlockExtractor = GeminiImageBlockExtractor()
) {
    var lastContent: String = ""
        private set

    data class TextDeltaResult(
        val delta: String?,
        val conversationId: String? = null,
        val continuationToken: String? = null
    )

    fun extractConversationContext(responsePart: JSONArray): Pair<String?, String?> {
        var convId: String? = null
        var continuationToken: String? = null

        try {
            if (responsePart.length() > 1) {
                val part1 = responsePart.optJSONArray(1)
                if (part1 != null && part1.length() > 0) {
                    val first = part1.optString(0, "")
                    if (first.startsWith("c_")) {
                        convId = first
                        for (i in 0 until part1.length()) {
                            val item = part1.optString(i, "")
                            if (item.length >= 28 && item.startsWith("Aw")) {
                                continuationToken = item
                                break
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        if (convId != null && continuationToken == null) {
            continuationToken = findContinuationTokenRecursive(responsePart)
        }

        return Pair(convId, continuationToken)
    }

    private fun findContinuationTokenRecursive(element: Any?): String? {
        when (element) {
            is String -> {
                if (element.length >= 28 && element.startsWith("Aw")) {
                    return element
                }
            }
            is JSONArray -> {
                for (i in 0 until element.length()) {
                    val found = findContinuationTokenRecursive(element.opt(i))
                    if (found != null) return found
                }
            }
            is org.json.JSONObject -> {
                val keys = element.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val found = findContinuationTokenRecursive(element.opt(key))
                    if (found != null) return found
                }
            }
        }
        return null
    }

    fun extractTextDelta(rawLine: String): TextDeltaResult {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return TextDeltaResult(null, null, null)

        val lineArray = try {
            JSONArray(trimmed)
        } catch (_: Exception) {
            return TextDeltaResult(null, null, null)
        }

        if (lineArray.length() == 0) return TextDeltaResult(null, null, null)

        val firstItem = lineArray.optJSONArray(0) ?: return TextDeltaResult(null, null, null)
        if (firstItem.length() < 3) return TextDeltaResult(null, null, null)

        val innerJsonStr = firstItem.optString(2, null) ?: return TextDeltaResult(null, null, null)
        val responsePart = try {
            JSONArray(innerJsonStr)
        } catch (_: Exception) {
            return TextDeltaResult(null, null, null)
        }

        // 提取會話 Session ID (c_...) 與續接 Token (Aw...)
        val (convId, contToken) = extractConversationContext(responsePart)

        // 提取累積正文內容並嚴格過濾思考歷程與內部圖片網址
        val rawContent = extractContent(responsePart) ?: return TextDeltaResult(null, convId, contToken)
        val content = sanitizeOutputText(rawContent)

        if (content.isEmpty()) {
            return TextDeltaResult(null, convId, contToken)
        }

        val delta = if (lastContent.isNotEmpty() && content.startsWith(lastContent)) {
            content.substring(lastContent.length)
        } else {
            content
        }
        lastContent = content

        val finalDelta = if (delta.isBlank()) null else delta
        return TextDeltaResult(finalDelta, convId, contToken)
    }

    /**
     * 純淨化輸出文字：委派給 ThoughtFilter 徹底過濾內部思考歷程與內部圖片佔位網址
     */
    fun sanitizeOutputText(rawText: String): String {
        return thoughtFilter.filter(rawText)
    }

    private fun extractContent(responsePart: JSONArray): String? {
        try {
            if (responsePart.length() >= 5) {
                val part4 = responsePart.optJSONArray(4) ?: return null
                val candidate = findCandidate(part4) ?: return null
                if (candidate.length() > 1) {
                    val contentNode = candidate.opt(1)
                    if (contentNode is JSONArray && contentNode.length() > 0) {
                        val text = contentNode.optString(0, "")
                        if (text.isNotEmpty()) return text
                    } else if (contentNode is String && contentNode.isNotEmpty()) {
                        return contentNode
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun findCandidate(part4: JSONArray): JSONArray? {
        if (part4.length() == 0) return null

        // 1. 直接候選：part4[0] 即為候選陣列 [id, [content], ...]
        val direct = part4.optJSONArray(0)
        if (direct != null && direct.length() > 1) {
            val second = direct.opt(1)
            if (second is JSONArray || second is String) {
                return direct
            }
        }

        // 2. 嵌套候選：part4[4][0] 為候選陣列 [ [id, [content], ...] ]
        if (part4.length() >= 5) {
            val nested = part4.optJSONArray(4)
            if (nested != null && nested.length() > 0) {
                val nestedFirst = nested.optJSONArray(0)
                if (nestedFirst != null && nestedFirst.length() > 1) {
                    val second = nestedFirst.opt(1)
                    if (second is JSONArray || second is String) {
                        return nestedFirst
                    }
                }
            }
        }

        return null
    }



    /**
     * 從串流回應中直接提取結構化圖片導出中繼資料（用於 c8o8Fe 超高解析原圖導出）。
     */
    fun extractImageExportMetadata(rawLine: String): ImageExportMetadata? {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return null

        val lineArray = try {
            JSONArray(trimmed)
        } catch (_: Exception) {
            return null
        }

        if (lineArray.length() == 0) return null
        val firstItem = lineArray.optJSONArray(0) ?: return null
        if (firstItem.length() < 3) return null

        val innerJsonStr = firstItem.optString(2, null) ?: return null
        val responsePart = try {
            JSONArray(innerJsonStr)
        } catch (_: Exception) {
            return null
        }

        return imageBlockExtractor.extract(responsePart)
    }

    fun reset() {
        lastContent = ""
    }
}
