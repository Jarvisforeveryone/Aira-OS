package com.aira.assistant.presentation.components

import java.util.UUID

enum class SttState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

enum class TtsEngine {
    AUTO,
    GOOGLE_TTS,
    PIPER_OFFLINE
}

enum class SttEngine {
    AUTO,
    VOSK_OFFLINE
}

enum class VoiceAssistantState {
    READY,
    DOWNLOADING,
    NOT_DOWNLOADED
}

data class VoiceCommandLog(
    val id: String = UUID.randomUUID().toString(),
    val command: String,
    val matchedTrigger: String?,
    val timestamp: String,
    val status: String,
    val details: String
)

data class OpenMeteoWeatherData(
    val locationName: String = "San Francisco",
    val country: String = "",
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val temperatureC: Double = 17.0,
    val windSpeedKmH: Double = 10.0,
    val windDirectionDeg: Int = 0,
    val weatherCode: Int = 0,
    val conditionDescription: String = "Clear Sky",
    val isDaytime: Boolean = true,
    val isGpsLocation: Boolean = false,
    val formattedText: String = "San Francisco: 17°C, Clear Sky"
)
