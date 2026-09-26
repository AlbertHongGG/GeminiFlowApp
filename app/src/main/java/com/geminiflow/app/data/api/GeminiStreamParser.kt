package com.geminiflow.app.data.api

import com.geminiflow.app.data.api.filter.GeminiThoughtFilter
import com.geminiflow.app.data.api.filter.ThoughtFilter
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Gemini 串流回應解析引擎。
 * 負責從 Batchexecute 原始回應中安全提取純淨正文、過濾思考推理歷程、偵測會話 ID 與提取生圖 URL。
 */
class GeminiStreamParser(
    private val thoughtFilter: ThoughtFilter = GeminiThoughtFilter()
) {
    var lastContent: String = ""
        private set

    private val urlPattern = Pattern.compile(
        """(https?://(?:googleusercontent\.com|gstatic\.com|content-push\.googleapis\.com|lh3\.googleusercontent\.com)[^\s"'\\<]+)"""
    )
    private val controlCharsPattern = Pattern.compile("""[\x00-\x1F\x7F\u200B\u200C\u200D\uFEFF]""")

    data class TextDeltaResult(
        val delta: String?,
        val sessionIds: List<String>?
    )

    fun extractTextDelta(rawLine: String): TextDeltaResult {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return TextDeltaResult(null, null)

        val lineArray = try {
            JSONArray(trimmed)
        } catch (_: Exception) {
            return TextDeltaResult(null, null)
        }

        if (lineArray.length() == 0) return TextDeltaResult(null, null)

        val firstItem = lineArray.optJSONArray(0) ?: return TextDeltaResult(null, null)
        if (firstItem.length() < 3) return TextDeltaResult(null, null)

        val innerJsonStr = firstItem.optString(2, null) ?: return TextDeltaResult(null, null)
        val responsePart = try {
            JSONArray(innerJsonStr)
        } catch (_: Exception) {
            return TextDeltaResult(null, null)
        }

        // 提取會話 Session IDs (c_...)
        var sessionIds: List<String>? = null
        try {
            if (responsePart.length() > 1) {
                val idsArray = responsePart.optJSONArray(1)
                if (idsArray != null && idsArray.length() > 0) {
                    val firstId = idsArray.optString(0, "")
                    if (firstId.startsWith("c_")) {
                        val list = mutableListOf<String>()
                        for (i in 0 until idsArray.length()) {
                            list.add(idsArray.optString(i))
                        }
                        sessionIds = list
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 提取累積正文內容並嚴格過濾思考歷程與內部圖片網址
        val rawContent = extractContent(responsePart) ?: return TextDeltaResult(null, sessionIds)
        val content = sanitizeOutputText(rawContent)

        if (content.isEmpty()) {
            return TextDeltaResult(null, sessionIds)
        }

        val delta = if (lastContent.isNotEmpty() && content.startsWith(lastContent)) {
            content.substring(lastContent.length)
        } else {
            content
        }
        lastContent = content

        val finalDelta = if (delta.isBlank()) null else delta
        return TextDeltaResult(finalDelta, sessionIds)
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
                val part4 = responsePart.optJSONArray(4)
                if (part4 != null && part4.length() > 0) {
                    val part4_0 = part4.optJSONArray(0)
                    if (part4_0 != null && part4_0.length() > 1) {
                        val part4_0_1 = part4_0.opt(1)
                        if (part4_0_1 is JSONArray && part4_0_1.length() > 0) {
                            val candidate = part4_0_1.optString(0, "")
                            if (candidate.isNotEmpty()) return candidate
                        } else if (part4_0_1 is String && part4_0_1.isNotEmpty()) {
                            return part4_0_1
                        }
                    }

                    // 降級搜索：在候選串中尋找非 metadata 的文字
                    val allStrings = mutableListOf<String>()
                    walkStrings(part4, allStrings)
                    if (allStrings.isNotEmpty()) {
                        return allStrings.maxByOrNull { it.length }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun walkStrings(element: Any?, results: MutableList<String>) {
        when (element) {
            is String -> {
                if (element.isNotEmpty() && !element.startsWith("rc_")) {
                    results.add(element)
                }
            }
            is JSONArray -> {
                for (i in 0 until element.length()) {
                    walkStrings(element.opt(i), results)
                }
            }
            is JSONObject -> {
                val keys = element.keys()
                while (keys.hasNext()) {
                    walkStrings(element.opt(keys.next()), results)
                }
            }
        }
    }

    fun extractImageCandidates(rawLine: String): List<String> {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lineArray = try {
            JSONArray(trimmed)
        } catch (_: Exception) {
            return emptyList()
        }

        if (lineArray.length() == 0) return emptyList()
        val firstItem = lineArray.optJSONArray(0) ?: return emptyList()
        if (firstItem.length() < 3) return emptyList()

        val innerJsonStr = firstItem.optString(2, null) ?: return emptyList()
        val responsePart = try {
            JSONArray(innerJsonStr)
        } catch (_: Exception) {
            return emptyList()
        }

        val allStrings = mutableListOf<String>()
        walkStrings(responsePart, allStrings)

        val foundUrls = linkedSetOf<String>()
        for (text in allStrings) {
            if (text.startsWith("data:image/")) {
                foundUrls.add(text)
                continue
            }
            val matcher = urlPattern.matcher(text)
            while (matcher.find()) {
                val found = matcher.group(1)
                if (!found.isNullOrEmpty()) {
                    foundUrls.add(found)
                }
            }
        }
        return foundUrls.toList()
    }

    fun classifyImageUrl(url: String): String? {
        val norm = controlCharsPattern.matcher(url.trim()).replaceAll("")
        if (norm.isEmpty()) return null

        if (norm.contains("googleusercontent.com/image_generation_content/") ||
            (norm.contains("lh3.googleusercontent.com/gg/") && !norm.contains("lh3.googleusercontent.com/gg-dl/"))
        ) {
            return "placeholder"
        }
        if (norm.startsWith("data:image/") || norm.contains("lh3.googleusercontent.com/gg-dl/")) {
            return "output"
        }
        return null
    }

    fun reset() {
        lastContent = ""
    }
}
