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

    fun getActiveProvider(): ApiProviderType {
        val name = keyManager.getSelectedProvider()
        return try {
            ApiProviderType.valueOf(name.uppercase())
        } catch (e: Exception) {
            ApiProviderType.GEMINI
        }
    }

    fun setActiveProvider(provider: ApiProviderType) {
        keyManager.setSelectedProvider(provider.name)
    }

    fun getSelectedModel(provider: ApiProviderType): String {
        val defaultModel = ApiDefaults.modelsMap[provider]?.firstOrNull() ?: ""
        return keyManager.getSelectedModel(provider.name, defaultModel)
    }

    fun setSelectedModel(provider: ApiProviderType, model: String) {
        keyManager.setSelectedModel(provider.name, model)
    }

    fun getKeyForProvider(provider: ApiProviderType): String {
        val multiKey = multiKeyManager.getNextKey(provider.name)
        if (!multiKey.isNullOrBlank()) return multiKey

        return when (provider) {
            ApiProviderType.GEMINI -> keyManager.getNextKey() ?: ""
            ApiProviderType.GROQ -> keyManager.getGroqKey()
            ApiProviderType.OPENAI -> keyManager.getOpenAiKey()
            ApiProviderType.CLAUDE -> keyManager.getClaudeKey()
            ApiProviderType.OPENROUTER -> keyManager.getOpenRouterKey()
            ApiProviderType.MISTRAL -> keyManager.getMistralKey()
            ApiProviderType.COHERE -> keyManager.getCohereKey()
            ApiProviderType.HUGGINGFACE -> keyManager.getHuggingFaceKey()
        }
    }

    fun saveKeyForProvider(provider: ApiProviderType, key: String) {
        multiKeyManager.addKey(provider.name, key)
        when (provider) {
            ApiProviderType.GEMINI -> keyManager.saveKey(1, key)
            ApiProviderType.GROQ -> keyManager.saveGroqKey(key)
            ApiProviderType.OPENAI -> keyManager.saveOpenAiKey(key)
            ApiProviderType.CLAUDE -> keyManager.saveClaudeKey(key)
            ApiProviderType.OPENROUTER -> keyManager.saveOpenRouterKey(key)
            ApiProviderType.MISTRAL -> keyManager.saveMistralKey(key)
            ApiProviderType.COHERE -> keyManager.saveCohereKey(key)
            ApiProviderType.HUGGINGFACE -> keyManager.saveHuggingFaceKey(key)
        }
    }

    /**
     * Executes AI prompt request using task-based auto-selected model or active provider,
     * with multi-key rotation and automatic failover chain across keys and providers.
     */
    fun queryAi(prompt: String, systemInstruction: String? = null): Result<Pair<ApiProviderType, String>> {
        val taskType = com.example.utils.TaskDetector.detectTaskType(prompt)
        Log.d("ApiManager", "Detected Task Type: $taskType for prompt")

        val primary = getActiveProvider()
        val allProviders = ApiProviderType.values().toList()

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

    private fun resolveModelForTask(provider: ApiProviderType, taskType: com.example.utils.TaskType): String {
        val userConfigured = getSelectedModel(provider)
        val defaultList = ApiDefaults.modelsMap[provider] ?: emptyList()

        return when (taskType) {
            com.example.utils.TaskType.CODE -> when (provider) {
                ApiProviderType.MISTRAL -> if (defaultList.contains("codestral-latest")) "codestral-latest" else userConfigured
                ApiProviderType.GROQ -> if (defaultList.contains("gpt-oss-120b")) "gpt-oss-120b" else userConfigured
                ApiProviderType.GEMINI -> if (defaultList.contains("gemini-3.1-pro-preview")) "gemini-3.1-pro-preview" else userConfigured
                else -> userConfigured
            }
            com.example.utils.TaskType.CREATIVE -> when (provider) {
                ApiProviderType.CLAUDE -> if (defaultList.contains("claude-3-5-sonnet-20241022")) "claude-3-5-sonnet-20241022" else userConfigured
                ApiProviderType.MISTRAL -> if (defaultList.contains("mistral-large-latest")) "mistral-large-latest" else userConfigured
                ApiProviderType.GEMINI -> if (defaultList.contains("gemini-3.5-flash")) "gemini-3.5-flash" else userConfigured
                else -> userConfigured
            }
            com.example.utils.TaskType.COMMAND -> when (provider) {
                ApiProviderType.GEMINI -> if (defaultList.contains("gemini-3.5-flash")) "gemini-3.5-flash" else userConfigured
                ApiProviderType.GROQ -> if (defaultList.contains("llama-3.1-8b-instant")) "llama-3.1-8b-instant" else userConfigured
                else -> userConfigured
            }
            com.example.utils.TaskType.RESEARCH -> when (provider) {
                ApiProviderType.GEMINI -> if (defaultList.contains("gemini-3.1-pro-preview")) "gemini-3.1-pro-preview" else userConfigured
                ApiProviderType.GROQ -> if (defaultList.contains("llama-3.3-70b-versatile")) "llama-3.3-70b-versatile" else userConfigured
                else -> userConfigured
            }
            com.example.utils.TaskType.CHAT -> when (provider) {
                ApiProviderType.GEMINI -> if (defaultList.contains("gemini-3.5-flash")) "gemini-3.5-flash" else userConfigured
                ApiProviderType.MISTRAL -> if (defaultList.contains("mistral-small-latest")) "mistral-small-latest" else userConfigured
                else -> userConfigured
            }
        }
    }

    private fun executeCall(
        provider: ApiProviderType,
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String?
    ): Result<String> {
        return when (provider) {
            ApiProviderType.GEMINI -> geminiClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProviderType.GROQ -> groqClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProviderType.OPENAI -> openAIClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProviderType.CLAUDE -> claudeClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProviderType.OPENROUTER -> openRouterClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProviderType.MISTRAL -> mistralClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProviderType.COHERE -> cohereClient.generateText(apiKey, model, prompt, systemInstruction)
            ApiProviderType.HUGGINGFACE -> huggingFaceClient.generateText(apiKey, model, prompt, systemInstruction)
        }
    }

    /**
     * Test connection for a given provider key
     */
    fun testConnection(provider: ApiProviderType, apiKey: String, model: String): Result<String> {
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
