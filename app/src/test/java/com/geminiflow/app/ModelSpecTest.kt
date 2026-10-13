package com.geminiflow.app

import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.model.chat.ModelFamily
import com.geminiflow.app.domain.model.chat.ModelSpec
import com.geminiflow.app.domain.model.chat.ThinkingLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelSpecTest {

    @Test
    fun testModelFamily_resolution() {
        assertEquals(ModelFamily.FLASH, ModelFamily.fromModelName("gemini-3.8-flash"))
        assertEquals(ModelFamily.FLASH, ModelFamily.fromModelName("gemini-3.5-flash"))
        assertEquals(ModelFamily.FLASH, ModelFamily.fromModelName("flash"))
        assertEquals(ModelFamily.FLASH, ModelFamily.fromModelName(null))

        assertEquals(ModelFamily.PRO, ModelFamily.fromModelName("gemini-3-pro"))
        assertEquals(ModelFamily.PRO, ModelFamily.fromModelName("pro"))
        assertEquals(ModelFamily.PRO, ModelFamily.fromModelName("gemini-pro-vision"))

        assertEquals(ModelFamily.ARGON, ModelFamily.fromModelName("gemini4Argon"))
        assertEquals(ModelFamily.ARGON, ModelFamily.fromModelName("argon"))
        assertEquals(ModelFamily.ARGON, ModelFamily.fromModelName("gemini-4"))
        assertEquals(ModelFamily.ARGON, ModelFamily.fromModelName("gemini4"))

        assertEquals(1, ModelFamily.FLASH.modelId)
        assertEquals("56fdd199312815e2", ModelFamily.FLASH.modelHash)

        assertEquals(3, ModelFamily.PRO.modelId)
        assertEquals("e6fa609c3fa255c0", ModelFamily.PRO.modelHash)

        assertEquals(8, ModelFamily.ARGON.modelId)
        assertEquals("95a4c0abcba24364", ModelFamily.ARGON.modelHash)
    }

    @Test
    fun testThinkingLevel_resolution() {
        assertEquals(ThinkingLevel.LOW, ThinkingLevel.fromValue(null))
        assertEquals(ThinkingLevel.LOW, ThinkingLevel.fromValue("low"))
        assertEquals(ThinkingLevel.LOW, ThinkingLevel.fromValue("off"))
        assertEquals(ThinkingLevel.LOW, ThinkingLevel.fromValue(1))

        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromValue("medium"))
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromValue("med"))
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromValue("mid"))
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromValue(4))

        assertEquals(ThinkingLevel.HIGH, ThinkingLevel.fromValue("high"))
        assertEquals(ThinkingLevel.HIGH, ThinkingLevel.fromValue("on"))
        assertEquals(ThinkingLevel.HIGH, ThinkingLevel.fromValue("true"))
        assertEquals(ThinkingLevel.HIGH, ThinkingLevel.fromValue(2))

        assertEquals(1, ThinkingLevel.LOW.levelValue)
        assertEquals(4, ThinkingLevel.MEDIUM.levelValue)
        assertEquals(2, ThinkingLevel.HIGH.levelValue)
    }

    @Test
    fun testModelSpec_orthogonality() {
        // Model name does not bleed into thinking level
        val spec1 = ModelSpec.resolve("gemini-3.7-flash-thinking", null)
        assertEquals(ModelFamily.FLASH, spec1.family)
        assertEquals(ThinkingLevel.LOW, spec1.thinking)
        assertEquals(1, spec1.modelId)
        assertEquals(1, spec1.thinkingLevel)

        val spec2 = ModelSpec.resolve("gemini-3.8-flash", "medium")
        assertEquals(ModelFamily.FLASH, spec2.family)
        assertEquals(ThinkingLevel.MEDIUM, spec2.thinking)
        assertEquals(1, spec2.modelId)
        assertEquals(4, spec2.thinkingLevel)

        val spec3 = ModelSpec.resolve("gemini-3-pro", "high")
        assertEquals(ModelFamily.PRO, spec3.family)
        assertEquals(ThinkingLevel.HIGH, spec3.thinking)
        assertEquals(3, spec3.modelId)
        assertEquals(2, spec3.thinkingLevel)

        val spec4 = ModelSpec.resolve("gemini4Argon", "medium")
        assertEquals(ModelFamily.ARGON, spec4.family)
        assertEquals(ThinkingLevel.MEDIUM, spec4.thinking)
        assertEquals(8, spec4.modelId)
        assertEquals(4, spec4.thinkingLevel)
    }

    @Test
    fun testChatRequest_modelSpecIntegration() {
        val req1 = ChatRequest(
            prompt = "測試",
            model = "gemini-3-pro",
            thinking = ThinkingLevel.HIGH
        )
        assertEquals(ModelFamily.PRO, req1.modelSpec.family)
        assertEquals(ThinkingLevel.HIGH, req1.modelSpec.thinking)
        assertEquals(3, req1.modelSpec.modelId)
        assertEquals(2, req1.modelSpec.thinkingLevel)

        val req2 = ChatRequest(
            prompt = "測試Argon",
            model = "gemini4Argon",
            thinking = ThinkingLevel.MEDIUM
        )
        assertEquals(ModelFamily.ARGON, req2.modelSpec.family)
        assertEquals(ThinkingLevel.MEDIUM, req2.modelSpec.thinking)
        assertEquals(8, req2.modelSpec.modelId)
        assertEquals(4, req2.modelSpec.thinkingLevel)
    }
}
