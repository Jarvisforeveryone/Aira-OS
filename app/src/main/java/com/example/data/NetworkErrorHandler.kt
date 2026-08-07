package com.example.data

import android.util.Log
import com.example.ui.AiraViewModel
import kotlinx.coroutines.delay

object NetworkErrorHandler {

    suspend fun <T> safeApiCall(
        serviceName: String,
        block: suspend () -> T
    ): T? {
        return safeApiCallWithRetry(serviceName = serviceName, maxRetries = 3, block = block)
    }

    suspend fun <T> safeApiCallWithRetry(
        serviceName: String,
        maxRetries: Int = 3,
        initialDelayMs: Long = 500L,
        maxDelayMs: Long = 4000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T? {
        var currentDelay = initialDelayMs
        for (attempt in 0..maxRetries) {
            try {
                return block()
            } catch (e: java.net.UnknownHostException) {
                Log.w("NetworkErrorHandler", "$serviceName: No internet connection (Attempt ${attempt + 1}/${maxRetries + 1})")
                if (attempt == maxRetries) {
                    AiraViewModel.showGlobalError("No connection detected. Please check your internet.")
                    return null
                }
            } catch (e: java.net.SocketTimeoutException) {
                Log.w("NetworkErrorHandler", "$serviceName: Timeout (Attempt ${attempt + 1}/${maxRetries + 1})")
                if (attempt == maxRetries) {
                    AiraViewModel.showGlobalError("The $serviceName timed out. Please try again.")
                    return null
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val isTransientOrRateLimit = msg.contains("429") || msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504") || e is java.io.IOException
                Log.w("NetworkErrorHandler", "$serviceName: Failure - ${e.javaClass.simpleName}: $msg (Attempt ${attempt + 1}/${maxRetries + 1})")
                if (attempt == maxRetries || !isTransientOrRateLimit) {
                    AiraViewModel.showGlobalError("$serviceName failed: ${e.localizedMessage ?: "Unknown error"}")
                    return null
                }
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
        return null
    }

    fun safeVoiceCall(
        engineName: String,
        block: () -> Unit
    ) {
        try {
            block()
        } catch (e: Exception) {
            Log.e("NetworkErrorHandler", "$engineName: Exception", e)
            AiraViewModel.showGlobalError("$engineName failure: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
