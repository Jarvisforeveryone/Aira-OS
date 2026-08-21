package com.example.network.api

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class CohereClient {

    fun generateText(apiKey: String, model: String, prompt: String, systemInstruction: String? = null): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Cohere API key is blank or not configured"))
        }
        return try {
            val modelName = if (model.isBlank()) "command-r-plus" else model
            val url = "https://api.cohere.com/v2/chat"

            val messages = JSONArray().apply {
                if (!systemInstruction.isNullOrBlank()) {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", JSONObject().apply {
                            put("type", "text")
                            put("text", systemInstruction)
                        })
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    })
                })
            }

            val jsonBody = JSONObject().apply {
                put("model", modelName)
                put("messages", messages)
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
                    Log.e("CohereClient", "Cohere HTTP ${response.code}: $bodyStr")
                    return Result.failure(Exception("Cohere error ${response.code}: $bodyStr"))
                }

                val json = JSONObject(bodyStr)
                val messageObj = json.optJSONObject("message")
                val contentArr = messageObj?.optJSONArray("content")
                if (contentArr != null && contentArr.length() > 0) {
                    val text = contentArr.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) {
                        return Result.success(text)
                    }
                }
                Result.failure(Exception("Cohere returned empty response"))
            }
        } catch (e: Exception) {
            Log.e("CohereClient", "Cohere request failed", e)
            Result.failure(e)
        }
    }
}
