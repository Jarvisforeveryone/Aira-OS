package com.example.domain.models

/**
 * Unified sealed class hierarchy for all application errors in AIRA OS.
 */
sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    data class Network(
        override val message: String = "Network connection failed",
        val statusCode: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Authentication(
        override val message: String = "Authentication or API key missing/invalid",
        val provider: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class SpeechRecognition(
        override val message: String = "Speech recognition failed",
        val errorCode: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class TextToSpeech(
        override val message: String = "TTS synthesis failed",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Database(
        override val message: String = "Database operation failed",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class PermissionDenied(
        val permission: String,
        override val message: String = "Required permission was denied: $permission",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class DeviceControl(
        override val message: String = "Device action could not be executed",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Unknown(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
