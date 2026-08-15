package com.example.network.api

import android.content.Context
import android.util.Log
import com.example.data.MultiKeyManager

object ApiTest {

    private const val TEST_PROMPT = "Say 'AIRA OK' if you receive this."

    private val geminiClient = GeminiClient()
    private val groqClient = GroqClient()
    private val openAIClient = OpenAIClient()
    private val claudeClient = ClaudeClient()
    private val openRouterClient = OpenRouterClient()
    private val mistralClient = MistralClient()
    private val cohereClient = CohereClient()
    private val huggingFaceClient = HuggingFaceClient()

    fun testKey(context: Context, provider: ApiProviderType, apiKey: String, model: String): Result<String> {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            val err = "API Key cannot be blank"
            MultiKeyManager.getInstance(context).setKeyStatus(provider.name, trimmedKey, false, err)
            return Result.failure(Exception(err))
        }

        val multiKeyMgr = MultiKeyManager.getInstance(context)
        val defaultModel = ApiDefaults.modelsMap[provider]?.firstOrNull() ?: model
        val targetModel = if (model.isNotBlank()) model else defaultModel

        Log.d("ApiTest", "Testing key for ${provider.displayName} with model $targetModel...")

        val result = when (provider) {
            ApiProviderType.GEMINI -> geminiClient.generateText(trimmedKey, targetModel, TEST_PROMPT, null)
            ApiProviderType.GROQ -> groqClient.generateText(trimmedKey, targetModel, TEST_PROMPT, null)
            ApiProviderType.OPENAI -> openAIClient.generateText(trimmedKey, targetModel, TEST_PROMPT, null)
            ApiProviderType.CLAUDE -> claudeClient.generateText(trimmedKey, targetModel, TEST_PROMPT, null)
            ApiProviderType.OPENROUTER -> openRouterClient.generateText(trimmedKey, targetModel, TEST_PROMPT, null)
            ApiProviderType.MISTRAL -> mistralClient.generateText(trimmedKey, targetModel, TEST_PROMPT, null)
            ApiProviderType.COHERE -> cohereClient.generateText(trimmedKey, targetModel, TEST_PROMPT, null)
            ApiProviderType.HUGGINGFACE -> huggingFaceClient.generateText(trimmedKey, targetModel, TEST_PROMPT, null)
        }

        return if (result.isSuccess) {
            val resp = result.getOrNull() ?: ""
            val successMsg = "✅ Connection Verified"
            multiKeyMgr.setKeyStatus(provider.name, trimmedKey, true, successMsg)
            Result.success(successMsg)
        } else {
            val rawErr = result.exceptionOrNull()?.message ?: "Unknown Error"
            val cleanErr = when {
                rawErr.contains("401") || rawErr.contains("403") || rawErr.contains("Key") -> "Invalid API Key"
                rawErr.contains("404") -> "Model unavailable ($targetModel)"
                rawErr.contains("429") -> "Rate limit reached"
                else -> rawErr.take(60)
            }
            val failMsg = "❌ Failed: $cleanErr"
            multiKeyMgr.setKeyStatus(provider.name, trimmedKey, false, failMsg)
            Result.failure(Exception(failMsg))
        }
    }
}
