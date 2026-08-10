package com.example.network.api

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ApiProvider(val displayName: String, val isPaid: Boolean) {
    GEMINI("Google Gemini", false),
    GROQ("Groq", false),
    OPENAI("OpenAI", true),
    CLAUDE("Claude (Anthropic)", true),
    OPENROUTER("OpenRouter", false),
    MISTRAL("Mistral AI", false),
    COHERE("Cohere", false),
    HUGGINGFACE("Hugging Face", false)
}

object ApiDefaults {
    val modelsMap = mapOf(
        ApiProvider.GEMINI to listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-2.0-flash"),
        ApiProvider.GROQ to listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "gemma-2-9b-it", "llama-3.2-3b-preview"),
        ApiProvider.OPENAI to listOf("gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo"),
        ApiProvider.CLAUDE to listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229"),
        ApiProvider.OPENROUTER to listOf("meta-llama/llama-3.3-70b-instruct", "google/gemini-2.0-flash-001", "mistralai/mistral-7b-instruct", "anthropic/claude-3.5-sonnet"),
        ApiProvider.MISTRAL to listOf("mistral-small-latest", "mistral-large-latest", "codestral-latest"),
        ApiProvider.COHERE to listOf("command-r-plus", "command-r", "command-a", "aya-expanse-32b"),
        ApiProvider.HUGGINGFACE to listOf("Qwen/Qwen2.5-72B-Instruct", "meta-llama/Llama-3.3-70B-Instruct", "mistralai/Mistral-7B-Instruct-v0.3")
    )
}

/**
 * Common OkHttpClient with resilient timeouts for all network clients.
 */
val commonOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
}
