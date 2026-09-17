package com.aira.assistant.core.logging

import android.util.Log

/**
 * Centralized logger for AIRA OS.
 * Provides unified debug/info/warning/error logging with standardized tags,
 * production gating, and automatic sensitive data redaction.
 */
object Logger {

    private const val DEFAULT_TAG = "AIRA_OS"
    var isDebugEnabled: Boolean = com.aira.assistant.BuildConfig.DEBUG

    // Common sensitive patterns: API keys, bearer tokens, passwords
    private val SENSITIVE_PATTERNS = listOf(
        Regex("(?i)(key|token|secret|password|bearer|auth|authorization)\\s*[:=]\\s*([\"']?)([a-zA-Z0-9_\\-\\.~]{6,})\\2"),
        Regex("sk-[a-zA-Z0-9]{20,}"),
        Regex("AIza[0-9A-Za-z-_]{35}"),
        Regex("gsk_[a-zA-Z0-9]{20,}")
    )

    fun sanitize(message: String): String {
        var sanitized = message
        for (pattern in SENSITIVE_PATTERNS) {
            sanitized = sanitized.replace(pattern) { matchResult ->
                val full = matchResult.value
                if (full.contains(":") || full.contains("=")) {
                    val parts = full.split(Regex("[:=]"), 2)
                    "${parts[0]}=[REDACTED]"
                } else {
                    "[REDACTED_KEY]"
                }
            }
        }
        return sanitized
    }

    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugEnabled) {
            Log.d(tag, sanitize(message))
        }
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugEnabled) {
            Log.i(tag, sanitize(message))
        }
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        val safeMessage = sanitize(message)
        if (throwable != null) {
            Log.w(tag, safeMessage, throwable)
        } else {
            Log.w(tag, safeMessage)
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        val safeMessage = sanitize(message)
        if (throwable != null) {
            Log.e(tag, safeMessage, throwable)
        } else {
            Log.e(tag, safeMessage)
        }
    }
}
