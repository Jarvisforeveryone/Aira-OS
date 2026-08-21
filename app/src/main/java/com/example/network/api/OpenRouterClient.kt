package com.example.network.api

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenRouterClient {

    fun generateText(apiKey: String, model: String, prompt: String, systemInstruction: String? = null): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("OpenRouter API key is blank or not configured"))
        }
        return try {
            val modelName = if (model.isBlank()) "nvidia/nemotron-3-ultra-550b-a55b:free" else model
            val url = "https://openrouter.ai/api/v1/chat/completions"

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
                .addHeader("HTTP-Referer", "https://aira.os")
                .addHeader("X-Title", "AIRA OS Voice Assistant")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            commonOkHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("OpenRouterClient", "OpenRouter HTTP ${response.code}: $bodyStr")
                    return Result.failure(Exception("OpenRouter error ${response.code}: $bodyStr"))
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
                Result.failure(Exception("OpenRouter returned empty response"))
            }
        } catch (e: Exception) {
            Log.e("OpenRouterClient", "OpenRouter request failed", e)
            Result.failure(e)
        }
    }
}
