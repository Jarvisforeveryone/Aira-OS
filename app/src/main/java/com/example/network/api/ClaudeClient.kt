package com.example.network.api

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class ClaudeClient {

    fun generateText(apiKey: String, model: String, prompt: String, systemInstruction: String? = null): Result<String> {
        if (apiKey.isNullOrBlank()) {
            throw AiraApiException(
                code = "API_KEY_MISSING",
                message = "AIRA API key not found. Please re-enter your API keys in Settings."
            )
        }
        return try {
            val modelName = if (model.isBlank()) "claude-3-5-sonnet-20241022" else model
            val url = "https://api.anthropic.com/v1/messages"

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }

            val jsonBody = JSONObject().apply {
                put("model", modelName)
                put("messages", messages)
                put("max_tokens", 1024)
                if (!systemInstruction.isNullOrBlank()) {
                    put("system", systemInstruction)
                }
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            commonOkHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("ClaudeClient", "Claude HTTP ${response.code}: $bodyStr")
                    return Result.failure(Exception("Claude error ${response.code}: $bodyStr"))
                }

                val json = JSONObject(bodyStr)
                val contentArray = json.optJSONArray("content")
                if (contentArray != null && contentArray.length() > 0) {
                    val first = contentArray.getJSONObject(0)
                    val text = first.optString("text", "")
                    if (text.isNotBlank()) {
                        return Result.success(text)
                    }
                }
                Result.failure(Exception("Claude returned empty response"))
            }
        } catch (e: Exception) {
            Log.e("ClaudeClient", "Claude request failed", e)
            Result.failure(e)
        }
    }
}
