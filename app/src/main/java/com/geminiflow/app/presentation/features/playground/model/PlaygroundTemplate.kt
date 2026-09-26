package com.geminiflow.app.presentation.features.playground.model

data class PlaygroundTemplate(
    val id: String,
    val category: String,
    val title: String,
    val promptText: String,
    val description: String
) {
    companion object {
        val TEMPLATES = listOf(
            PlaygroundTemplate(
                id = "self-intro",
                category = "系統巡檢",
                title = "自我介紹與功能巡檢",
                promptText = "請用繁體中文自我介紹，並告訴我你支援什麼功能與核心架構。",
                description = "測試繁體中文理解度與模型自我認知架構"
            ),
            PlaygroundTemplate(
                id = "mars-shiba",
                category = "圖像創作",
                title = "火星探險柴犬宇航員",
                promptText = "請畫一隻穿著宇航服在火星探險的柴犬，高解析度寫實風格，背景有火星地表與星空。",
                description = "測試多模態 Imagen 3 寫實圖像生成能力"
            ),
            PlaygroundTemplate(
                id = "quantum-computing",
                category = "深度科普",
                title = "量子運算深度解析",
                promptText = "請簡明扼要解釋量子運算與傳統運算的差異，並以日常生活舉例說明疊加態與量子糾纏。",
                description = "測試長文本推理與深入淺出的科普解釋能力"
            ),
            PlaygroundTemplate(
                id = "code-refactor",
                category = "程式開發",
                title = "代碼重構與架構審查",
                promptText = "請幫我檢視一段 Kotlin 程式碼，並從時間複雜度、記憶體配置與物件導向設計原則提出優化建議。",
                description = "測試代碼分析、效能審核與演算法重構能力"
            ),
            PlaygroundTemplate(
                id = "cyberpunk-taipei",
                category = "圖像創作",
                title = "賽博龐克台北夜雨",
                promptText = "未來賽博龐克風格的雨夜台北街頭，霓虹燈倒影、高科技飛行器穿梭，8K 奇幻光影極致細節。",
                description = "測試複雜光影渲染與科幻場景生成表現"
            ),
            PlaygroundTemplate(
                id = "logic-puzzle",
                category = "思維推導",
                title = "三個水果箱邏輯謎題",
                promptText = "有三個箱子，一個只裝蘋果，一個只裝橘子，一個裝蘋果和橘子。三個箱子標籤全錯。你只能從一個箱子拿一顆水果，如何確定三個箱子分別裝什麼？請逐步推導。",
                description = "測試思維鏈（Chain of Thought）逐步邏輯分析能力"
            )
        )
    }
}
