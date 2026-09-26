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

    @Test
    fun testExtractImageExportMetadata() {
        val parser = GeminiStreamParser()
        val mockInner = """[null,["c_conv123","r_resp456"],null,null,[["rc_choice789"],null,null,null,null,null,null,null,null,null,null,null,[null,null,null,null,null,null,null,[[[null,null,null,[null,1,"watermarked.jpg","https://lh3.googleusercontent.com/gg-dl/preview123",null,"${'$'}Aesyi1TokenSecret"]],["http://googleusercontent.com/image_generation_content/0_123"],null,[20,"Prompt text"],null,null,null,null,"im_img999"]]]]]"""
        val mockLine = org.json.JSONArray().apply {
            put(org.json.JSONArray().apply {
                put("wrb.fr")
                put(org.json.JSONObject.NULL)
                put(mockInner)
            })
        }.toString()

        val metadata = parser.extractImageExportMetadata(mockLine)
        assertNotNull(metadata)
        assertEquals("c_conv123", metadata?.conversationId)
        assertEquals("r_resp456", metadata?.responseId)
        assertEquals("rc_choice789", metadata?.choiceId)
        assertEquals("im_img999", metadata?.imageId)
        assertEquals("https://lh3.googleusercontent.com/gg-dl/preview123", metadata?.previewUrl)
    }

    @Test
    fun testSuppressesThinkingInPart4_0_37DuringImageGeneration() {
        val parser = GeminiStreamParser()
        val thinkingText = "**Gathering Inspiration for Scene**\n\nI am currently focusing on capturing the essence of a cyberpunk rain-soaked Taipei street."

        val root = org.json.JSONArray()
        root.put(org.json.JSONObject.NULL) // 0
        root.put(org.json.JSONArray().put("c_conv123").put("r_resp456")) // 1: sessions
        root.put(org.json.JSONObject.NULL) // 2
        root.put(org.json.JSONObject.NULL) // 3

        val part4 = org.json.JSONArray()
        // part4[0]: choice array with choiceId and empty text
        val part4_0 = org.json.JSONArray().put("rc_choice789").put(org.json.JSONArray().put(""))
        part4.put(part4_0) // index 0

        // 填充到 index 12
        for (i in 1..11) {
            part4.put(org.json.JSONObject.NULL)
        }

        // index 12: image block
        val imgBlock = org.json.JSONArray().apply {
            put(org.json.JSONArray().put(org.json.JSONObject.NULL).put(org.json.JSONObject.NULL).put(org.json.JSONObject.NULL).put(
                org.json.JSONArray().put(org.json.JSONObject.NULL).put(1).put("watermarked.jpg").put("https://lh3.googleusercontent.com/gg-dl/preview123").put(org.json.JSONObject.NULL).put("\$Aesyi1TokenSecret")
            ))
            put(org.json.JSONArray().put("http://googleusercontent.com/image_generation_content/0_123"))
            put(org.json.JSONObject.NULL)
            put(org.json.JSONArray().put(20).put("Prompt text"))
            put(org.json.JSONObject.NULL)
            put(org.json.JSONObject.NULL)
            put(org.json.JSONObject.NULL)
            put(org.json.JSONObject.NULL)
            put("im_img999") // index 8: imageId
        }
        val imgBlockWrapper = org.json.JSONArray().apply {
            for (i in 0..6) put(org.json.JSONObject.NULL)
            put(org.json.JSONArray().put(org.json.JSONArray().put(imgBlock))) // index 7
        }
        part4.put(imgBlockWrapper) // index 12

        // 填充到 index 37: thinking process
        for (i in 13..36) {
            part4.put(org.json.JSONObject.NULL)
        }
        val thinkingArray = org.json.JSONArray().put(org.json.JSONArray().put(thinkingText))
        part4.put(thinkingArray) // index 37

        root.put(part4) // 4

        val mockLine = org.json.JSONArray().apply {
            put(org.json.JSONArray().apply {
                put("wrb.fr")
                put(org.json.JSONObject.NULL)
                put(root.toString())
            })
        }.toString()

        // 驗證：正文絕對不能提取到 index 37 的思考歷程，delta 必須為 null
        val result = parser.extractTextDelta(mockLine)
        assertEquals(null, result.delta)

        // 驗證：生圖中繼資料仍能精準提取
        val exportMeta = parser.extractImageExportMetadata(mockLine)
        assertNotNull(exportMeta)
        assertEquals("im_img999", exportMeta?.imageId)
        assertEquals("https://lh3.googleusercontent.com/gg-dl/preview123", exportMeta?.previewUrl)
    }
}
