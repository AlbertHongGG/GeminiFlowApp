package com.geminiflow.app.presentation.ui.playground.model

/**
 * 沙盒模型規格資料物件。
 */
data class PlaygroundModelSpec(
    val id: String,
    val displayName: String,
    val tag: String,
    val description: String,
    val isImageCapable: Boolean = false,
    val isThinkingCapable: Boolean = false
) {
    companion object {
        val AVAILABLE_MODELS = listOf(
            PlaygroundModelSpec(
                id = "gemini-3-pro",
                displayName = "3-Pro (旗艦生圖)",
                tag = "旗艦推理 · Imagen 3",
                description = "Google 頂級旗艦模型，支援多模態文字問答與超擬真 Imagen 3 圖片生成",
                isImageCapable = true
            ),
            PlaygroundModelSpec(
                id = "gemini-3.5-flash",
                displayName = "3.5-Flash (極速)",
                tag = "超低延遲 · 日常問答",
                description = "極速輕量化模型，適合連續文字對話、日常查詢與即時程式碼分析",
                isImageCapable = false
            ),
            PlaygroundModelSpec(
                id = "gemini-3.7-flash",
                displayName = "3.7-Flash (平衡)",
                tag = "新世代多模態",
                description = "具備卓越綜合推理與速度的新世代平衡旗艦，各類任務均有優異表現",
                isImageCapable = false
            ),
            PlaygroundModelSpec(
                id = "gemini-3.7-flash-thinking",
                displayName = "3.7-Flash Thinking (深度思考)",
                tag = "思維鏈 · 複雜推理",
                description = "具備混合式深度思考架構，專精於複雜數學、演算法、邏輯謎題推導",
                isThinkingCapable = true
            ),
            PlaygroundModelSpec(
                id = "gemini-3-flash-image",
                displayName = "Flash-Image (純圖)",
                tag = "極速出圖",
                description = "專注於單張高解析度圖片的快速生成與風格轉繪",
                isImageCapable = true
            )
        )

        val DEFAULT = AVAILABLE_MODELS.first()

        fun findById(id: String): PlaygroundModelSpec {
            return AVAILABLE_MODELS.find { it.id == id } ?: DEFAULT
        }
    }
}
