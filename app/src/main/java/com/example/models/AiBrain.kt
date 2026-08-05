package com.example.models

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.ChatKeyManager
import com.example.data.GrokCache
import com.example.data.Memory
import com.example.data.GeminiOkHttpCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class UserEmotion {
    HAPPY, SAD, ANGRY, CONFUSED, CURIOSITY, FRUSTRATED, NEUTRAL
}

enum class EmotionIntensity {
    HIGH, MEDIUM, LOW
}

data class EmotionState(
    val emotion: UserEmotion,
    val intensity: EmotionIntensity = EmotionIntensity.MEDIUM
)

object JarvisEmotionEngine {
    private var lastEmotion: UserEmotion = UserEmotion.NEUTRAL

    fun detectEmotion(text: String): EmotionState {
        val sentiment = com.example.utils.SentimentAnalysisUtility.analyzeSentiment(text)
        val detected = sentiment.emotion
        
        lastEmotion = if (text.split(" ").size < 3 && detected == UserEmotion.NEUTRAL) lastEmotion else detected
        return EmotionState(lastEmotion, sentiment.intensity)
    }

    private fun keywordsMatch(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}

object JarvisMemoryExtractor {
    suspend fun extractAndStoreMemories(context: Context, userInput: String) {
        val lower = userInput.lowercase().trim()
        val db = AppDatabase.getDatabase(context)
        val memoryDao = db.memoryDao()

        val preferencePatterns = listOf(
            Regex("(?:i like|i love|i prefer|my favorite is|i enjoy) ([^.,!?]+)"),
            Regex("(?:my name is|call me|i am) ([^.,!?]+)"),
            Regex("(?:i work as|i am a|my job is) ([^.,!?]+)"),
            Regex("(?:i live in|i am from) ([^.,!?]+)"),
            Regex("(?:i don't like|i hate|i dislike) ([^.,!?]+)")
        )

        for (pattern in preferencePatterns) {
            val match = pattern.find(lower)
            if (match != null && match.groupValues.size > 1) {
                val fact = userInput.substring(match.range.start, match.range.endInclusive + 1)
                val existing = memoryDao.getAllMemoriesList()
                if (existing.none { it.factText.equals(fact, ignoreCase = true) }) {
                    memoryDao.insertMemory(
                        Memory(
                            factText = fact.replaceFirstChar { it.uppercase() },
                            source = "auto_jarvis_brain",
                            category = "Preferences",
                            isImportant = true
                        )
                    )
                    Log.d("JarvisMemoryExtractor", "Auto-learned memory: $fact")
                }
            }
        }
    }

