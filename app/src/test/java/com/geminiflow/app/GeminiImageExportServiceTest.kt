package com.geminiflow.app

import com.geminiflow.app.data.network.export.GeminiImageExportService
import com.geminiflow.app.data.network.export.ImageExportResponseParser
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.export.ImageExportMetadata
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class GeminiImageExportServiceTest {

    @Test
    fun testImageExportResponseParser_ExtractsAndNormalizesUrl() {
        val parser = ImageExportResponseParser()
        val mockResponseBody = """)]}'

335
[["wrb.fr","c8o8Fe","[\"https://lh3.googleusercontent.com/gg-dl/AHiVA1kfk8ZDsCDl56RQEJtwS\"]",null,null,null,"generic"]]
"""

        val extracted = parser.parse(mockResponseBody)
        assertNotNull(extracted)
        assertEquals(
            "https://lh3.googleusercontent.com/gg-dl/AHiVA1kfk8ZDsCDl56RQEJtwS=s0",
            extracted
        )
    }

    @Test
    fun testImageExportResponseParser_ThrowsOnServerError() {
        val parser = ImageExportResponseParser()
        val mockErrorResponse = """)]}'

105
[["wrb.fr","c8o8Fe",null,null,null,[13],"generic"]]
"""

        try {
            parser.parse(mockErrorResponse)
            org.junit.Assert.fail("應當拋出例外")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("13") == true)
        }
    }

    @Test
    fun testExportHighResolutionImageUrl_BuildsBatchexecuteRequestAndReturnsUrl() = runBlocking {
        var recordedRequest: Request? = null
        var recordedBody: String? = null

        val mockInterceptor = Interceptor { chain ->
            val req = chain.request()
            recordedRequest = req
            val buffer = okio.Buffer()
            req.body?.writeTo(buffer)
            recordedBody = buffer.readUtf8()

            val mockResponseJson = """)]}'
200
[["wrb.fr","c8o8Fe","[\"https://lh3.googleusercontent.com/gg-dl/super_res_2816x1536\"]",null,null,null,"generic"]]"""

            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(mockResponseJson.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val testClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val service = GeminiImageExportService(testClient)
        val tokens = GeminiTokens(snlm0e = "mock_snlm0e", sid = "12345678")
        val cookies = mapOf("__Secure-1PSID" to "mock_psid_val")

        val metadata = ImageExportMetadata(
            conversationId = "c_conv123",
            responseId = "r_resp456",
            choiceId = "rc_choice789",
            imageId = "im_image999",
            imageBlockJson = """[[null,null,null,[null,1,"watermarked.jpg","https://lh3.googleusercontent.com/preview",null,"${'$'}Aesyi1TokenSecret"]],["http://googleusercontent.com/image_generation_content/0_123"],null,[20,"Prompt"],null,null,null,null,"im_image999"]""",
            previewUrl = "https://lh3.googleusercontent.com/preview"
        )

        val result = service.exportHighResolutionImageUrl(
            metadata = metadata,
            tokens = tokens,
            cookies = cookies
        )

        assertTrue(result.isSuccess)
        assertEquals(
            "https://lh3.googleusercontent.com/gg-dl/super_res_2816x1536=s0",
            result.getOrNull()
        )

        assertNotNull(recordedRequest)
        assertEquals("POST", recordedRequest?.method)
        assertEquals("c8o8Fe", recordedRequest?.url?.queryParameter("rpcids"))
        assertEquals("/app/conv123", recordedRequest?.url?.queryParameter("source-path"))

        assertNotNull(recordedBody)
        val decodedBody = URLDecoder.decode(recordedBody, "UTF-8")
        assertTrue(decodedBody.contains("mock_snlm0e"))
        assertTrue(decodedBody.contains("c8o8Fe"))
        assertTrue(decodedBody.contains("\$Aesyi1TokenSecret"))
        assertTrue(decodedBody.contains("c_conv123"))
        assertTrue(decodedBody.contains("rc_choice789"))
        assertTrue(decodedBody.contains("im_image999"))
    }
}
