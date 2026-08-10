package com.example.domain.models

data class VoiceCommand(
    val id: Long = 0,
    val phrase: String,
    val action: String,
    val isEnabled: Boolean = true
)
