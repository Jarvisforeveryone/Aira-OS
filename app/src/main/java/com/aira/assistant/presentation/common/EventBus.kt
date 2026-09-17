package com.aira.assistant.presentation.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class AiraEvent {
    data class SpeakRequested(val text: String, val flush: Boolean = true) : AiraEvent()
    data object StopSpeakingRequested : AiraEvent()
    data class StartListeningRequested(val prompt: String = "") : AiraEvent()
    data object StopListeningRequested : AiraEvent()
    data class ChatMessageSubmitted(val query: String, val isVoice: Boolean = false) : AiraEvent()
    data class StatusUpdated(val status: String) : AiraEvent()
    data class ShizukuTaskRequested(val command: String) : AiraEvent()
    data class WakeWordDetected(val phrase: String) : AiraEvent()
    data class ToastRequested(val message: String) : AiraEvent()
}

object EventBus {
    private val _events = MutableSharedFlow<AiraEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AiraEvent> = _events.asSharedFlow()

    fun emit(event: AiraEvent) {
        _events.tryEmit(event)
    }

    suspend fun send(event: AiraEvent) {
        _events.emit(event)
    }
}