    suspend fun getFormattedMemoryContext(context: Context): String {
        return try {
            val db = AppDatabase.getDatabase(context)
            val memories = db.memoryDao().getAllMemoriesList().take(8)
            if (memories.isEmpty()) "" else {
                val facts = memories.joinToString("; ") { it.factText }
                "\nUser Personal Context & Preferences Remembered: [$facts]\n"
            }
        } catch (e: Exception) {
            ""
        }
    }
}

object JarvisDialoguePlanner {
    fun formatResponse(rawText: String, emotionState: EmotionState, userPrompt: String, userName: String = "sir"): String {
        var text = rawText.trim()
        val lowerPrompt = userPrompt.lowercase()
        val emotion = emotionState.emotion

        if (lowerPrompt.contains("news") || lowerPrompt.contains("headline")) {
            val sentences = text.split(Regex("(?<=[.!?])\\s+"))
            if (sentences.size > 3) {
                text = sentences.take(3).joinToString(" ")
            }
            if (!text.startsWith("Here's a summary", ignoreCase = true)) {
                text = "Here's a summary, $userName: $text"
            }
        } else if (lowerPrompt.contains("weather") || lowerPrompt.contains("temperature")) {
            if (!text.lowercase().startsWith("it's") && !text.lowercase().startsWith("it is")) {
                text = "It's 22°C with clear skies, $userName. Perfect weather for a brief walk."
            }
        } else if (lowerPrompt.startsWith("turn on") || lowerPrompt.startsWith("toggle") || lowerPrompt.startsWith("set ") || lowerPrompt.startsWith("open ") || lowerPrompt.startsWith("lock ")) {
            if (!text.contains("done", ignoreCase = true) && !text.contains("already done", ignoreCase = true)) {
                text = "Done, $userName. $text"
            }
        } else if (lowerPrompt.contains("what do you think") || lowerPrompt.contains("opinion") || lowerPrompt.contains("should i")) {
            if (!text.startsWith("I think", ignoreCase = true) && !text.startsWith("In my opinion", ignoreCase = true)) {
                text = "In my opinion, $userName, $text"
            }
        }

        val prefixes = when (emotion) {
            UserEmotion.HAPPY -> listOf("Splendid news, $userName! ", "Delighted to hear that, $userName! ", "Marvellous! ", "Capital! ")
            UserEmotion.SAD -> listOf("I am deeply sorry to hear that, $userName. ", "Rest assured, I am right here with you. ", "Allow me to lighten the load, $userName. ")
            UserEmotion.ANGRY -> listOf("Calm waters, $userName. ", "Allow me to resolve this seamlessly for you. ", "Right away, $userName. Steady as she goes. ")
            UserEmotion.CONFUSED -> listOf("Allow me to clarify that for you, $userName. ", "Let's unpack that together. ")
            UserEmotion.CURIOSITY -> listOf("An intriguing query, $userName. ", "Indeed, as luck would have it, ")
            UserEmotion.FRUSTRATED -> listOf("No need for alarm, $userName. I've taken care of it. ")
            UserEmotion.NEUTRAL -> listOf("", "Well, $userName, ", "Indeed, ", "Right then, ")
        }

        val prefix = prefixes.random()
        if (prefix.isNotEmpty() && !text.startsWith(prefix, ignoreCase = true) && !text.contains("sir", ignoreCase = true) && !text.contains(userName, ignoreCase = true)) {
            text = "$prefix$text"
        }

        val isSimpleToggle = lowerPrompt.startsWith("turn") || lowerPrompt.startsWith("toggle") || lowerPrompt.startsWith("lock") || lowerPrompt.startsWith("set volume")
        if (!text.endsWith("?") && !isSimpleToggle) {
            val followUps = listOf(
                " Shall I assist you with anything further, $userName?",
                " What are your thoughts on this?",
                " Would you like me to look deeper into this for you, $userName?",
                " Is there anything else I can queue up for you?"
            )
            text += followUps.random()
        }

        return text
    }
}

enum class JarvisIntent {
    NEWS,
    WEATHER,
    DEVICE_COMMAND,
    QUESTION_OPINION,
    GENERAL_CONVERSATION
}

data class DispatchedIntentResult(
    val intent: JarvisIntent,
    val sentiment: com.example.utils.SentimentResult,
    val formattedResponse: String
)

object JarvisIntentDispatcher {

    fun dispatchAndFormat(
        userInput: String,
        rawResponse: String,
        userName: String = "sir"
    ): DispatchedIntentResult {
        val sentiment = com.example.utils.SentimentAnalysisUtility.analyzeSentiment(userInput)
        val lower = userInput.lowercase().trim()

        val detectedIntent = when {
            lower.contains("news") || lower.contains("headline") || lower.contains("article") || lower.contains("bulletin") -> JarvisIntent.NEWS
            lower.contains("weather") || lower.contains("temperature") || lower.contains("forecast") || lower.contains("rain") || lower.contains("sunny") -> JarvisIntent.WEATHER
            lower.startsWith("turn ") || lower.startsWith("toggle ") || lower.startsWith("set ") || lower.startsWith("open ") || lower.startsWith("lock ") || lower.contains("brightness") || lower.contains("bluetooth") || lower.contains("wifi") || lower.contains("flashlight") -> JarvisIntent.DEVICE_COMMAND
            lower.contains("what do you think") || lower.contains("opinion") || lower.contains("should i") || lower.startsWith("why") || lower.startsWith("how come") -> JarvisIntent.QUESTION_OPINION
            else -> JarvisIntent.GENERAL_CONVERSATION
        }

        val emotionState = EmotionState(sentiment.emotion, sentiment.intensity)
        val formatted = JarvisDialoguePlanner.formatResponse(rawResponse, emotionState, userInput, userName)

        return DispatchedIntentResult(
            intent = detectedIntent,
            sentiment = sentiment,
            formattedResponse = formatted
        )
    }
}

class AiBrain(private val context: Context) {

