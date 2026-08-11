package com.example.data

import android.content.Context
import android.util.Log
import com.example.network.api.*

class ApiManager private constructor(private val context: Context) {

    private val keyManager = ChatKeyManager.getInstance(context)
    private val multiKeyManager = MultiKeyManager.getInstance(context)

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
        val multiKey = multiKeyManager.getNextKey(provider.name)
        if (!multiKey.isNullOrBlank()) return multiKey

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
        multiKeyManager.addKey(provider.name, key)
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
     * Executes AI prompt request using task-based auto-selected model or active provider,
     * with multi-key rotation and automatic failover chain across keys and providers.
     */
    fun queryAi(prompt: String, systemInstruction: String? = null): Result<Pair<ApiProvider, String>> {
        val taskType = com.example.utils.TaskDetector.detectTaskType(prompt)
        Log.d("ApiManager", "Detected Task Type: $taskType for prompt")

        val primary = getActiveProvider()
        val allProviders = ApiProvider.values().toList()

        // Order: Selected Primary Provider first, then remaining providers
        val providerOrder = listOf(primary) + allProviders.filter { it != primary }

        for (provider in providerOrder) {
            // Retrieve all configured keys for provider
            val keys = multiKeyManager.getKeys(provider.name).ifEmpty {
                val legacy = getKeyForProvider(provider)
                if (legacy.isNotBlank()) listOf(legacy) else emptyList()
            }

            if (keys.isEmpty()) continue

            // Determine model based on task type mapping
            val model = resolveModelForTask(provider, taskType)
            Log.d("ApiManager", "Attempting request via ${provider.displayName} ($model) for task $taskType...")

            for (apiKey in keys) {
                val result = executeCall(provider, apiKey, model, prompt, systemInstruction)
                if (result.isSuccess) {
                    val text = result.getOrNull() ?: ""
                    if (text.isNotBlank()) {
                        Log.d("ApiManager", "Success via ${provider.displayName} ($model)")
                        return Result.success(Pair(provider, text))
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w("ApiManager", "${provider.displayName} key failed: $err. Trying next key/provider...")
                    multiKeyManager.markCooldown(provider.name, apiKey)
                }
            }
        }

        return Result.failure(Exception("All Multi-API providers and keys failed or no API keys configured."))
    }

    private fun resolveModelForTask(provider: ApiProvider, taskType: com.example.utils.TaskType): String {
        val userConfigured = getSelectedModel(provider)
        val defaultList = ApiDefaults.modelsMap[provider] ?: emptyList()

        return when (taskType) {
            com.example.utils.TaskType.CODE -> when (provider) {
                ApiProvider.MISTRAL -> if (defaultList.contains("codestral-latest")) "codestral-latest" else userConfigured
                ApiProvider.GROQ -> if (defaultList.contains("gpt-oss-120b")) "gpt-oss-120b" else userConfigured
                ApiProvider.GEMINI -> if (defaultList.contains("gemini-3.6-flash")) "gemini-3.6-flash" else userConfigured
                else -> userConfigured
            }
            com.example.utils.TaskType.CREATIVE -> when (provider) {
                ApiProvider.CLAUDE -> if (defaultList.contains("claude-3-5-sonnet-20241022")) "claude-3-5-sonnet-20241022" else userConfigured
                ApiProvider.MISTRAL -> if (defaultList.contains("mistral-large-latest")) "mistral-large-latest" else userConfigured
                else -> userConfigured
            }
            com.example.utils.TaskType.COMMAND -> when (provider) {
                ApiProvider.GEMINI -> if (defaultList.contains("gemini-2.5-flash")) "gemini-2.5-flash" else userConfigured
                ApiProvider.GROQ -> if (defaultList.contains("llama-3.1-8b-instant")) "llama-3.1-8b-instant" else userConfigured
                else -> userConfigured
            }
            com.example.utils.TaskType.RESEARCH -> when (provider) {
                ApiProvider.GEMINI -> if (defaultList.contains("gemini-3.6-flash")) "gemini-3.6-flash" else userConfigured
                ApiProvider.GROQ -> if (defaultList.contains("llama-3.3-70b-versatile")) "llama-3.3-70b-versatile" else userConfigured
                else -> userConfigured
            }
            com.example.utils.TaskType.CHAT -> when (provider) {
                ApiProvider.GEMINI -> if (defaultList.contains("gemini-2.5-flash-lite")) "gemini-2.5-flash-lite" else userConfigured
                ApiProvider.MISTRAL -> if (defaultList.contains("mistral-small-latest")) "mistral-small-latest" else userConfigured
                else -> userConfigured
            }
        }
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
        return com.example.network.api.ApiTest.testKey(context, provider, apiKey, model)
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
