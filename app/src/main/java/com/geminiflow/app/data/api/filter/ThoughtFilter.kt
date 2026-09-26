package com.geminiflow.app.data.api.filter

/**
 * 思考歷程與內部元資料過濾器介面。
 * 負責從大模型串流輸出中過濾內部思考、思維鏈標題區塊、以及佔位網址，
 * 確保終端使用者僅接收純淨的最終回答或結果。
 */
interface ThoughtFilter {
    /**
     * 純淨化文字輸出。
     * @param rawText 原始模型文本
     * @return 過濾後的乾淨文本，若僅包含思考或元資料則返回空字串
     */
    fun filter(rawText: String): String
}

/**
 * 專為 Gemini 模型設計的思考歷程過濾實作。
 * 包含：
 * 1. XML 標籤思維鏈 (<thought>...</thought>, <think>...</think>, <reasoning>...</reasoning>)
 * 2. 串流未閉合標籤思維鏈
 * 3. Gemini 影像生成與思維擴展標題段落 (**Envisioning ...**, **Crafting ...**, **Thinking Process:** 等)
 * 4. 內部圖片佔位符與 Google CDN 網址過濾
 */
class GeminiThoughtFilter : ThoughtFilter {

    private val xmlThoughtRegex = Regex(
        """<(?:thought|think|reasoning|thought_process)>[\s\S]*?(?:</(?:thought|think|reasoning|thought_process)>|$)""",
        RegexOption.IGNORE_CASE
    )

    private val imagePlaceholderUrlRegex = Regex(
        """https?://[^\s"'\\<]*image_generation_content[^\s"'\\<]*""",
        RegexOption.IGNORE_CASE
    )

    private val googleCdnUrlRegex = Regex(
        """https?://lh3\.googleusercontent\.com/[^\s"'\\<]+""",
        RegexOption.IGNORE_CASE
    )

    // 匹配思維鏈標題區塊，例如 **Gathering Inspiration for Scene**、**Thinking Process:**、**Envisioning Cyberpunk Taipei** 等
    // 以現在分詞 (Present Participle, -ing) 或 Thinking/Thought/Reasoning 關鍵結構為特徵，消除有限動詞枚舉漏洞
    private val thoughtBlockRegex = Regex(
        """(?:\*\*|#+)\s*(?:[A-Za-z]+ing|Thought|Thinking|Reasoning|Reflection|Rationale)\b[^*#\n]*\**[\s\S]*?(?=(?:\n\s*(?:---+|___+)|${'$'}))""",
        RegexOption.IGNORE_CASE
    )

    override fun filter(rawText: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText

        // 1. 移除 XML 標籤思維歷程
        text = xmlThoughtRegex.replace(text, "")

        // 2. 移除內部圖片佔位符網址
        text = imagePlaceholderUrlRegex.replace(text, "")

        // 3. 移除 Google 內部 CDN 圖片網址
        text = googleCdnUrlRegex.replace(text, "")

        // 4. 移除思維鏈標題區塊
        text = thoughtBlockRegex.replace(text, "")

        // 5. 去除首尾空白與過多的換行
        val cleaned = text.trim()

        // 6. 如果剩餘的文字只是未閉合的思考標題（串流過程中剛輸出前半段）
        if (cleaned.startsWith("**") && !cleaned.contains("\n") && !cleaned.endsWith("**")) {
            val lower = cleaned.lowercase()
            if (lower.contains("ing") || lower.contains("think") || lower.contains("thought") || lower.contains("reason")) {
                return ""
            }
        }

        return cleaned
    }
}
