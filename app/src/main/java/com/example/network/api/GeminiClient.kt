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

data class GeminiStreamChunk(
    val text: String,
    val isThought: Boolean = false
)

class GeminiClient {

    fun resolveModelName(model: String, isThinking: Boolean): String {
        val trimmed = model.trim().lowercase()
        return when {
            isThinking || trimmed.contains("pro") || trimmed.contains("thinking") -> "gemini-3.1-pro-preview"
            trimmed.contains("lite") -> "gemini-3.1-flash-lite-preview"
            trimmed.contains("image") -> "gemini-2.5-flash-image"
            trimmed.contains("native-audio") -> "gemini-2.5-flash-native-audio-preview-12-2025"
            trimmed.contains("1.5") || trimmed.contains("2.0") || trimmed.isBlank() || trimmed == "gemini" -> "gemini-3.5-flash"
            else -> model.trim()
        }
    }

    fun streamGenerateContent(
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String? = null,
        enableThinkingMode: Boolean = false
    ): Flow<GeminiStreamChunk> = flow {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key is blank or not configured")
        }

        val isThinkingModel = enableThinkingMode || model.contains("gemini-3.1-pro") || model.contains("thinking")
        val modelName = resolveModelName(model, isThinkingModel)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:streamGenerateContent?alt=sse&key=$apiKey"

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
            } else {
                put("maxOutputTokens", 4096)
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

        val response = commonOkHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            response.close()
            throw IllegalStateException("Gemini stream HTTP ${response.code}: $errBody")
        }

        val body = response.body ?: run {
            response.close()
            throw IllegalStateException("Empty response body from Gemini stream")
        }

        try {
            val reader = BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8))
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val dataJson = trimmed.substring(5).trim()
                    if (dataJson.isNotEmpty() && dataJson != "[DONE]") {
                        try {
                            val json = JSONObject(dataJson)
                            val candidates = json.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val first = candidates.getJSONObject(0)
                                val content = first.optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                if (parts != null) {
                                    for (i in 0 until parts.length()) {
                                        val part = parts.getJSONObject(i)
                                        val partText = part.optString("text", "")
                                        val isThought = part.optBoolean("thought", false)
                                        if (partText.isNotEmpty()) {
                                            emit(GeminiStreamChunk(text = partText, isThought = isThought))
                                        }
                                    }
                                }
                            }
                        } catch (parseEx: Exception) {
                            Log.w("GeminiClient", "Failed to parse SSE JSON chunk: $dataJson", parseEx)
                        }
                    }
                }
                line = reader.readLine()
            }
        } finally {
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    fun generateText(
        apiKey: String, 
        model: String, 
        prompt: String, 
        systemInstruction: String? = null,
        enableThinkingMode: Boolean = false
    ): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Gemini API key is blank or not configured"))
        }

        return try {
            val isThinkingModel = enableThinkingMode || model.contains("gemini-3.1-pro") || model.contains("thinking")
            val modelName = resolveModelName(model, isThinkingModel)

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
                } else {
                    put("maxOutputTokens", 4096)
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
                        val sb = StringBuilder()
                        for (i in 0 until parts.length()) {
                            val partText = parts.getJSONObject(i).optString("text", "")
                            sb.append(partText)
                        }
                        val fullText = sb.toString().trim()
                        if (fullText.isNotBlank()) {
                            return Result.success(fullText)
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
