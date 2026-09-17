package com.aira.assistant.network.api

interface ApiProvider {
    suspend fun generateResponse(query: String, context: String? = null): String
}
