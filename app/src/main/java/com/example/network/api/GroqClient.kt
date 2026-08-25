package com.example.network.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class GroqClient {

    fun streamGenerateContent(
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String? = null
    ): Flow<String> = flow {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Groq API key is blank or not configured")
        }

        val modelName = if (model.isBlank()) "llama-3.3-70b-versatile" else model
        val url = "https://api.groq.com/openai/v1/chat/completions"

        val messages = JSONArray().apply {
            if (!systemInstruction.isNullOrBlank()) {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", modelName)
            put("messages", messages)
            put("temperature", 0.7)
            put("max_tokens", 2048)
            put("stream", true)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = commonOkHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            response.close()
            throw IllegalStateException("Groq stream HTTP ${response.code}: $errBody")
        }

        val body = response.body ?: run {
            response.close()
            throw IllegalStateException("Empty response body from Groq stream")
        }

        try {
            val reader = BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8))
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val dataJson = trimmed.substring(5).trim()
                    if (dataJson == "[DONE]") {
                        break
                    }
                    if (dataJson.isNotEmpty()) {
                        try {
                            val json = JSONObject(dataJson)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val first = choices.getJSONObject(0)
                                val delta = first.optJSONObject("delta")
                                val content = delta?.optString("content", "") ?: ""
                                if (content.isNotEmpty()) {
                                    emit(content)
                                }
                            }
                        } catch (parseEx: Exception) {
                            Log.w("GroqClient", "Failed to parse SSE JSON chunk: $dataJson", parseEx)
                        }
                    }
                }
                line = reader.readLine()
            }
        } finally {
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    fun generateText(apiKey: String, model: String, prompt: String, systemInstruction: String? = null): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Groq API key is blank or not configured"))
        }
        return try {
            val modelName = if (model.isBlank()) "llama-3.3-70b-versatile" else model
            val url = "https://api.groq.com/openai/v1/chat/completions"

            val messages = JSONArray().apply {
                if (!systemInstruction.isNullOrBlank()) {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemInstruction)
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }

            val jsonBody = JSONObject().apply {
                put("model", modelName)
                put("messages", messages)
                put("temperature", 0.7)
                put("max_tokens", 1024)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            commonOkHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("GroqClient", "Groq HTTP ${response.code}: $bodyStr")
                    return Result.failure(Exception("Groq error ${response.code}: $bodyStr"))
                }

                val json = JSONObject(bodyStr)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val first = choices.getJSONObject(0)
                    val message = first.optJSONObject("message")
                    val text = message?.optString("content", "") ?: ""
                    if (text.isNotBlank()) {
                        return Result.success(text)
                    }
                }
                Result.failure(Exception("Groq returned empty response"))
            }
        } catch (e: Exception) {
            Log.e("GroqClient", "Groq request failed", e)
            Result.failure(e)
        }
    }
}
