package com.example.util

import java.util.regex.Pattern

/**
 * Security helper utilities for input validation, sanitization, and network safety.
 */
object SecurityUtils {

    private val SCRIPT_PATTERN = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE)
    private val HTML_PATTERN = Pattern.compile("<[^>]*>")

    /**
     * Sanitizes user input before passing to AI prompts or local databases.
     */
    fun sanitizeInput(input: String): String {
        var clean = input.trim()
        clean = SCRIPT_PATTERN.matcher(clean).replaceAll("")
        clean = HTML_PATTERN.matcher(clean).replaceAll("")
        return clean
    }

    /**
     * Validates hex color string safety.
     */
    fun isValidHexColor(hex: String): Boolean {
        val colorPattern = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")
        return colorPattern.matcher(hex.trim()).matches()
    }

    /**
     * Checks if a URL is secure HTTPS.
     */
    fun isSecureUrl(url: String): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }
}
