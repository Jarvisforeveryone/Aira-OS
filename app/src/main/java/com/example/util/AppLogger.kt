package com.example.util

import android.util.Log

/**
 * Centralized logging and error tracking framework for Aira.
 * Safe for production with log-level controls and error recording.
 */
object AppLogger {
    private const val DEFAULT_TAG = "AiraCore"
    var isDebug: Boolean = true

    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isDebug) {
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

    fun recordException(tag: String = DEFAULT_TAG, throwable: Throwable) {
        Log.e(tag, "CRASH_PREVENTED: ${throwable.localizedMessage}", throwable)
    }
}
