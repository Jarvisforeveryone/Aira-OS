package com.example.network.api

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiClient {

    fun generateText(
        apiKey: String, 
        model: String, 
        prompt: String, 
        systemInstruction: String? = null,
        enableThinkingMode: Boolean = false
    ): Result<String> {
        return try {
            val resolvedModel = if (model.isBlank()) "gemini-3.5-flash" else model
            val isThinkingModel = enableThinkingMode || resolvedModel.contains("gemini-3.1-pro") || resolvedModel.contains("thinking")
            val modelName = if (isThinkingModel && !resolvedModel.contains("gemini-3.1-pro")) "gemini-3.1-pro-preview" else resolvedModel

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            }

            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                if (isThinkingModel) {
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    })
                    // CRITICAL: Do NOT set maxOutputTokens when using thinking mode
                } else {
                    put("maxOutputTokens", 2048)
                }
            }

            val jsonBody = JSONObject().apply {
                put("contents", contentsArray)
                if (!systemInstruction.isNullOrBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemInstruction))
                        })
                    })
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            commonOkHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("GeminiClient", "Gemini HTTP ${response.code}: $bodyStr")
                    return Result.failure(Exception("Gemini error ${response.code}: $bodyStr"))
                }

                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val first = candidates.getJSONObject(0)
                    val content = first.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank()) {
                            return Result.success(text)
                        }
                    }
                }
                Result.failure(Exception("Gemini returned empty payload"))
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "Gemini request failed", e)
            Result.failure(e)
        }
    }
}
