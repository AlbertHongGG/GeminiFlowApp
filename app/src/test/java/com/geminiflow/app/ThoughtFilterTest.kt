package com.geminiflow.app

import com.geminiflow.app.data.api.filter.GeminiThoughtFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class ThoughtFilterTest {

    private val filter = GeminiThoughtFilter()

    @Test
    fun testFiltersGatheringInspirationLeakedThought() {
        val raw = """
            **Gathering Inspiration for Scene**
            
            I am currently focusing on capturing the essence of a cyberpunk rain-soaked Taipei street. I'm exploring search terms like "Taipei neon street" and "Taipei landmarks cyberpunk" to define the setting. I am looking for ways to visualize neon reflections and incorporate high-tech flying vehicles into the scene.
        """.trimIndent()

        val filtered = filter.filter(raw)
        assertEquals("", filtered)
    }

    @Test
    fun testFiltersEnvisioningThought() {
        val raw = """
            **Envisioning Cyberpunk Taipei**
            
            I'm focusing on a future Taipei, in a rain-soaked night. I envision neon reflections, high-tech flying vehicles, and intricate details.
        """.trimIndent()

        val filtered = filter.filter(raw)
        assertEquals("", filtered)
    }

    @Test
    fun testFiltersXmlThoughtTags() {
        val raw = "<thought>Analyzing the request...</thought>Here is the requested answer."
        val filtered = filter.filter(raw)
        assertEquals("Here is the requested answer.", filtered)
    }

    @Test
    fun testFiltersInternalImagePlaceholderAndCdnUrls() {
        val raw = "Generated: https://googleusercontent.com/image_generation_content/12345 and https://lh3.googleusercontent.com/gg-dl/abc123xyz"
        val filtered = filter.filter(raw)
        assertEquals("Generated:  and", filtered)
    }

    @Test
    fun testPreservesLegitimateMarkdownBoldText() {
        val raw = "**台北 101** 是台灣代表性的摩天大樓，位於信義計畫區。"
        val filtered = filter.filter(raw)
        assertEquals("**台北 101** 是台灣代表性的摩天大樓，位於信義計畫區。", filtered)
    }

    @Test
    fun testFiltersUnclosedStreamingThoughtHeader() {
        val raw = "**Gathering Inspiration"
        val filtered = filter.filter(raw)
        assertEquals("", filtered)
    }
}
