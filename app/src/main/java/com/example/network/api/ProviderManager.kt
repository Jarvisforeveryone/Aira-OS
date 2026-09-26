package com.example.network.api

import android.content.Context
import com.example.data.ChatKeyManager

class ProviderManager(private val context: Context) {

    fun getProvider(providerName: String? = null): ApiProvider {
        val keyManager = ChatKeyManager.getInstance(context)
        val selected = providerName ?: keyManager.getSelectedProvider()

        return when (selected.uppercase()) {
            "GROQ" -> GroqProvider(context)
            "CLAUDE" -> ClaudeProvider(context)
            "OPENAI" -> OpenAIProvider(context)
            "OPENROUTER" -> OpenRouterProvider(context)
            else -> GeminiProvider(context) // Gemini is forced primary default
        }
    }

    /**
     * Returns the complete prioritized fallback chain:
     * Primary Selected -> Gemini -> Groq -> OpenAI -> Claude -> OpenRouter
     */
    fun getFallbackChain(primary: String? = null): List<ApiProvider> {
        val keyManager = ChatKeyManager.getInstance(context)
        val selected = (primary ?: keyManager.getSelectedProvider()).uppercase()

        val allProviders = linkedMapOf<String, ApiProvider>(
            "GEMINI" to GeminiProvider(context),
            "GROQ" to GroqProvider(context),
            "OPENAI" to OpenAIProvider(context),
            "CLAUDE" to ClaudeProvider(context),
            "OPENROUTER" to OpenRouterProvider(context)
        )

        val chain = mutableListOf<ApiProvider>()
        allProviders[selected]?.let { chain.add(it) }
        allProviders.forEach { (name, provider) ->
            if (name != selected) {
                chain.add(provider)
            }
        }
        return chain
    }

    fun setProvider(providerName: String) {
        ChatKeyManager.getInstance(context).setSelectedProvider(providerName)
        val prefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_ai_provider", providerName).putString("active_api_provider", providerName).apply()
    }
}
