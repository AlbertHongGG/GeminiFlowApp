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
 * 專為 Gemini 模型設計的語意安全思考歷程過濾實作。
 * 核心原則：
 * 1. 嚴格限縮思維特徵詞彙（如明確的 XML 標籤與 Imagen 專屬提示標題），絕不誤殺任何合法 Markdown 標題或英文動名詞（如 Writing, Coding, Building）。
 * 2. 嚴格保護文字格式：絕不任意對串流尾端執行 .trim()，確保換行符號（\n）與空白縮排即時正確傳遞。
 */
class GeminiThoughtFilter : ThoughtFilter {

    private val xmlThoughtRegex = Regex(
        """<(?:thought|think|reasoning|thought_process)>[\s\S]*?(?:</(?:thought|think|reasoning|thought_process)>|${'$'})""",
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

    // 精準匹配 Gemini 官方生圖或思維鏈洩漏標題（絕不使用泛用 [A-Za-z]+ing 避免誤殺合法標題）
    // 僅鎖定確切已知的思維標題片語：Gathering Inspiration、Envisioning、Crafting Prompt、Thinking Process、Thought Process
    private val thoughtBlockRegex = Regex(
        """(?:\*\*|#+)\s*(?:Gathering\s+Inspiration|Envisioning|Crafting\s+Prompt|Thinking\s+Process|Thought\s+Process)\b[^*#\n]*\**[\s\S]*?(?=(?:\n\s*(?:---+|___+)|${'$'}))""",
        RegexOption.IGNORE_CASE
    )

    override fun filter(rawText: String): String {
        if (rawText.isEmpty()) return ""

        var text = rawText

        // 1. 移除 XML 標籤思維歷程
        text = xmlThoughtRegex.replace(text, "")

        // 2. 移除內部圖片佔位符網址
        text = imagePlaceholderUrlRegex.replace(text, "")

        // 3. 移除 Google 內部 CDN 圖片網址
        text = googleCdnUrlRegex.replace(text, "")

        // 4. 移除專屬思維標題區塊
        text = thoughtBlockRegex.replace(text, "")

        // 5. 串流未閉合之生圖思考標題檢測（剛好輸出前半段時）
        if (text.startsWith("**") && !text.contains("\n") && !text.endsWith("**")) {
            val lower = text.lowercase()
            if (lower.contains("gathering inspiration") ||
                lower.contains("envisioning") ||
                lower.contains("crafting prompt") ||
                lower.contains("thinking process") ||
                lower.contains("thought process")
            ) {
                return ""
            }
        }

        // 6. 若過濾後僅剩空白字元則返回空字串；若有有效文字，絕對不執行尾端 trim()，保留所有合法換行與空白
        if (text.all { it.isWhitespace() }) {
            return ""
        }

        // 7. 若原始輸入並非以空白字元結尾，但網址被過濾後在末尾殘留了孤立空格，則安全清除該殘留空格
        if (!rawText.endsWith(" ") && text.endsWith(" ")) {
            text = text.trimEnd(' ')
        }

        return text
    }
}
