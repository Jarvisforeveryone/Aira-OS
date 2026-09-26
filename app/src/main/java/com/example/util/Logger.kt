package com.example.util

import android.util.Log

/**
 * Centralized logger for AIRA OS.
 * Provides unified debug/info/warning/error logging with standardized tags.
 */
object Logger {

    private const val DEFAULT_TAG = "AIRA_OS"
    var isDebugEnabled: Boolean = true

    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugEnabled) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, message)
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
