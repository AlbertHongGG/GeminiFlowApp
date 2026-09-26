package com.geminiflow.app

import com.geminiflow.app.data.network.RequestTelemetryTag
import com.geminiflow.app.data.network.TrafficLoggingInterceptor
import com.geminiflow.app.data.storage.TrafficLogManager
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class TrafficLoggingInterceptorTest {

    @Test
    fun testSupportsChinesePromptThroughMemoryTagWithoutCrashing() {
        val chinesePrompt = "未來賽博龐克風格的雨夜台北街頭，霓虹燈倒影、高科技飛行器穿梭，8K 奇幻光影極致細節。"
        val logManager = TrafficLogManager()
        val interceptor = TrafficLoggingInterceptor(logManager)

        val request = Request.Builder()
            .url("https://gemini.google.com/_/BardChatUi/data/batchexecute")
            .tag(
                RequestTelemetryTag::class.java,
                RequestTelemetryTag(
                    promptSummary = chinesePrompt,
                    source = "gemini-3-pro"
                )
            )
            .build()

        val fakeChain = object : Interceptor.Chain {
            override fun request(): Request = request
            override fun proceed(request: Request): Response {
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("OK")
                    .build()
            }
            override fun connection(): Connection? = null
            override fun call(): okhttp3.Call = throw UnsupportedOperationException()
            override fun connectTimeoutMillis(): Int = 1000
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun readTimeoutMillis(): Int = 1000
            override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun writeTimeoutMillis(): Int = 1000
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        }

        val response = interceptor.intercept(fakeChain)
        assertEquals(200, response.code)

        val recordedLogs = logManager.logs.value
        assertEquals(1, recordedLogs.size)
        val logItem = recordedLogs.first()
        assertEquals(chinesePrompt, logItem.promptSummary)
        assertEquals("/stream (gemini-3-pro)", logItem.path)
        assertEquals(200, logItem.statusCode)
    }
}
