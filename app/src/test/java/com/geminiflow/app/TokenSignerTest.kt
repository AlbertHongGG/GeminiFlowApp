package com.geminiflow.app

import com.geminiflow.app.data.attestation.NoOpTokenSigner
import com.geminiflow.app.domain.attestation.TokenSigner
import com.geminiflow.app.domain.model.attestation.BotguardToken
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom

class TokenSignerTest {

    class FakeTokenSigner : TokenSigner {
        var lastSignedPrompt: String? = null
        var lastSignedConvId: String? = null

        override suspend fun sign(effectivePrompt: String, conversationId: String?): BotguardToken {
            lastSignedPrompt = effectivePrompt
            lastSignedConvId = conversationId

            val saltHex = "a8f7c9e01b2d4f5e6a7c8b9d0e1f2a3b"
            val toHash = (effectivePrompt + saltHex).toByteArray(Charsets.UTF_8)
            val qh = MessageDigest.getInstance("SHA-256").digest(toHash).joinToString("") { "%02x".format(it) }

            return BotguardToken(
                token = "!FAKE_BOTGUARD_TOKEN_$qh",
                salt = saltHex
            )
        }
    }

    @Test
    fun testNoOpTokenSigner_returnsNull() = runBlocking {
        val signer = NoOpTokenSigner()
        val token = signer.sign("test prompt", null)
        assertNull(token)
    }

    @Test
    fun testFakeTokenSigner_contentBoundHash() = runBlocking {
        val signer = FakeTokenSigner()
        val prompt = "寫一個 QuickSort 演算法"
        val convId = "c_12345"

        val token = signer.sign(prompt, convId)
        assertNotNull(token)
        assertTrue(token!!.token.startsWith("!"))
        assertEquals("a8f7c9e01b2d4f5e6a7c8b9d0e1f2a3b", token.salt)
        assertEquals(prompt, signer.lastSignedPrompt)
        assertEquals(convId, signer.lastSignedConvId)

        // Verify SHA-256 query hash binding
        val expectedHash = MessageDigest.getInstance("SHA-256")
            .digest((prompt + token.salt).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        assertTrue(token.token.contains(expectedHash))
    }

    @Test
    fun testSaltGeneration_32HexCharacters() {
        val random = SecureRandom()
        val saltBytes = ByteArray(16).also { random.nextBytes(it) }
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }

        assertEquals(32, saltHex.length)
        assertTrue(saltHex.matches(Regex("^[0-9a-f]{32}$")))
    }
}
