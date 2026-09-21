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
}
