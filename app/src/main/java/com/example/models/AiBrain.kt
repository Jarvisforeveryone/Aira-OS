package com.example.models

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.ChatKeyManager
import com.example.data.GrokCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiBrain(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getAiResponse(
        prompt: String,
        systemInstruction: String,
        history: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val query = prompt.trim()
        val cacheDao = AppDatabase.getDatabase(context).grokCacheDao()

        // Step 1: Check Room DB cache. If same prompt in last 24h, return cached answer.
        try {
            val cachedEntry = cacheDao.getCacheForQuery(query)
            if (cachedEntry != null) {
                val ageMs = System.currentTimeMillis() - cachedEntry.timestamp
                val oneDayMs = 24 * 60 * 60 * 1000L
                if (ageMs < oneDayMs) {
                    val responseCached = cachedEntry.response
                    return@withContext responseCached
                } else {
                    cacheDao.deleteCache(query)
                }
            }
        } catch (e: Exception) {
            Log.e("AiBrain", "Cache lookup failed: ", e)
        }

        // Check if user selected Groq API as their online brain
        val sharedPrefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
        val selectedOnlineModel = sharedPrefs.getString("online_model", "Gemini API") ?: "Gemini API"

        if (selectedOnlineModel.equals("Groq API", ignoreCase = true)) {
            val groqKey = ChatKeyManager.getInstance(context).getGroqKey()
            if (groqKey.isEmpty()) {
                return@withContext "Groq API key is missing. Please configure it in Settings."
            }

            val url = "https://api.groq.com/openai/v1/chat/completions"

            val resultText = com.example.data.NetworkErrorHandler.safeApiCall("Groq API") {
                val messagesArray = JSONArray()

                // System Instruction
                if (systemInstruction.isNotEmpty()) {
                    messagesArray.put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemInstruction)
                    })
                }

                // Add History
                for (turn in history) {
                    val roleName = if (turn.first.equals("user", ignoreCase = true)) "user" else "assistant"
                    messagesArray.put(JSONObject().apply {
                        put("role", roleName)
                        put("content", turn.second)
                    })
                }

                // Add current prompt
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })

                val rootJson = JSONObject().apply {
                    put("model", "llama-3.2-3b-preview") // Ultra-fast Llama model on Groq
                    put("messages", messagesArray)
                    put("max_tokens", 300)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = rootJson.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $groqKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val code = response.code
                    if (code == 200) {
                        val responseBodyStr = response.body?.string()
                        if (!responseBodyStr.isNullOrEmpty()) {
                            val rootResp = JSONObject(responseBodyStr)
                            val choices = rootResp.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val firstChoice = choices.getJSONObject(0)
                                val messageObj = firstChoice.optJSONObject("message")
                                if (messageObj != null) {
                                    val responseText = messageObj.optString("content", "No response text")

                                    // Save successful response in Room cache
                                    try {
                                        cacheDao.insertCache(GrokCache(query = query, response = responseText))
                                    } catch (e: Exception) {
                                        Log.e("AiBrain", "Cache insert failed: ", e)
                                    }

                                    responseText
                                } else "No response content"
                            } else "No choices array"
                        } else "Empty body"
                    } else {
                        val errorBody = response.body?.string() ?: ""
                        Log.e("AiBrain", "Groq error response: $errorBody")
                        throw Exception("HTTP $code")
                    }
                }
            }
            return@withContext resultText ?: "Error: Failed to connect to Groq."
        }

        // Step 2 & 4: If no cache, call ChatKeyManager.getNextKey() and retry. Max 3 retries.
        var activeKey = ChatKeyManager.getInstance(context).getNextKey()
        var retries = 0
        val maxRetries = 3

        while (retries < maxRetries) {
            if (activeKey.isNullOrEmpty()) {
                return@withContext "All chat keys are down. Add new key in Settings"
            }

            val modelName = "gemini-3.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$activeKey"

            try {
                val contentsArray = JSONArray()

                // Add History
                for (turn in history) {
                    val turnObj = JSONObject()
                    val isUser = turn.first.equals("user", ignoreCase = true)
                    turnObj.put("role", if (isUser) "user" else "model")
                    
                    val partsArray = JSONArray()
                    val partObj = JSONObject()
                    partObj.put("text", turn.second)
                    partsArray.put(partObj)
                    
                    turnObj.put("parts", partsArray)
                    contentsArray.put(turnObj)
                }

                // Add current prompt
                val currentUserTurn = JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                }
                contentsArray.put(currentUserTurn)

                val rootJson = JSONObject().apply {
                    put("contents", contentsArray)
                    if (systemInstruction.isNotEmpty()) {
                        val sysInstObj = JSONObject()
                        val sysPartsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstruction) })
                        }
                        sysInstObj.put("parts", sysPartsArray)
                        put("systemInstruction", sysInstObj)
                    }
                    // Max tokens setting to save API quotas
                    put("generationConfig", JSONObject().apply {
                        put("maxOutputTokens", 300)
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = rootJson.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val apiResponse = com.example.data.NetworkErrorHandler.safeApiCall("Gemini API") {
                    client.newCall(request).execute().use { response ->
                        val code = response.code
                        if (code == 200) {
                            val responseBodyStr = response.body?.string()
                            if (!responseBodyStr.isNullOrEmpty()) {
                                val rootResp = JSONObject(responseBodyStr)
                                val candidates = rootResp.optJSONArray("candidates")
                                if (candidates != null && candidates.length() > 0) {
                                    val firstCandidate = candidates.getJSONObject(0)
                                    val content = firstCandidate.optJSONObject("content")
                                    if (content != null) {
                                        val parts = content.optJSONArray("parts")
                                        if (parts != null && parts.length() > 0) {
                                            val responseText = parts.getJSONObject(0).optString("text", "No response text")
                                            
                                            // Step 3: Save successful response in Room cache
                                            try {
                                                cacheDao.insertCache(GrokCache(query = query, response = responseText))
                                            } catch (e: Exception) {
                                                Log.e("AiBrain", "Cache insert failed: ", e)
                                            }

                                            responseText
                                        } else "No text parts"
                                    } else "No response content"
                                } else "No candidate choices"
                            } else "Empty Gemini response"
                        } else if (code == 401 || code == 429 || code == 500) {
                            activeKey?.let { ChatKeyManager.getInstance(context).markCooldown(it) }
                            activeKey = ChatKeyManager.getInstance(context).getNextKey()
                            retries++
                            throw Exception("HTTP $code")
                        } else {
                            val errorBody = response.body?.string() ?: ""
                            Log.e("AiBrain", "Error response: $errorBody")
                            throw Exception("HTTP $code")
                        }
                    }
                }
                if (apiResponse != null) {
                    return@withContext apiResponse
                } else {
                    retries++
                    activeKey = ChatKeyManager.getInstance(context).getNextKey()
                }
            } catch (e: Exception) {
                Log.e("AiBrain", "Exception in API call: ", e)
                retries++
                activeKey = ChatKeyManager.getInstance(context).getNextKey()
            }
        }
        return@withContext "All chat keys are down. Add new key in Settings"
    }

    /**
     * Highly efficient local rule-based helper that executes when the app is in "Offline Mode"
     * to prevent JVM OutOfMemory crashes on 2GB RAM devices, while remaining beautifully supportive of voice commands.
     */
    fun getOfflineLocalResponse(prompt: String): String {
        val clean = prompt.lowercase().trim()
        return when {
            clean.contains("call") || clean.contains("phone") || clean.contains("dial") -> {
                "Offline Mode Assistant: Initiating dial action. Please speak the number or grant Call permission."
            }
            clean.contains("flashlight") || clean.contains("torch") || clean.contains("light") -> {
                "Offline Mode Assistant: Flashlight toggle command acknowledged. Toggling torch."
            }
            clean.contains("alarm") || clean.contains("wake me") -> {
                "Offline Mode Assistant: Alarm module activated. Let's configure your alert."
            }
            clean.contains("weather") || clean.contains("temperature") -> {
                "Offline Mode Assistant: Live weather retrieval requires online sync. Current offline mock temperature is 24°C, Clear Sky."
            }
            clean.contains("news") || clean.contains("headline") -> {
                "Offline Mode Assistant: News feeds require online sync. Locally saved headline: Aira Version 1.0 successfully active."
            }
            clean.contains("hello") || clean.contains("hey") || clean.contains("hi") -> {
                "Hello from Aira! I am active Offline. Internet-dependent features are currently paused, but I can toggle local hardware modules."
            }
            else -> {
                "Offline Assistant: I am running in Low Performance Offline Mode to safeguard your device's 2GB RAM. Toggle Online in Settings for standard Gemini conversations!"
            }
        }
    }
}