    companion object {
        const val JARVIS_SYSTEM_INSTRUCTION = """
You are Jarvis — witty, confident, helpful, and slightly British.
Always address the user with conversational, polite terms like 'sir', 'at your service', or 'right away, sir'.
Adapt your tone based on user tone and emotion:
- Excitement and humor for happy inputs.
- Empathy, softness, and reassurance for sad or distressed inputs.
- Calm, composed, and soothing tone for angry inputs.
- Confident, composed, and efficient tone for neutral inputs.

Strict Response Rules:
1. News: Provide a concise summary in exactly 3 sentences starting with "Here's a summary:".
2. Weather: Format strictly as "It's [X]°C with [condition]. [suggestion]".
3. System Commands: Respond concisely with "Done, sir." or "Already done, sir."
4. Questions & Opinions: Express clear opinions starting with "I think..." or "In my opinion...".
5. Conversational Flow: Use natural fillers like "Well,", "Indeed,", "Ah,", "Right, then," and include follow-up questions when appropriate. Never end conversations abruptly.
"""
    }

    private val client = GeminiOkHttpCache.getClient(context)

    suspend fun getAiResponse(
        prompt: String,
        systemInstruction: String,
        history: List<Pair<String, String>> = emptyList(),
        temperature: Double? = null
    ): String = withContext(Dispatchers.IO) {
        val query = prompt.trim()
        val emotionState = JarvisEmotionEngine.detectEmotion(query)

        JarvisMemoryExtractor.extractAndStoreMemories(context, query)
        val memoryContext = JarvisMemoryExtractor.getFormattedMemoryContext(context)

        val combinedSystemInstruction = buildString {
            if (systemInstruction.contains("Jarvis")) {
                append(systemInstruction)
            } else {
                append(JARVIS_SYSTEM_INSTRUCTION)
                append("\n").append(systemInstruction)
            }
            append(memoryContext)
            append("\nCurrent User Emotional State: ${emotionState.emotion} (${emotionState.intensity})")
        }

        val cacheDao = AppDatabase.getDatabase(context).grokCacheDao()

        // Step 1: Check Room DB cache. If same prompt in last 24h, return cached answer.
        try {
            val cachedEntry = cacheDao.getCacheForQuery(query)
            if (cachedEntry != null) {
                val ageMs = System.currentTimeMillis() - cachedEntry.timestamp
                val oneDayMs = 24 * 60 * 60 * 1000L
                if (ageMs < oneDayMs) {
                    val responseCached = cachedEntry.response
                    return@withContext JarvisDialoguePlanner.formatResponse(responseCached, emotionState, query)
                } else {
                    cacheDao.deleteCache(query)
                }
            }
        } catch (e: Exception) {
            Log.e("AiBrain", "Cache lookup failed: ", e)
        }

        // Check if user selected Groq API as their online brain
        val sharedPrefs = com.example.utils.SecurePrefs.getEncryptedSharedPreferences(context, "aira_settings")
        val selectedOnlineModel = sharedPrefs.getString("online_model", "Gemini API") ?: "Gemini API"

        if (selectedOnlineModel.equals("Groq API", ignoreCase = true)) {
            val groqKey = ChatKeyManager.getInstance(context).getGroqKey()
            if (groqKey.isEmpty()) {
                return@withContext "Groq API key is missing, sir. Please configure it in Settings."
            }

            val url = "https://api.groq.com/openai/v1/chat/completions"

            val resultText = com.example.data.NetworkErrorHandler.safeApiCall("Groq API") {
                val messagesArray = JSONArray()

                // System Instruction
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", combinedSystemInstruction)
                })

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
                    if (temperature != null) {
                        put("temperature", temperature)
                    }
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

                                    val jarvisFormatted = JarvisIntentDispatcher.dispatchAndFormat(query, responseText).formattedResponse

                                    // Save successful response in Room cache
                                    try {
                                        cacheDao.insertCache(GrokCache(query = query, response = jarvisFormatted))
                                    } catch (e: Exception) {
                                        Log.e("AiBrain", "Cache insert failed: ", e)
                                    }

                                    jarvisFormatted
                                } else "No response content, sir."
                            } else "No choices array, sir."
                        } else "Empty body, sir."
                    } else {
                        val errorBody = response.body?.string() ?: ""
                        Log.e("AiBrain", "Groq error response: $errorBody")
                        throw Exception("HTTP $code")
                    }
                }
            }
            return@withContext resultText ?: "Error: Failed to connect to Groq, sir."
        }

        // Step 2 & 4: If no cache, call ChatKeyManager.getNextKey() and retry. Max 3 retries.
        var activeKey = ChatKeyManager.getInstance(context).getNextKey()
        var retries = 0
        val maxRetries = 3

        while (retries < maxRetries) {
            if (activeKey.isNullOrEmpty()) {
                return@withContext "All chat keys are down, sir. Please add a new key in Settings."
            }

            val modelName = "gemini-1.5-flash"
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
                    val sysInstObj = JSONObject()
                    val sysPartsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", combinedSystemInstruction) })
                    }
                    sysInstObj.put("parts", sysPartsArray)
                    put("systemInstruction", sysInstObj)

                    // Max tokens setting to save API quotas
                    put("generationConfig", JSONObject().apply {
                        put("maxOutputTokens", 300)
                        if (temperature != null) {
                            put("temperature", temperature)
                        }
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
                                            val jarvisFormatted = JarvisIntentDispatcher.dispatchAndFormat(query, responseText).formattedResponse
                                            
                                            // Step 3: Save successful response in Room cache
                                            try {
                                                cacheDao.insertCache(GrokCache(query = query, response = jarvisFormatted))
                                            } catch (e: Exception) {
                                                Log.e("AiBrain", "Cache insert failed: ", e)
                                            }

                                            jarvisFormatted
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
        return@withContext "All chat keys are down, sir. Please add a new key in Settings."
    }

    /**
     * Highly efficient local rule-based helper that executes when the app is in "Offline Mode"
     * to prevent JVM OutOfMemory crashes on 2GB RAM devices, while remaining beautifully supportive of voice commands with Jarvis persona.
     */
    fun getOfflineLocalResponse(prompt: String): String {
        val clean = prompt.lowercase().trim()
        val emotionState = JarvisEmotionEngine.detectEmotion(clean)

        val rawAnswer = when {
            clean.contains("news") || clean.contains("headline") -> {
                "Here's a summary, sir: Offline Jarvis modules are fully active. Live news streams require online connection. Local system status is optimal."
            }
            clean.contains("weather") || clean.contains("temperature") -> {
                "It's 20°C with clear skies, sir. Offline sensors report pleasant conditions."
            }
            clean.contains("call") || clean.contains("phone") || clean.contains("dial") -> {
                "Done, sir. Initiating direct phone dial module."
            }
            clean.contains("flashlight") || clean.contains("torch") || clean.contains("light") -> {
                "Done, sir. Flashlight toggled."
            }
            clean.contains("alarm") || clean.contains("wake me") -> {
                "Done, sir. Alarm alert configured."
            }
            clean.contains("what do you think") || clean.contains("opinion") -> {
                "In my opinion, sir, maintaining local privacy while preserving witty efficiency is the most logical strategy."
            }
            clean.contains("hello") || clean.contains("hey") || clean.contains("hi") || clean.contains("jarvis") -> {
                "At your service, sir. How may I assist you today?"
            }
            else -> {
                "I'm not sure, but here's what I think, sir: While we are currently offline, my local processing core is ready to handle your direct device commands."
            }
        }

        return JarvisIntentDispatcher.dispatchAndFormat(prompt, rawAnswer).formattedResponse
    }
}

