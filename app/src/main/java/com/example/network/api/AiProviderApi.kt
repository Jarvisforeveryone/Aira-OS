package com.example.network.api

/**
 * Unified interface for AI model providers in AIRA OS.
 * Provides standard synchronous and context-aware response generation.
 */
interface AiProviderApi : ApiProvider {
    val providerType: ApiProviderType

    suspend fun generateAiText(
        prompt: String,
        systemInstruction: String? = null,
        temperature: Double? = null,
        enableThinking: Boolean = false
    ): Result<String>

    override suspend fun generateResponse(query: String, context: String?): String {
        return generateAiText(prompt = query, systemInstruction = context).getOrDefault("")
    }
}
