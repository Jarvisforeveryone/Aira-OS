package com.example.domain.models

data class ChatMessage(
    val id: Long = 0,
    val sender: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOffline: Boolean = false
)
