package com.example.presentation.common

import android.util.Log
import com.example.domain.models.AppError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object GlobalErrorHandler {
    private const val TAG = "GlobalErrorHandler"

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val _appErrors = MutableSharedFlow<AppError>(extraBufferCapacity = 10)
    val appErrors: SharedFlow<AppError> = _appErrors.asSharedFlow()

    fun handleError(throwable: Throwable, userFriendlyMessage: String? = null) {
        val msg = userFriendlyMessage ?: throwable.localizedMessage ?: "An unexpected error occurred."
        Log.e(TAG, "Global error captured: $msg", throwable)
        _errorEvents.tryEmit(msg)
        if (throwable is AppError) {
            _appErrors.tryEmit(throwable)
        } else {
            _appErrors.tryEmit(AppError.Unknown(msg, throwable))
        }
    }

    fun handleAppError(error: AppError) {
        Log.e(TAG, "AppError captured: ${error.message}", error.cause)
        _errorEvents.tryEmit(error.message)
        _appErrors.tryEmit(error)
    }

    fun handleErrorMessage(message: String) {
        Log.e(TAG, "Global error message captured: $message")
        _errorEvents.tryEmit(message)
        _appErrors.tryEmit(AppError.Unknown(message))
    }
}
