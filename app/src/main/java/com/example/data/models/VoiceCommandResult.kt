package com.example.data.models

/**
 * Standardized result for parsed and executed voice commands.
 */
data class VoiceCommandResult(
    val originalQuery: String,
    val matchedAction: String?,
    val executionSuccess: Boolean,
    val responseSpeech: String,
    val requiresTts: Boolean = true
)
