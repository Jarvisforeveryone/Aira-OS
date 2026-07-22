package com.example.data

import android.util.Log
import com.example.ui.AiraViewModel

object NetworkErrorHandler {
    suspend fun <T> safeApiCall(
        serviceName: String,
        block: suspend () -> T
    ): T? {
        return try {
            block()
        } catch (e: java.net.UnknownHostException) {
            Log.e("NetworkErrorHandler", "$serviceName: No internet connection", e)
            AiraViewModel.showGlobalError("No connection detected. Please check your internet.")
            null
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("NetworkErrorHandler", "$serviceName: Timeout", e)
            AiraViewModel.showGlobalError("The $serviceName timed out. Please try again.")
            null
        } catch (e: Exception) {
            Log.e("NetworkErrorHandler", "$serviceName: Unexpected failure", e)
            AiraViewModel.showGlobalError("$serviceName failed: ${e.localizedMessage ?: "Unknown error"}")
            null
        }
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
