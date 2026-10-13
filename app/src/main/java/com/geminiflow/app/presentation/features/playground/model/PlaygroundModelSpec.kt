package com.geminiflow.app.presentation.features.playground.model

import com.geminiflow.app.domain.model.chat.ModelFamily

data class PlaygroundModelSpec(
    val family: ModelFamily,
    val id: String,
    val displayName: String,
    val shortName: String,
    val tag: String,
    val isImageCapable: Boolean = false
) {
    companion object {
        val AVAILABLE_MODELS = listOf(
            PlaygroundModelSpec(
                family = ModelFamily.FLASH,
                id = "gemini-3.8-flash",
                displayName = "Gemini 3.8 Flash",
                shortName = "3.8 Flash",
                tag = "極速",
                isImageCapable = false
            ),
            PlaygroundModelSpec(
                family = ModelFamily.PRO,
                id = "gemini-3-pro",
                displayName = "Gemini 3 Pro",
                shortName = "Pro",
                tag = "旗艦 · 生圖",
                isImageCapable = true
            )
        )

        val DEFAULT = AVAILABLE_MODELS.first()

        fun findByFamily(family: ModelFamily): PlaygroundModelSpec {
            return AVAILABLE_MODELS.firstOrNull { it.family == family } ?: DEFAULT
        }

        fun findById(id: String): PlaygroundModelSpec {
            val family = ModelFamily.fromModelName(id)
            return findByFamily(family)
        }
    }
}
