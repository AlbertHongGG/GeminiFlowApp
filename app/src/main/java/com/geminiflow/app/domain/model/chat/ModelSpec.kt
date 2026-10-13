package com.geminiflow.app.domain.model.chat

enum class ModelFamily(val modelId: Int, val modelHash: String) {
    FLASH(1, "56fdd199312815e2"),
    PRO(3, "e6fa609c3fa255c0"),
    ARGON(8, "95a4c0abcba24364");

    companion object {
        fun fromModelName(name: String?): ModelFamily {
            val lower = name?.trim()?.lowercase() ?: ""
            return when {
                listOf("argon", "gemini-4", "gemini4").any { lower.contains(it) } -> ARGON
                lower.contains("pro") -> PRO
                else -> FLASH
            }
        }
    }
}

enum class ThinkingLevel(val levelValue: Int) {
    LOW(1),
    MEDIUM(4),
    HIGH(2);

    companion object {
        fun fromValue(value: Any?): ThinkingLevel {
            return when (value) {
                is ThinkingLevel -> value
                is Int -> entries.find { it.levelValue == value } ?: LOW
                is String -> {
                    val s = value.trim().lowercase()
                    when (s) {
                        "4", "medium", "med", "mid" -> MEDIUM
                        "2", "high", "on", "true" -> HIGH
                        else -> LOW
                    }
                }
                else -> LOW
            }
        }
    }
}

data class ModelSpec(
    val family: ModelFamily,
    val thinking: ThinkingLevel,
    val rawName: String
) {
    val modelId: Int get() = family.modelId
    val modelHash: String get() = family.modelHash
    val thinkingLevel: Int get() = thinking.levelValue

    companion object {
        fun resolve(modelName: String? = null, thinking: Any? = null): ModelSpec {
            val raw = modelName?.trim()?.takeIf { it.isNotBlank() } ?: "gemini-3.8-flash"
            val family = ModelFamily.fromModelName(raw)
            val thinkingLvl = ThinkingLevel.fromValue(thinking)
            return ModelSpec(family = family, thinking = thinkingLvl, rawName = raw)
        }
    }
}
