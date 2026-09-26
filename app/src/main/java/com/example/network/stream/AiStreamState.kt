package com.example.network.stream

import com.example.network.api.ApiProviderType

/**
 * State representation for UI observables and StateFlow consumers.
 */
sealed interface AiStreamState {
    object Idle : AiStreamState

    data class Connecting(
        val provider: ApiProviderType,
        val model: String,
        val startTimeMs: Long = System.currentTimeMillis()
    ) : AiStreamState

    data class Thinking(
        val provider: ApiProviderType,
        val thoughtText: String,
        val elapsedTimeMs: Long
    ) : AiStreamState

    data class Streaming(
        val provider: ApiProviderType,
        val model: String,
        val currentText: String,
        val thoughtText: String,
        val chunkCount: Int,
        val sentenceCount: Int,
        val elapsedTimeMs: Long
    ) : AiStreamState

    data class Success(
        val fullText: String,
        val thoughtText: String,
        val provider: ApiProviderType,
        val model: String,
        val totalDurationMs: Long
    ) : AiStreamState

    data class Failed(
        val errorMessage: String,
        val error: Throwable?,
        val provider: ApiProviderType?,
        val canRetry: Boolean
    ) : AiStreamState
}
