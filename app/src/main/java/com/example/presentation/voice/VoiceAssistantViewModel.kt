package com.example.presentation.voice

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.ui.components.OrbState
import com.example.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SttEngineType {
    AUTO,
    GOOGLE_STT,
    VOSK_OFFLINE
}

enum class TtsEngineType {
    AUTO,
    GOOGLE_TTS,
    PIPER_OFFLINE
}

data class VoiceAssistantUiState(
    val orbState: OrbState = OrbState.IDLE,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isProcessing: Boolean = false,
    val audioAmplitude: Float = 0f,
    val currentStatus: String = "Tap mic to speak",
    val selectedSttEngine: SttEngineType = SttEngineType.AUTO,
    val selectedTtsEngine: TtsEngineType = TtsEngineType.AUTO,
    val isOfflineTtsEnabled: Boolean = false,
    val isOfflineSttEnabled: Boolean = false,
    val piperSpeed: Float = 1.0f
)

/**
 * Feature ViewModel for Voice Recognition (STT), Speech Synthesis (TTS),
 * Voice Orb state orchestration, and engine configurations.
 */
class VoiceAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VoiceAssistantUiState())
    val uiState: StateFlow<VoiceAssistantUiState> = _uiState.asStateFlow()

    private val prefs = application.getSharedPreferences("aira_voice_prefs", Context.MODE_PRIVATE)

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        val sttPref = prefs.getString("selected_stt_engine", SttEngineType.AUTO.name) ?: SttEngineType.AUTO.name
        val ttsPref = prefs.getString("selected_tts_engine", TtsEngineType.AUTO.name) ?: TtsEngineType.AUTO.name
        val offlineTts = prefs.getBoolean("offline_tts_enabled", false)
        val speed = prefs.getFloat("piper_speed", 1.0f)

        _uiState.value = _uiState.value.copy(
            selectedSttEngine = runCatching { SttEngineType.valueOf(sttPref) }.getOrDefault(SttEngineType.AUTO),
            selectedTtsEngine = runCatching { TtsEngineType.valueOf(ttsPref) }.getOrDefault(TtsEngineType.AUTO),
            isOfflineTtsEnabled = offlineTts,
            piperSpeed = speed
        )
    }

    fun setOrbState(state: OrbState) {
        _uiState.value = _uiState.value.copy(orbState = state)
    }

    fun setListening(listening: Boolean) {
        _uiState.value = _uiState.value.copy(
            isListening = listening,
            orbState = if (listening) OrbState.LISTENING else if (_uiState.value.isSpeaking) OrbState.SPEAKING else OrbState.IDLE,
            currentStatus = if (listening) "Listening..." else "Tap mic to speak"
        )
    }

    fun setSpeaking(speaking: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSpeaking = speaking,
            orbState = if (speaking) OrbState.SPEAKING else if (_uiState.value.isListening) OrbState.LISTENING else OrbState.IDLE,
            currentStatus = if (speaking) "Speaking..." else "Tap mic to speak"
        )
    }

    fun setProcessing(processing: Boolean) {
        _uiState.value = _uiState.value.copy(
            isProcessing = processing,
            orbState = if (processing) OrbState.PROCESSING else if (_uiState.value.isListening) OrbState.LISTENING else OrbState.IDLE,
            currentStatus = if (processing) "Processing..." else "Tap mic to speak"
        )
    }

    fun updateAmplitude(amplitude: Float) {
        _uiState.value = _uiState.value.copy(audioAmplitude = amplitude)
    }

    fun setSelectedSttEngine(engine: SttEngineType) {
        _uiState.value = _uiState.value.copy(selectedSttEngine = engine)
        prefs.edit().putString("selected_stt_engine", engine.name).apply()
        Logger.d("VoiceAssistantViewModel", "Selected STT Engine: $engine")
    }

    fun setSelectedTtsEngine(engine: TtsEngineType) {
        _uiState.value = _uiState.value.copy(selectedTtsEngine = engine)
        prefs.edit().putString("selected_tts_engine", engine.name).apply()
        Logger.d("VoiceAssistantViewModel", "Selected TTS Engine: $engine")
    }

    fun togglePiperTtsOffline(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isOfflineTtsEnabled = enabled)
        prefs.edit().putBoolean("offline_tts_enabled", enabled).apply()
        Logger.d("VoiceAssistantViewModel", "Piper TTS Offline enabled: $enabled")
    }

    fun setPiperSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(piperSpeed = speed)
        prefs.edit().putFloat("piper_speed", speed).apply()
        Logger.d("VoiceAssistantViewModel", "Piper TTS Speed set: $speed")
    }
}
