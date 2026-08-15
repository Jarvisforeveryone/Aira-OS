package com.example.network.api

import android.content.Context

class ProviderManager(private val context: Context) {

    fun getProvider(providerName: String? = null): ApiProvider {
        val prefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
        val selected = providerName ?: prefs.getString("selected_ai_provider", "Gemini") ?: "Gemini"

        return when (selected.uppercase()) {
            "GROQ" -> GroqProvider(context)
            "CLAUDE" -> ClaudeProvider(context)
            "OPENAI" -> OpenAIProvider(context)
            "OPENROUTER" -> OpenRouterProvider(context)
            else -> GeminiProvider(context)
        }
    }

    fun setProvider(providerName: String) {
        val prefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_ai_provider", providerName).apply()
    }
}
