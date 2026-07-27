package com.example

import com.example.data.Memory
import com.example.data.NetworkErrorHandler
import com.example.util.AnalyticsManager
import com.example.util.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Production local unit tests for Aira application logic and utilities.
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSecurityUtilsSanitization() {
        val unsafeInput = "<script>alert('xss')</script>Hello World <b>Test</b>"
        val clean = SecurityUtils.sanitizeInput(unsafeInput)
        assertEquals("Hello World Test", clean)
    }

    @Test
    fun testSecurityUtilsHexColorValidation() {
        assertTrue(SecurityUtils.isValidHexColor("#2563EB"))
        assertTrue(SecurityUtils.isValidHexColor("#FF2563EB"))
        assertFalse(SecurityUtils.isValidHexColor("invalid-color"))
        assertFalse(SecurityUtils.isValidHexColor("#123"))
    }

    @Test
    fun testSecurityUtilsSecureUrl() {
        assertTrue(SecurityUtils.isSecureUrl("https://generativelanguage.googleapis.com"))
        assertFalse(SecurityUtils.isSecureUrl("http://insecure-site.com"))
    }

    @Test
    fun testMemoryModel() {
        val memory = Memory(
            id = 1,
            factText = "User lives in San Francisco",
            source = "voice",
            createdAt = 1000L
        )
        assertEquals(1L, memory.id)
        assertEquals("User lives in San Francisco", memory.factText)
        assertEquals("voice", memory.source)
    }

    @Test
    fun testAnalyticsManagerLogging() {
        AnalyticsManager.logScreenView("HomeScreen")
        val events = AnalyticsManager.events.value
        assertTrue(events.any { it.eventName == "screen_view" && it.params["screen_name"] == "HomeScreen" })
    }

    @Test
    fun testNetworkErrorHandlerFormatting() {
        val formatted = NetworkErrorHandler.formatError("Gemini API", Exception("Timeout"))
        assertTrue(formatted.contains("Gemini API"))
        assertTrue(formatted.contains("Timeout"))
    }
}
