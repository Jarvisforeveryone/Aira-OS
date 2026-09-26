package com.example.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Global extension functions for AIRA OS.
 */

fun Context.toast(message: String, isLong: Boolean = false) {
    Utils.showToast(this, message, isLong)
}

fun String.truncate(maxLength: Int): String {
    return if (this.length > maxLength) this.take(maxLength) + "..." else this
}

fun Long.toFormattedTime(): String {
    return Utils.formatTimestamp(this)
}

fun Long.toFormattedDate(): String {
    return Utils.formatDate(this)
}

fun <T> Flow<T>.catchAppError(action: suspend (Throwable) -> Unit): Flow<T> {
    return this.catch { e ->
        Logger.e("FlowExtension", "Unhandled error in flow collection", e)
        action(e)
    }
}
