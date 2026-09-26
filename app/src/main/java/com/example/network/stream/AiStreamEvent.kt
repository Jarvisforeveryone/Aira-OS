package com.example.network.stream

import com.example.network.api.ApiProviderType

/**
 * Granular events emitted during asynchronous AI streaming execution.
 */
sealed interface AiStreamEvent {
    data class Started(
        val provider: ApiProviderType,
        val model: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : AiStreamEvent

    data class Thinking(
        val thoughtChunk: String,
        val accumulatedThoughts: String
    ) : AiStreamEvent

    data class Chunk(
        val delta: String,
        val accumulatedText: String,
        val chunkIndex: Int
    ) : AiStreamEvent

    data class SentenceReady(
        val sentence: String,
        val sentenceIndex: Int
    ) : AiStreamEvent

    data class Completed(
        val fullText: String,
        val thoughts: String,
        val durationMs: Long,
        val provider: ApiProviderType,
        val model: String
    ) : AiStreamEvent

    data class Error(
        val error: Throwable,
        val message: String,
        val provider: ApiProviderType? = null
    ) : AiStreamEvent
}
