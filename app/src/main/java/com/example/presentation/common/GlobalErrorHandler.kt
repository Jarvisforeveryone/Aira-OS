package com.example.presentation.common

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object GlobalErrorHandler {
    private const val TAG = "GlobalErrorHandler"

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    fun handleError(throwable: Throwable, userFriendlyMessage: String? = null) {
        val msg = userFriendlyMessage ?: throwable.localizedMessage ?: "An unexpected error occurred."
        Log.e(TAG, "Global error captured: $msg", throwable)
        _errorEvents.tryEmit(msg)
    }

    fun handleErrorMessage(message: String) {
        Log.e(TAG, "Global error message captured: $message")
        _errorEvents.tryEmit(message)
    }
}
