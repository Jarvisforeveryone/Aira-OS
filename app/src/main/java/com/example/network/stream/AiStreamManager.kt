package com.example.network.stream

import android.content.Context
import android.util.Log
import com.example.data.ApiManager
import com.example.network.api.ApiProviderType
import com.example.network.api.GeminiClient
import com.example.network.api.GroqClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-performance asynchronous AI streaming execution manager using Kotlin Flow and StateFlow.
 * Handles token-by-token streaming, non-blocking UI state updates, sentence pipelining for TTS,
 * and robust provider failover.
 */
class AiStreamManager private constructor(private val context: Context) {

    private val apiManager = ApiManager.getInstance(context)
    private val geminiClient = GeminiClient()
    private val groqClient = GroqClient()

    private val _streamState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val streamState: StateFlow<AiStreamState> = _streamState.asStateFlow()

    private val _liveStreamingText = MutableStateFlow("")
    val liveStreamingText: StateFlow<String> = _liveStreamingText.asStateFlow()

    private val _activeThoughts = MutableStateFlow("")
    val activeThoughts: StateFlow<String> = _activeThoughts.asStateFlow()

    private val _isStreamingActive = MutableStateFlow(false)
    val isStreamingActive: StateFlow<Boolean> = _isStreamingActive.asStateFlow()

    private var currentStreamJob: Job? = null

