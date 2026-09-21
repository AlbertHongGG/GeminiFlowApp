package com.geminiflow.app

import com.geminiflow.app.data.auth.browser.AuthBrowserConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthBrowserConfigTest {

    @Test
    fun testCleanMobileChromeUa() {
        val dirtyUa = "Mozilla/5.0 (Linux; U; Android 14; Pixel 8 Build/UD1A.230803.041; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/128.0.6613.88 Mobile Safari/537.36"
        val cleanUa = AuthBrowserConfig.buildCleanMobileChromeUa(dirtyUa)

        assertFalse(cleanUa.contains("; wv"))
        assertFalse(cleanUa.contains("Version/4.0"))
        assertTrue(cleanUa.contains("Chrome/128.0.6613.88"))
        assertTrue(cleanUa.contains("Mobile Safari/537.36"))
    }

    @Test
    fun testInitialLoginUrl() {
        val url = AuthBrowserConfig.INITIAL_LOGIN_URL
        assertTrue(url.startsWith("https://accounts.google.com/ServiceLogin"))
        assertTrue(url.contains("service=mail"))
        assertTrue(url.contains("continue=https%3A%2F%2Fgemini.google.com%2Fapp"))
    }
}
