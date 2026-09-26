package com.example.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor that automatically retries failed network requests
 * using exponential backoff for transient server errors (e.g. HTTP 429, 502, 503, 504).
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 4000L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null
        var attempt = 0
        var currentDelay = initialDelayMs

        while (attempt < maxRetries) {
            try {
                response?.close()
                response = chain.proceed(request)
                if (response.isSuccessful || !isRetryableStatusCode(response.code)) {
                    return response
                }
            } catch (e: IOException) {
                exception = e
            }

            attempt++
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(currentDelay)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("Request retry interrupted", ie)
                }
                currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
            }
        }

        response?.let { return it }
        throw exception ?: IOException("Request failed after $maxRetries retry attempts")
    }

    private fun isRetryableStatusCode(code: Int): Boolean {
        return code == 429 || code == 502 || code == 503 || code == 504
    }
}