    /**
     * Executes an AI prompt as a reactive Kotlin Flow, emitting AiStreamEvents in real time.
     * Also updates internal StateFlows for direct UI observation without blocking the UI thread.
     */
    fun streamPrompt(
        prompt: String,
        systemInstruction: String? = null,
        onSentenceReady: ((String) -> Unit)? = null
    ): Flow<AiStreamEvent> = flow {
        val startTime = System.currentTimeMillis()
        val primary = apiManager.getActiveProvider()
        val allProviders = ApiProviderType.values().toList()
        val providerOrder = listOf(primary) + allProviders.filter { it != primary }

        val taskType = com.example.utils.TaskDetector.detectTaskType(prompt)
        val chunker = SentenceChunker()
        chunker.reset()

        var streamSucceeded = false
        var lastError: Throwable? = null

        for (provider in providerOrder) {
            val keys = apiManager.getKeyForProvider(provider).let { legacyKey ->
                if (legacyKey.isNotBlank()) listOf(legacyKey) else emptyList()
            }
            if (keys.isEmpty()) continue

            val apiKey = keys.first()
            val model = apiManager.getSelectedModel(provider)

            emit(AiStreamEvent.Started(provider = provider, model = model, timestamp = startTime))
            _streamState.value = AiStreamState.Connecting(provider = provider, model = model, startTimeMs = startTime)
            _isStreamingActive.value = true

            val fullTextBuilder = StringBuilder()
            val thoughtBuilder = StringBuilder()
            var chunkIndex = 0
            var sentenceIndex = 0

            try {
                when (provider) {
                    ApiProviderType.GEMINI -> {
                        val isThinking = model.contains("pro") || model.contains("thinking")
                        geminiClient.streamGenerateContent(
                            apiKey = apiKey,
                            model = model,
                            prompt = prompt,
                            systemInstruction = systemInstruction,
                            enableThinkingMode = isThinking
                        ).collect { chunk ->
                            if (chunk.isThought) {
                                thoughtBuilder.append(chunk.text)
                                _activeThoughts.value = thoughtBuilder.toString()
                                emit(AiStreamEvent.Thinking(chunk.text, thoughtBuilder.toString()))
                                _streamState.value = AiStreamState.Thinking(
                                    provider = provider,
                                    thoughtText = thoughtBuilder.toString(),
                                    elapsedTimeMs = System.currentTimeMillis() - startTime
                                )
                            } else {
                                fullTextBuilder.append(chunk.text)
                                val currentAccumulated = fullTextBuilder.toString()
                                _liveStreamingText.value = currentAccumulated
                                chunkIndex++

                                emit(AiStreamEvent.Chunk(chunk.text, currentAccumulated, chunkIndex))

                                // Check for completed sentences for instant TTS dispatch
                                val sentences = chunker.append(chunk.text)
                                for (s in sentences) {
                                    sentenceIndex++
                                    emit(AiStreamEvent.SentenceReady(s, sentenceIndex))
                                    onSentenceReady?.invoke(s)
                                }

                                _streamState.value = AiStreamState.Streaming(
                                    provider = provider,
                                    model = model,
                                    currentText = currentAccumulated,
                                    thoughtText = thoughtBuilder.toString(),
                                    chunkCount = chunkIndex,
                                    sentenceCount = sentenceIndex,
                                    elapsedTimeMs = System.currentTimeMillis() - startTime
                                )
                            }
                        }
                    }
                    ApiProviderType.GROQ -> {
                        groqClient.streamGenerateContent(
                            apiKey = apiKey,
                            model = model,
                            prompt = prompt,
                            systemInstruction = systemInstruction
                        ).collect { delta ->
                            fullTextBuilder.append(delta)
                            val currentAccumulated = fullTextBuilder.toString()
                            _liveStreamingText.value = currentAccumulated
                            chunkIndex++

                            emit(AiStreamEvent.Chunk(delta, currentAccumulated, chunkIndex))

                            val sentences = chunker.append(delta)
                            for (s in sentences) {
                                sentenceIndex++
                                emit(AiStreamEvent.SentenceReady(s, sentenceIndex))
                                onSentenceReady?.invoke(s)
                            }

                            _streamState.value = AiStreamState.Streaming(
                                provider = provider,
                                model = model,
                                currentText = currentAccumulated,
                                thoughtText = thoughtBuilder.toString(),
                                chunkCount = chunkIndex,
                                sentenceCount = sentenceIndex,
                                elapsedTimeMs = System.currentTimeMillis() - startTime
                            )
                        }
                    }
                    else -> {
                        // Fallback for providers without direct SSE implementation: run via standard call and emit
                        val result = apiManager.queryAi(prompt, systemInstruction)
                        if (result.isSuccess) {
                            val text = result.getOrNull()?.second ?: ""
                            fullTextBuilder.append(text)
                            _liveStreamingText.value = text
                            emit(AiStreamEvent.Chunk(text, text, 1))
                            onSentenceReady?.invoke(text)
                        } else {
                            throw result.exceptionOrNull() ?: Exception("Provider call failed")
                        }
                    }
                }

                // Flush remaining sentence
                val flushed = chunker.flush()
                if (!flushed.isNullOrBlank()) {
                    sentenceIndex++
                    emit(AiStreamEvent.SentenceReady(flushed, sentenceIndex))
                    onSentenceReady?.invoke(flushed)
                }

                val finalFullText = fullTextBuilder.toString().trim()
                if (finalFullText.isNotEmpty()) {
                    val duration = System.currentTimeMillis() - startTime
                    _streamState.value = AiStreamState.Success(
                        fullText = finalFullText,
                        thoughtText = thoughtBuilder.toString(),
                        provider = provider,
                        model = model,
                        totalDurationMs = duration
                    )
                    emit(
                        AiStreamEvent.Completed(
                            fullText = finalFullText,
                            thoughts = thoughtBuilder.toString(),
                            durationMs = duration,
                            provider = provider,
                            model = model
                        )
                    )
                    streamSucceeded = true
                    break
                }
            } catch (e: CancellationException) {
                Log.d("AiStreamManager", "Stream cancelled by user/client")
                throw e
            } catch (e: Throwable) {
                Log.w("AiStreamManager", "Stream failed on ${provider.displayName}: ${e.message}. Trying fallback...")
                lastError = e
            }
        }

        if (!streamSucceeded) {
            val errorMsg = lastError?.message ?: "All stream providers failed or no API keys configured"
            _streamState.value = AiStreamState.Failed(
                errorMessage = errorMsg,
                error = lastError,
                provider = primary,
                canRetry = true
            )
            emit(AiStreamEvent.Error(lastError ?: Exception(errorMsg), errorMsg, primary))
        }
    }.onStart {
        _isStreamingActive.value = true
        _liveStreamingText.value = ""
        _activeThoughts.value = ""
    }.onCompletion {
        _isStreamingActive.value = false
    }.flowOn(Dispatchers.IO)

    /**
     * Cancels any currently executing stream job and resets state.
     */
    fun cancelCurrentStream() {
        currentStreamJob?.cancel()
        currentStreamJob = null
        _isStreamingActive.value = false
        _streamState.value = AiStreamState.Idle
    }

    fun resetState() {
        _streamState.value = AiStreamState.Idle
        _liveStreamingText.value = ""
        _activeThoughts.value = ""
        _isStreamingActive.value = false
    }

    companion object {
        @Volatile
        private var INSTANCE: AiStreamManager? = null

        fun getInstance(context: Context): AiStreamManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AiStreamManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
