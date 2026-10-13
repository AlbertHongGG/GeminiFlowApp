package com.geminiflow.app.presentation.features.playground

import com.geminiflow.app.domain.model.chat.ModelFamily
import com.geminiflow.app.presentation.features.playground.model.PlaygroundModelSpec
import com.geminiflow.app.presentation.features.playground.model.PlaygroundTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaygroundDomainTest {

    @Test
    fun modelSpec_verifiesCatalogAndFallback() {
        val models = PlaygroundModelSpec.AVAILABLE_MODELS
        assertTrue(models.isNotEmpty())

        val defaultModel = PlaygroundModelSpec.DEFAULT
        assertEquals("gemini-3.8-flash", defaultModel.id)
        assertEquals(ModelFamily.FLASH, defaultModel.family)
        assertEquals("極速", defaultModel.tag)
        assertFalse(defaultModel.isImageCapable)

        val foundFlash = PlaygroundModelSpec.findByFamily(ModelFamily.FLASH)
        assertEquals(defaultModel, foundFlash)

        val foundPro = PlaygroundModelSpec.findByFamily(ModelFamily.PRO)
        assertEquals("gemini-3-pro", foundPro.id)
        assertEquals(ModelFamily.PRO, foundPro.family)
        assertEquals("旗艦 · 生圖", foundPro.tag)
        assertTrue(foundPro.isImageCapable)

        // Fallback checks
        val fallback = PlaygroundModelSpec.findById("unknown-model-xyz")
        assertEquals(defaultModel, fallback)

        val proFallback = PlaygroundModelSpec.findById("custom-pro-variant")
        assertEquals(foundPro, proFallback)
    }

    @Test
    fun templateCatalog_verifiesPredefinedTemplates() {
        val templates = PlaygroundTemplate.TEMPLATES
        assertTrue(templates.size >= 5)

        templates.forEach { template ->
            assertTrue(template.id.isNotBlank())
            assertTrue(template.title.isNotBlank())
            assertTrue(template.promptText.isNotBlank())
            assertTrue(template.category.isNotBlank())
        }

        val selfIntro = templates.find { it.id == "self-intro" }
        assertNotNull(selfIntro)
        assertEquals("系統巡檢", selfIntro?.category)

        val marsShiba = templates.find { it.id == "mars-shiba" }
        assertNotNull(marsShiba)
        assertEquals("圖像創作", marsShiba?.category)
    }
}
