package com.geminiflow.app

import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.data.auth.WebKitCredentialStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebKitCredentialStoreTest {

    @Test
    fun testGetCookiesAndHasRequiredCredential() {
        val fakeStore = mutableMapOf<String, String>()
        fakeStore[GeminiConfig.GEMINI_BASE_URL] = "NID=123; SID=456"
        fakeStore[GeminiConfig.GEMINI_APP_URL] = "__Secure-1PSID=mock_psid_value; OSID=789"

        val store = WebKitCredentialStore(
            cookieGetter = { url -> fakeStore[url] }
        )
        val cookies = store.getCookies()

        assertEquals("mock_psid_value", cookies["__Secure-1PSID"])
        assertEquals("123", cookies["NID"])
        assertEquals("456", cookies["SID"])
        assertEquals("789", cookies["OSID"])
        assertTrue(store.hasRequiredCredential())
    }

    @Test
    fun testMissingRequiredCredentialReturnsFalse() {
        val fakeStore = mutableMapOf<String, String>()
        fakeStore[GeminiConfig.GEMINI_BASE_URL] = "NID=123; SID=456"

        val store = WebKitCredentialStore(
            cookieGetter = { url -> fakeStore[url] }
        )
        assertFalse(store.hasRequiredCredential())
    }

    @Test
    fun testSaveCookiesAndClearCredentials() {
        val saved = mutableListOf<Pair<String, String>>()
        var cleared = false

        val store = WebKitCredentialStore(
            cookieGetter = { null },
            cookieSetter = { url, cookie -> saved.add(url to cookie) },
            cookieClearer = { cleared = true }
        )

        store.saveCookies("foo=bar; baz=qux")
        assertEquals(4, saved.size) // 2 cookies * 2 urls (APP_URL and BASE_URL)

        store.clearCredentials()
        assertTrue(cleared)
    }
}
