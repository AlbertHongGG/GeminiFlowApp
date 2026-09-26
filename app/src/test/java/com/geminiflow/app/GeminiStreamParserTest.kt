package com.geminiflow.app

import com.geminiflow.app.data.api.GeminiStreamParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiStreamParserTest {

    @Test
    fun testExtractTextDelta() {
        val parser = GeminiStreamParser()
        val mockLine = """[["wrb.fr",null,"[null,[\"c_12345\"],null,null,[null,null,null,null,[[\"test_id\",[\"Hello world!\"],null,null,null,null,null,null,null,null,[]]]]]"]]"""

        val result = parser.extractTextDelta(mockLine)
        assertEquals("Hello world!", result.delta)
        assertNotNull(result.sessionIds)
        assertEquals("c_12345", result.sessionIds?.first())

        // Next delta with same prefix
        val nextMockLine = """[["wrb.fr",null,"[null,[\"c_12345\"],null,null,[null,null,null,null,[[\"test_id\",[\"Hello world! How are you?\"],null,null,null,null,null,null,null,null,[]]]]]"]]"""
        val nextResult = parser.extractTextDelta(nextMockLine)
        assertEquals(" How are you?", nextResult.delta)
    }

    @Test
    fun testExtractImageCandidates() {
        val parser = GeminiStreamParser()
        val mockLine = """[["wrb.fr",null,"[null,null,null,null,[\"Check this image: https://lh3.googleusercontent.com/gg-dl/abc123xyz\"]]"]]"""

        val images = parser.extractImageCandidates(mockLine)
        assertTrue(images.isNotEmpty())
        assertEquals("output", parser.classifyImageUrl(images[0]))
    }

    @Test
    fun testSuppressesEnvisioningThoughtsDuringImageGeneration() {
        val parser = GeminiStreamParser()
        val envisioningText = "**Envisioning Cyberpunk Taipei**\n\nI'm focusing on a future Taipei, in a rain-soaked night. I envision neon reflections, high-tech flying vehicles, and intricate details."
        val mockLine = """[["wrb.fr",null,"[null,null,null,null,[null,null,null,null,[[\"test_id\",[\"$envisioningText\"],null,null,null,null,null,null,null,null,[]]]]]"]]"""

        val result = parser.extractTextDelta(mockLine)
        // 思考過程必須被過濾，delta 為 null
        assertEquals(null, result.delta)
    }

    @Test
    fun testFiltersXmlThoughtsAndLeavesFinalAnswer() {
        val parser = GeminiStreamParser()
        val thinkingWithAnswer = "<thought>Calculating answer...</thought>Paris is the capital of France."
        val mockLine = """[["wrb.fr",null,"[null,null,null,null,[null,null,null,null,[[\"test_id\",[\"$thinkingWithAnswer\"],null,null,null,null,null,null,null,null,[]]]]]"]]"""

        val result = parser.extractTextDelta(mockLine)
        assertEquals("Paris is the capital of France.", result.delta)
    }

    @Test
    fun testRemovesImagePlaceholderUrlFromEmittedText() {
        val parser = GeminiStreamParser()
        val textWithUrl = "http://googleusercontent.com/image_generation_content/0_635"
        val mockLine = """[["wrb.fr",null,"[null,null,null,null,[null,null,null,null,[[\"test_id\",[\"$textWithUrl\"],null,null,null,null,null,null,null,null,[]]]]]"]]"""

        val result = parser.extractTextDelta(mockLine)
        assertEquals(null, result.delta)
    }
}
