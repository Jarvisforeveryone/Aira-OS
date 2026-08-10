package com.example.data

import android.content.Context
import android.util.Log
import com.example.network.api.*

class ApiManager private constructor(private val context: Context) {

    private val keyManager = ChatKeyManager.getInstance(context)

    private val geminiClient = GeminiClient()
    private val groqClient = GroqClient()
    private val openAIClient = OpenAIClient()
    private val claudeClient = ClaudeClient()
    private val openRouterClient = OpenRouterClient()
    private val mistralClient = MistralClient()
    private val cohereClient = CohereClient()
    private val huggingFaceClient = HuggingFaceClient()

    fun getActiveProvider(): ApiProvider {
        val name = keyManager.getSelectedProvider()
        return try {
            ApiProvider.valueOf(name.uppercase())
        } catch (e: Exception) {
            ApiProvider.GEMINI
        }
    }

    fun setActiveProvider(provider: ApiProvider) {
        keyManager.setSelectedProvider(provider.name)
    }

    fun getSelectedModel(provider: ApiProvider): String {
        val defaultModel = ApiDefaults.modelsMap[provider]?.firstOrNull() ?: ""
        return keyManager.getSelectedModel(provider.name, defaultModel)
    }

    fun setSelectedModel(provider: ApiProvider, model: String) {
        keyManager.setSelectedModel(provider.name, model)
    }

    fun getKeyForProvider(provider: ApiProvider): String {
        return when (provider) {
            ApiProvider.GEMINI -> keyManager.getNextKey() ?: ""
            ApiProvider.GROQ -> keyManager.getGroqKey()
            ApiProvider.OPENAI -> keyManager.getOpenAiKey()
            ApiProvider.CLAUDE -> keyManager.getClaudeKey()
            ApiProvider.OPENROUTER -> keyManager.getOpenRouterKey()
            ApiProvider.MISTRAL -> keyManager.getMistralKey()
            ApiProvider.COHERE -> keyManager.getCohereKey()
            ApiProvider.HUGGINGFACE -> keyManager.getHuggingFaceKey()
        }
    }

    fun saveKeyForProvider(provider: ApiProvider, key: String) {
        when (provider) {
            ApiProvider.GEMINI -> keyManager.saveKey(1, key)
            ApiProvider.GROQ -> keyManager.saveGroqKey(key)
            ApiProvider.OPENAI -> keyManager.saveOpenAiKey(key)
            ApiProvider.CLAUDE -> keyManager.saveClaudeKey(key)
            ApiProvider.OPENROUTER -> keyManager.saveOpenRouterKey(key)
            ApiProvider.MISTRAL -> keyManager.saveMistralKey(key)
            ApiProvider.COHERE -> keyManager.saveCohereKey(key)
            ApiProvider.HUGGINGFACE -> keyManager.saveHuggingFaceKey(key)
        }
    }

    /**
     * Executes AI prompt request using primary selected provider,
     * and automatically failover through configured fallbacks if primary fails.
     */
    fun queryAi(prompt: String, systemInstruction: String? = null): Result<Pair<ApiProvider, String>> {
        val primary = getActiveProvider()
        val allProviders = ApiProvider.values().toList()

        // Order: Selected Primary Provider first, then remaining providers
        val providerOrder = listOf(primary) + allProviders.filter { it != primary }

        for (provider in providerOrder) {
            val apiKey = getKeyForProvider(provider)
            if (apiKey.isBlank()) continue

            val model = getSelectedModel(provider)
            Log.d("ApiManager", "Attempting request via ${provider.displayName} ($model)...")

            val result = executeCall(provider, apiKey, model, prompt, systemInstruction)
            if (result.isSuccess) {
                val text = result.getOrNull() ?: ""
                if (text.isNotBlank()) {
                    Log.d("ApiManager", "Success via ${provider.displayName}")
                    return Result.success(Pair(provider, text))
                }
            } else {
                Log.w("ApiManager", "${provider.displayName} request failed: ${result.exceptionOrNull()?.message}. Trying fallback...")
            }
        }

        return Result.failure(Exception("All Multi-API providers failed or no API keys configured."))
    }

    private fun executeCall(
        provider: ApiProvider,
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String?
    ): Result<String> {
        return when (provider) {
            ApiProvider.GEMINI -> geminiClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProvider.GROQ -> groqClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProvider.OPENAI -> openAIClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProvider.CLAUDE -> claudeClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProvider.OPENROUTER -> openRouterClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProvider.MISTRAL -> mistralClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProvider.COHERE -> cohereClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProvider.HUGGINGFACE -> huggingFaceClient.generateText(apiKey, model, prompt, systemInstruction)
        }
    }

    /**
     * Test connection for a given provider key
     */
    fun testConnection(provider: ApiProvider, apiKey: String, model: String): Result<String> {
        if (apiKey.isBlank()) return Result.failure(Exception("API Key cannot be blank"))
        val testPrompt = "Respond with 'AIRA OK' to confirm API connection."
        val result = executeCall(provider, apiKey, model, testPrompt, null)
        return if (result.isSuccess) {
            Result.success("${provider.displayName} connection verified successfully! ✅")
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Connection test failed"))
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ApiManager? = null

        fun getInstance(context: Context): ApiManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ApiManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
