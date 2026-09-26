package com.example.presentation.common

import android.util.Log
import com.example.domain.models.AppError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object GlobalErrorHandler {
    private const val TAG = "GlobalErrorHandler"

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val _globalErrorFlow = MutableStateFlow<String?>(null)
    val globalError: StateFlow<String?> = _globalErrorFlow.asStateFlow()

    private val _appErrors = MutableSharedFlow<AppError>(extraBufferCapacity = 10)
    val appErrors: SharedFlow<AppError> = _appErrors.asSharedFlow()

    fun showGlobalError(message: String) {
        _globalErrorFlow.value = message
        _errorEvents.tryEmit(message)
    }

    fun clearGlobalError() {
        _globalErrorFlow.value = null
    }

    fun handleError(throwable: Throwable, userFriendlyMessage: String? = null) {
        val msg = userFriendlyMessage ?: throwable.localizedMessage ?: "An unexpected error occurred."
        Log.e(TAG, "Global error captured: $msg", throwable)
        showGlobalError(msg)
        if (throwable is AppError) {
            _appErrors.tryEmit(throwable)
        } else {
            _appErrors.tryEmit(AppError.Unknown(msg, throwable))
        }
    }

    fun handleAppError(error: AppError) {
        Log.e(TAG, "AppError captured: ${error.message}", error.cause)
        showGlobalError(error.message)
        _appErrors.tryEmit(error)
    }

    fun handleErrorMessage(message: String) {
        Log.e(TAG, "Global error message captured: $message")
        showGlobalError(message)
        _appErrors.tryEmit(AppError.Unknown(message))
    }
}
