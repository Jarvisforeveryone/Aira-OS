package com.example.data.models

/**
 * Standardized response container returned from AI brain and providers.
 */
data class AiResponseModel(
    val text: String,
    val provider: String,
    val model: String? = null,
    val isOfflineFallback: Boolean = false,
    val latencyMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
