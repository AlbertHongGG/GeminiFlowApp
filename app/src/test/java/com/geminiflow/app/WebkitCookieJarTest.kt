package com.geminiflow.app

import com.geminiflow.app.data.network.WebkitCookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebkitCookieJarTest {

    @Test
    fun testDomainIsolation_Hop0HasNoCookies() {
        val cookieMap = mapOf(
            "https://work.fife.usercontent.google.com" to "__Secure-1PSID=test_psid; SAPISID=test_sapisid",
            "https://gemini.google.com" to "__Secure-1PSID=test_psid"
        )

        val jar = WebkitCookieJar(
            cookieProvider = { url ->
                cookieMap.entries.firstOrNull { url.startsWith(it.key) }?.value
            }
        )

        val hop0Url = "https://lh3.googleusercontent.com/gg-dl/abc123token".toHttpUrl()
        val hop0Cookies = jar.loadForRequest(hop0Url)

        // Hop 0 網域為 googleusercontent.com，未匹配到任何 Cookie，必須為空以確保 302 跳轉成功
        assertTrue(hop0Cookies.isEmpty())
    }

    @Test
    fun testAuthenticationHop_Hop1ReceivesGoogleSessionCookies() {
        val expectedPsid = "test_psid_val"
        val expectedSapisid = "test_sapisid_val"
        val cookieHeader = "__Secure-1PSID=$expectedPsid; SAPISID=$expectedSapisid"

        val jar = WebkitCookieJar(
            cookieProvider = { url ->
                if (url.contains(".google.com")) cookieHeader else null
            }
        )

        val hop1Url = "https://work.fife.usercontent.google.com/rd-gg-dl/verified_token".toHttpUrl()
        val hop1Cookies = jar.loadForRequest(hop1Url)

        assertEquals(2, hop1Cookies.size)
        val psidCookie = hop1Cookies.first { it.name == "__Secure-1PSID" }
        assertEquals(expectedPsid, psidCookie.value)
        assertEquals("work.fife.usercontent.google.com", psidCookie.domain)
    }

    @Test
    fun testSaveFromResponse_InvokesSaver() {
        val saved = mutableListOf<Pair<String, String>>()
        val jar = WebkitCookieJar(
            cookieProvider = { null },
            cookieSaver = { url, cookieStr -> saved.add(url to cookieStr) }
        )

        val url = "https://gemini.google.com".toHttpUrl()
        val testCookie = Cookie.Builder()
            .name("SESSION_ID")
            .value("12345")
            .domain("gemini.google.com")
            .build()

        jar.saveFromResponse(url, listOf(testCookie))
        assertEquals(1, saved.size)
        assertEquals("https://gemini.google.com/", saved[0].first)
        assertTrue(saved[0].second.contains("SESSION_ID=12345"))
    }
}
