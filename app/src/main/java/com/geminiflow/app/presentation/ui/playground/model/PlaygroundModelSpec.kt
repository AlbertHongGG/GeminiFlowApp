package com.geminiflow.app.presentation.ui.playground.model

/**
 * 沙盒模型規格資料物件 (Clean OOP Domain Model)。
 * 僅保留純粹模型核心識別與能力屬性，杜絕無效行銷贅言。
 */
data class PlaygroundModelSpec(
    val id: String,
    val displayName: String,
    val tag: String,
    val isImageCapable: Boolean = false,
    val isThinkingCapable: Boolean = false
) {
    companion object {
        val AVAILABLE_MODELS = listOf(
            PlaygroundModelSpec(
                id = "gemini-3-pro",
                displayName = "Gemini 3 Pro",
                tag = "旗艦 · 生圖",
                isImageCapable = true
            ),
            PlaygroundModelSpec(
                id = "gemini-3.5-flash",
                displayName = "Gemini 3.5 Flash",
                tag = "極速響應",
                isImageCapable = false
            ),
            PlaygroundModelSpec(
                id = "gemini-3.7-flash",
                displayName = "Gemini 3.7 Flash",
                tag = "平衡新世代",
                isImageCapable = false
            ),
            PlaygroundModelSpec(
                id = "gemini-3.7-flash-thinking",
                displayName = "Gemini 3.7 Thinking",
                tag = "深度思考",
                isThinkingCapable = true
            ),
            PlaygroundModelSpec(
                id = "gemini-3-flash-image",
                displayName = "Flash Image",
                tag = "極速出圖",
                isImageCapable = true
            )
        )

        val DEFAULT = AVAILABLE_MODELS.first()

        fun findById(id: String): PlaygroundModelSpec {
            return AVAILABLE_MODELS.find { it.id == id } ?: DEFAULT
        }
    }
}
