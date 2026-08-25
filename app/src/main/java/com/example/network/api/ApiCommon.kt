package com.example.network.api

import android.util.Log
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

enum class ApiProviderType(val displayName: String, val isPaid: Boolean) {
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
        ApiProviderType.GEMINI to listOf(
            "gemini-3.5-flash",
            "gemini-flash-latest",
            "gemini-3.1-pro-preview",
            "gemini-3.1-flash-lite-preview",
            "gemini-2.5-flash-image"
        ),
        ApiProviderType.GROQ to listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "gemma2-9b-it"),
        ApiProviderType.OPENAI to listOf("gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo"),
        ApiProviderType.CLAUDE to listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229"),
        ApiProviderType.OPENROUTER to listOf(
            "nvidia/nemotron-3-ultra-550b-a55b:free",
            "google/gemma-4-31b-it:free",
            "inclusionai/ling-3.0-tiny:free",
            "cohere/north-mini-code:free",
            "poolside/laguna-s-2.1:free",
            "openai/gpt-oss-20b:free"
        ),
        ApiProviderType.MISTRAL to listOf("mistral-small-latest", "mistral-large-latest", "codestral-latest"),
        ApiProviderType.COHERE to listOf("command-r-plus", "command-a-111b"),
        ApiProviderType.HUGGINGFACE to listOf("meta-llama/Llama-3.3-70B-Instruct", "Qwen/Qwen2.5-72B-Instruct")
    )
}

/**
 * Common OkHttpClient with high-performance connection pooling,
 * robust 60-second timeouts for complex LLM generation and reasoning, and automatic gzip compression.
 */
val commonConnectionPool: ConnectionPool by lazy {
    ConnectionPool(15, 5L, TimeUnit.MINUTES)
}

val commonOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectionPool(commonConnectionPool)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("Accept-Encoding", "gzip")
                .build()
            chain.proceed(req)
        }
        .build()
}
