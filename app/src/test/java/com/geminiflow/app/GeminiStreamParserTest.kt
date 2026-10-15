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
        val mockLine = """[["wrb.fr",null,"[null,[\"c_12345\",null,null,null,null,null,null,\"AwAAAAAAAAAQANM7mBjXKZS5cPttExo\"],null,null,[null,null,null,null,[[\"test_id\",[\"Hello world!\"],null,null,null,null,null,null,null,null,[]]]]]"]]"""

        val result = parser.extractTextDelta(mockLine)
        assertEquals("Hello world!", result.delta)
        assertEquals("c_12345", result.conversationId)
        assertEquals("AwAAAAAAAAAQANM7mBjXKZS5cPttExo", result.continuationToken)

        // Next delta with same prefix
        val nextMockLine = """[["wrb.fr",null,"[null,[\"c_12345\",null,null,null,null,null,null,\"AwAAAAAAAAAQANM7mBjXKZS5cPttExo\"],null,null,[null,null,null,null,[[\"test_id\",[\"Hello world! How are you?\"],null,null,null,null,null,null,null,null,[]]]]]"]]"""
        val nextResult = parser.extractTextDelta(nextMockLine)
        assertEquals(" How are you?", nextResult.delta)
    }

    @Test
    fun testExtractConversationContext_standalone() {
        val parser = GeminiStreamParser()
        val mockLine = """[["wrb.fr",null,"[null,[\"c_fb6da9be329112e7\",null,null,null,null,null,null,\"AwAAAAAAAAAQANM7mBjXKZS5cPttExo\"],null,null,[null,null,null,null,[[\"rc_test\",[\"回覆文字\"],null,null,null,null,null,null,null,null,[]]]]]"]]"""
        val result = parser.extractTextDelta(mockLine)
        assertEquals("c_fb6da9be329112e7", result.conversationId)
        assertEquals("AwAAAAAAAAAQANM7mBjXKZS5cPttExo", result.continuationToken)
        assertEquals("回覆文字", result.delta)
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

    @Test
    fun testIsEndOfStream_detection() {
        val parser = GeminiStreamParser()

        // 1. End Frame [["e", 1]]
        val endFrame1 = """[["e",1,null,null,120]]"""
        assertTrue(parser.isEndOfStream(endFrame1))

        // 2. Data Interaction Finish Frame [["di", null]]
        val diFrame1 = """[["di",null,null,null,null,[]]]"""
        assertTrue(parser.isEndOfStream(diFrame1))

        // 3. Mixed / secondary position di frame
        val mixedFrame = """[["wrb.fr",null,"[null]"],["di",null]]"""
        assertTrue(parser.isEndOfStream(mixedFrame))

        // 4. Normal content frame - should NOT be end of stream
        val normalFrame = """[["wrb.fr",null,"[null,null,null,null,[[\"test\",[\"Hello\"]]]]]"]]"""
        org.junit.Assert.assertFalse(parser.isEndOfStream(normalFrame))

        // 5. Empty line - should NOT be end of stream
        org.junit.Assert.assertFalse(parser.isEndOfStream(""))
    }

    @Test
    fun testExtractContinuationToken_fromPart2Key26() {
        val parser = GeminiStreamParser()
        val part2Json = org.json.JSONObject().apply {
            put("26", "AwAAAAAAAAAQANM7mBjXKZS5cPttExoLate")
        }
        val root = org.json.JSONArray().apply {
            put(org.json.JSONObject.NULL) // 0
            put(org.json.JSONArray().put("c_test_conv_999")) // 1: conv_id
            put(part2Json) // 2: part2 with key 26
        }
        val (convId, contToken) = parser.extractConversationContext(root)
        assertEquals("c_test_conv_999", convId)
        assertEquals("AwAAAAAAAAAQANM7mBjXKZS5cPttExoLate", contToken)
    }
}
