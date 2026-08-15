package com.example.network.api

interface ApiProvider {
    suspend fun generateResponse(query: String, context: String? = null): String
}
