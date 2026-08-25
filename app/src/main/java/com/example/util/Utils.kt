package com.example.util

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Consolidated utility functions for AIRA OS.
 */
object Utils {

    fun formatTimestamp(timestampMs: Long, pattern: String = "hh:mm a"): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestampMs))
        } catch (e: Exception) {
            ""
        }
    }

    fun formatDate(timestampMs: Long, pattern: String = "MMM dd, yyyy"): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestampMs))
        } catch (e: Exception) {
            ""
        }
    }

    fun cleanAiResponse(text: String): String {
        return text
            .replace(Regex("```[a-zA-Z]*"), "")
            .replace("```", "")
            .trim()
    }

    fun showToast(context: Context, message: String, isLong: Boolean = false) {
        Toast.makeText(context, message, if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}
