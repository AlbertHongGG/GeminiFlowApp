package com.geminiflow.app.domain.telemetry

import com.geminiflow.app.domain.model.common.AuthenticationRequiredException
import com.geminiflow.app.domain.model.common.NetworkException
import com.geminiflow.app.domain.model.common.PayloadException
import com.geminiflow.app.domain.model.common.RateLimitException
import com.geminiflow.app.domain.model.common.StreamingActivityTimeoutException
import com.geminiflow.app.domain.model.common.TokenExpiredException
import com.geminiflow.app.domain.telemetry.classifier.ServerRequestErrorClassifier
import com.geminiflow.app.domain.telemetry.model.ServerRequestErrorAttribution
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.nio.channels.ClosedChannelException

class ServerRequestErrorClassifierTest {

    @Test
    fun testAuthenticationRequiredExceptionClassifiedAsServerFault() {
        val ex = AuthenticationRequiredException("請先登入 Google")
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ServerFault)
        assertFalse(attribution.isClientFault)
        assertEquals(401, attribution.statusCode)
        assertTrue(attribution.diagnosticAction.contains("Google"))
    }

    @Test
    fun testTokenExpiredExceptionClassifiedAsServerFault() {
        val ex = TokenExpiredException("Token Expired")
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ServerFault)
        assertEquals(401, attribution.statusCode)
        assertTrue(attribution.diagnosticAction.contains("憑證"))
    }

    @Test
    fun testRateLimitExceptionClassifiedAsServerFault() {
        val ex = RateLimitException("Too Many Requests")
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ServerFault)
        assertEquals(429, attribution.statusCode)
    }

    @Test
    fun testNetworkExceptionClassifiedAsServerFault() {
        val ex = NetworkException("Connection timeout")
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ServerFault)
        assertEquals(502, attribution.statusCode)
    }

    @Test
    fun testPayloadExceptionClassifiedAsClientFault() {
        val ex = PayloadException("缺少必要欄位 prompt")
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ClientFault)
        assertTrue(attribution.isClientFault)
        assertEquals(422, attribution.statusCode)
    }

    @Test
    fun testSerializationExceptionClassifiedAsClientFault() {
        val ex = SerializationException("Field 'model' is required but missing")
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ClientFault)
        assertTrue(attribution.isClientFault)
        assertEquals(400, attribution.statusCode)
    }

    @Test
    fun testStreamingActivityTimeoutExceptionClassifiedAsServerFault504() {
        val ex = StreamingActivityTimeoutException()
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ServerFault)
        assertEquals(504, attribution.statusCode)
        assertTrue(attribution.summary.contains("30 秒"))
    }

    @Test
    fun testTimeoutCancellationExceptionClassifiedAsServerFault504() = kotlinx.coroutines.runBlocking {
        val ex = try {
            kotlinx.coroutines.withTimeout(1L) {
                kotlinx.coroutines.delay(50L)
            }
            null
        } catch (e: TimeoutCancellationException) {
            e
        }
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ServerFault)
        assertEquals(504, attribution.statusCode)
        assertTrue(attribution.summary.contains("逾時"))
    }

    @Test
    fun testCancellationExceptionClassifiedAsClientFault499() {
        val ex = CancellationException("Client cancelled request")
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ClientFault)
        assertEquals(499, attribution.statusCode)
        assertTrue(attribution.summary.contains("中斷連線"))
    }

    @Test
    fun testClosedChannelExceptionClassifiedAsClientFault499() {
        val ex = ClosedChannelException()
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ClientFault)
        assertEquals(499, attribution.statusCode)
    }

    @Test
    fun testBrokenPipeIOExceptionClassifiedAsClientFault499() {
        val ex = IOException("java.io.IOException: Broken pipe")
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ClientFault)
        assertEquals(499, attribution.statusCode)
    }

    @Test
    fun testConnectionResetClassifiedAsClientFault499() {
        val ex = IOException("Connection reset by peer")
        val attribution = ServerRequestErrorClassifier.classify(ex)

        assertTrue(attribution is ServerRequestErrorAttribution.ClientFault)
        assertEquals(499, attribution.statusCode)
    }

    @Test
    fun testManual400StatusCodeClassifiedAsClientFault() {
        val attribution = ServerRequestErrorClassifier.classify(
            throwable = null,
            manualStatusCode = 400,
            fallbackMessage = "缺少模型參數 (model)"
        )

        assertTrue(attribution is ServerRequestErrorAttribution.ClientFault)
        assertEquals(400, attribution.statusCode)
        assertEquals("缺少模型參數 (model)", attribution.summary)
    }

    @Test
    fun testManual499StatusCodeClassifiedAsClientFault() {
        val attribution = ServerRequestErrorClassifier.classify(
            throwable = null,
            manualStatusCode = 499
        )

        assertTrue(attribution is ServerRequestErrorAttribution.ClientFault)
        assertEquals(499, attribution.statusCode)
    }

    @Test
    fun testManual504StatusCodeClassifiedAsServerFault() {
        val attribution = ServerRequestErrorClassifier.classify(
            throwable = null,
            manualStatusCode = 504
        )

        assertTrue(attribution is ServerRequestErrorAttribution.ServerFault)
        assertEquals(504, attribution.statusCode)
    }

    @Test
    fun testManual500StatusCodeClassifiedAsServerFault() {
        val attribution = ServerRequestErrorClassifier.classify(
            throwable = RuntimeException("Crash"),
            manualStatusCode = 500
        )

        assertTrue(attribution is ServerRequestErrorAttribution.ServerFault)
        assertEquals(500, attribution.statusCode)
    }
}
