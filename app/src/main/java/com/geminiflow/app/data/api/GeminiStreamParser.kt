package com.geminiflow.app.data.api

import com.geminiflow.app.data.api.filter.GeminiThoughtFilter
import com.geminiflow.app.data.api.filter.ThoughtFilter
import com.geminiflow.app.data.api.streaming.StreamFrame
import com.geminiflow.app.domain.model.export.ImageExportMetadata
import org.json.JSONArray

/**
 * Gemini 串流回應解析引擎。
 * 負責從 Batchexecute 原始回應中安全提取純淨正文、過濾思考推理歷程、偵測會話 ID 與提取生圖導出中繼資料。
 * 支援高效率的單趟解析（Single-Pass Parsing），將位元流轉換為強型別 StreamFrame。
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

    /**
     * 單趟全能訊框解析器（核心重構方法）：
     * 一行資料只做一次 JSON 反序列化，同時判定結束訊框、生圖資料、文字增量與會話狀態。
     */
    fun parseLine(rawLine: String): StreamFrame {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return StreamFrame.Ignored

        // 快速結束訊框前置特徵檢測
        if (isEndOfStreamFastCheck(trimmed)) {
            return StreamFrame.EndOfStream
        }

        val lineArray = try {
            JSONArray(trimmed)
        } catch (_: Exception) {
            return StreamFrame.Ignored
        }

        if (lineArray.length() == 0) return StreamFrame.Ignored

        // 檢查陣列中是否包含結束訊框 (e 或 di)
        for (i in 0 until lineArray.length()) {
            val item = lineArray.optJSONArray(i)
            if (item != null && item.length() > 0) {
                val first = item.optString(0, "")
                if (first == "e" || first == "di") {
                    return StreamFrame.EndOfStream
                }
            }
        }

        val firstItem = lineArray.optJSONArray(0) ?: return StreamFrame.Ignored
        if (firstItem.length() < 3) return StreamFrame.Ignored

        val innerJsonStr = firstItem.optString(2, null) ?: return StreamFrame.Ignored
        val responsePart = try {
            JSONArray(innerJsonStr)
        } catch (_: Exception) {
            return StreamFrame.Ignored
        }

        // 1. 提取會話 Session ID (c_...) 與續接 Token (Aw...)
        val (convId, contToken) = extractConversationContext(responsePart)

        // 2. 提取圖片導出中繼資料 (c8o8Fe)
        val exportMeta = imageBlockExtractor.extract(responsePart)
        if (exportMeta != null) {
            return StreamFrame.ImageExport(
                metadata = exportMeta,
                conversationId = convId,
                continuationToken = contToken
            )
        }

        // 3. 提取累積正文內容並嚴格過濾思考歷程
        val rawContent = extractContent(responsePart)
        if (rawContent != null) {
            val content = sanitizeOutputText(rawContent)
            if (content.isNotEmpty()) {
                val delta = if (lastContent.isNotEmpty() && content.startsWith(lastContent)) {
                    content.substring(lastContent.length)
                } else {
                    content
                }
                lastContent = content

                // 核心修復：使用 isNotEmpty() 確保所有換行符與空格縮排 100% 傳遞，不被誤殺
                if (delta.isNotEmpty()) {
                    return StreamFrame.Text(
                        delta = delta,
                        conversationId = convId,
                        continuationToken = contToken
                    )
                }
            }
        }

        // 若無文字增量亦無圖片，但包含會話上下文，則回傳 SessionContext
        if (convId != null) {
            return StreamFrame.SessionContext(
                conversationId = convId,
                continuationToken = contToken
            )
        }

        return StreamFrame.Ignored
    }

    private fun isEndOfStreamFastCheck(stripped: String): Boolean {
        return stripped.startsWith("""[["e",""") ||
               stripped.startsWith("""[["di",""") ||
               stripped.contains(""""di",null""") ||
               stripped.contains("""["di",null""")
    }

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
            if (responsePart.length() > 2) {
                val part2 = responsePart.optJSONObject(2)
                if (part2 != null) {
                    val tok = part2.optString("26", "")
                    if (tok.length >= 28 && tok.startsWith("Aw")) {
                        continuationToken = tok
                    }
                }
            }
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

        val (convId, contToken) = extractConversationContext(responsePart)
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

        // 核心修復：改為 isEmpty() 避免 isBlank() 吃掉換行與空格
        val finalDelta = if (delta.isEmpty()) null else delta
        return TextDeltaResult(finalDelta, convId, contToken)
    }

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

        val direct = part4.optJSONArray(0)
        if (direct != null && direct.length() > 1) {
            val second = direct.opt(1)
            if (second is JSONArray || second is String) {
                return direct
            }
        }

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

    fun isEndOfStream(rawLine: String): Boolean {
        val stripped = rawLine.trim()
        if (stripped.isEmpty()) return false

        try {
            val lineArray = JSONArray(stripped)
            for (i in 0 until lineArray.length()) {
                val item = lineArray.optJSONArray(i)
                if (item != null && item.length() > 0) {
                    val first = item.optString(0, "")
                    if (first == "e" || first == "di") {
                        return true
                    }
                }
            }
        } catch (_: Exception) {
        }

        return isEndOfStreamFastCheck(stripped)
    }

    fun reset() {
        lastContent = ""
    }
}
