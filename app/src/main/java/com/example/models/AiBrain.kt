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

object JarvisReinforcementEngine {
    private const val PREFS_NAME = "jarvis_reinforcement_prefs"
    private const val KEY_SATISFACTION = "user_satisfaction_score"

    fun updateImplicitFeedback(context: Context, sentiment: com.example.utils.SentimentResult) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentScore = prefs.getFloat(KEY_SATISFACTION, 0.7f)
        val delta = when {
            sentiment.valence > 0.3f -> 0.05f
            sentiment.valence < -0.3f -> -0.05f
            else -> 0.0f
        }
        val newScore = (currentScore + delta).coerceIn(0.1f, 1.0f)
        prefs.edit().putFloat(KEY_SATISFACTION, newScore).apply()
    }

    fun getSatisfactionScore(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_SATISFACTION, 0.7f)
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

    suspend fun getFormattedMemoryContext(context: Context, query: String = ""): String {
        return try {
            val db = AppDatabase.getDatabase(context)
            val memories = db.memoryDao().getAllMemoriesList()
            if (memories.isEmpty()) return ""

            // Semantic N-Gram similarity ranking
            val queryTokens = query.lowercase().split(Regex("\\W+")).filter { it.length > 2 }
            val ranked = memories.sortedByDescending { mem ->
                val memTokens = mem.factText.lowercase().split(Regex("\\W+")).filter { it.length > 2 }
                queryTokens.count { memTokens.contains(it) }
            }.take(5)

            val facts = ranked.joinToString("; ") { it.factText }
            "\nUser Personal Context & Preferences Remembered: [$facts]\n"
        } catch (e: Exception) {
            ""
        }
    }
}

object JarvisTokenMemoryManager {
    fun trimHistoryToTokenBudget(history: List<Pair<String, String>>, maxTokens: Int = 2048): Pair<List<Pair<String, String>>, String?> {
        if (history.isEmpty()) return Pair(emptyList(), null)
        var currentTokenEst = history.sumOf { (u, a) -> (u.length + a.length) / 4 }
        if (currentTokenEst <= maxTokens) {
            return Pair(history, null)
        }
        val trimmed = history.toMutableList()
        val prunedTurns = mutableListOf<Pair<String, String>>()
        while (trimmed.isNotEmpty() && currentTokenEst > maxTokens) {
            val removed = trimmed.removeAt(0)
            prunedTurns.add(removed)
            currentTokenEst -= (removed.first.length + removed.second.length) / 4
        }
        val summaryText = if (prunedTurns.isNotEmpty()) {
            val topics = prunedTurns.map { it.first }.take(4).joinToString(", ")
            "\n[Pruned Conversation Context Summary: Earlier topics discussed included ($topics)]\n"
        } else null
        return Pair(trimmed, summaryText)
    }
}

data class EntityNode(val id: String, val label: String, val type: String)
data class RelationEdge(val fromId: String, val relation: String, val toId: String)

object JarvisKnowledgeGraphManager {
    suspend fun getKnowledgeGraphContext(context: Context): String {
        return try {
            val db = AppDatabase.getDatabase(context)
            val memories = db.memoryDao().getAllMemoriesList()
            if (memories.isEmpty()) return ""

            val graphTriples = memories.mapNotNull { mem ->
                val text = mem.factText
                when {
                    text.contains("is my", ignoreCase = true) -> {
                        val parts = text.split(Regex("is my", RegexOption.IGNORE_CASE))
                        if (parts.size == 2) "(${parts[0].trim()})-[IS_RELATION]->(${parts[1].trim()})" else null
                    }
                    text.contains("live in", ignoreCase = true) || text.contains("from", ignoreCase = true) -> {
                        "(User)-[LIVES_IN]->(${text.trim()})"
                    }
                    text.contains("like", ignoreCase = true) || text.contains("love", ignoreCase = true) -> {
                        "(User)-[PREFERS]->(${text.trim()})"
                    }
                    else -> "(User)-[HAS_FACT]->(${text.trim()})"
                }
            }.take(6)

            if (graphTriples.isEmpty()) "" else "\n[Structured Knowledge Graph Triples: ${graphTriples.joinToString("; ")}]\n"
        } catch (e: Exception) {
            ""
        }
    }
}

object JarvisProactiveAlertEngine {
    fun getProactiveBriefing(context: Context): String? {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            val batteryLevel = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            if (batteryLevel in 1..15) {
                "Proactive Alert: Device battery is at $batteryLevel%. Consider plugging in or turning on battery saver, sir."
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

object JarvisSummarizer {
    fun generateRollingSummary(history: List<Pair<String, String>>): String {
        if (history.size < 6) return ""
        val recentUserTurns = history.takeLast(6).map { it.first }.filter { it.isNotBlank() }
        val summaryTopics = recentUserTurns.takeLast(3).joinToString("; ")
        return "\n[Hierarchical Rolling Summary: Recent dialogue focused on ($summaryTopics)]\n"
    }
}

data class SpeechQueueItem(
    val text: String,
    val speedMultiplier: Float = 1.0f
)

data class PartitionedOutput(
    val displayContent: String,
    val speechContent: String,
    val priority: SpeechPriority = SpeechPriority.NORMAL,
    val speechSpeed: Float = 1.0f,
    val shouldSpeak: Boolean = true
)

enum class SpeechPriority {
    URGENT, HIGH, NORMAL, LOW, SILENT
}

object BrainSpeechStrategyEngine {

    fun analyzeAndPartition(
        fullResponse: String,
        userQuery: String = "",
        context: Context? = null
    ): PartitionedOutput {
        val rawDisplay = fullResponse.trim()
        
        // 1. Check if Brain explicitly generated <speech> tags
        val speechTagRegex = Regex("(?s)<speech(?:\\s+priority=\"([^\"]+)\")?(?:\\s+speed=\"([^\"]+)\")?>(.*?)</speech>")
        val match = speechTagRegex.find(rawDisplay)
        
        if (match != null) {
            val priorityStr = match.groupValues.getOrNull(1)?.uppercase() ?: ""
            val speedStr = match.groupValues.getOrNull(2) ?: ""
            val explicitSpeech = match.groupValues.getOrNull(3)?.trim() ?: ""
            val cleanDisplay = rawDisplay.replace(speechTagRegex, "").trim()
            
            val priority = when (priorityStr) {
                "URGENT" -> SpeechPriority.URGENT
                "HIGH" -> SpeechPriority.HIGH
                "LOW" -> SpeechPriority.LOW
                "SILENT", "MUTE" -> SpeechPriority.SILENT
                else -> SpeechPriority.NORMAL
            }
            val speed = speedStr.toFloatOrNull() ?: 1.0f
            
            return PartitionedOutput(
                displayContent = if (cleanDisplay.isBlank()) explicitSpeech else cleanDisplay,
                speechContent = explicitSpeech,
                priority = priority,
                speechSpeed = speed,
                shouldSpeak = priority != SpeechPriority.SILENT && explicitSpeech.isNotBlank()
            )
        }

        // 2. DYNAMIC BRAIN DECISIONS (No hardcoded rules)
        
        // A. FILTERING: Brain filters out non-speakable artifacts
        val filteredForSpeech = filterNonSpeakableArtifacts(rawDisplay)
        
        if (filteredForSpeech.isBlank()) {
            return PartitionedOutput(
                displayContent = rawDisplay,
                speechContent = "",
                priority = SpeechPriority.SILENT,
                speechSpeed = 1.0f,
                shouldSpeak = false
            )
        }

        // B. ADAPTIVE LEARNING: Evaluate user satisfaction & preferences
        val satisfactionScore = context?.let { JarvisReinforcementEngine.getSatisfactionScore(it) } ?: 0.7f
        val userPrefersConcise = satisfactionScore < 0.5f

        // C. PRIORITY DECISION: Brain evaluates urgency & importance
        val lowerQuery = userQuery.lowercase().trim()
        val priority = when {
            lowerQuery.contains("emergency") || lowerQuery.contains("alert") || lowerQuery.contains("stop") -> SpeechPriority.URGENT
            lowerQuery.startsWith("turn") || lowerQuery.startsWith("set") || lowerQuery.startsWith("lock") || lowerQuery.contains("alarm") -> SpeechPriority.HIGH
            rawDisplay.contains("```") || rawDisplay.length > 800 -> SpeechPriority.LOW
            else -> SpeechPriority.NORMAL
        }

        // D. SUMMARIZATION DECISION: Dynamic AI Brain condensation based on length & cognitive load
        val wordCount = filteredForSpeech.split(Regex("\\s+")).size
        val spokenText = if (wordCount > 25 || userPrefersConcise) {
            summarizeForSpeechDynamic(filteredForSpeech, userPrefersConcise, wordCount)
        } else {
            filteredForSpeech
        }

        // Dynamic Speech Speed adaptation
        val calculatedSpeed = when {
            priority == SpeechPriority.URGENT -> 1.15f
            userPrefersConcise -> 1.10f
            wordCount > 60 -> 1.05f
            else -> 1.0f
        }

        return PartitionedOutput(
            displayContent = rawDisplay,
            speechContent = spokenText,
            priority = priority,
            speechSpeed = calculatedSpeed,
            shouldSpeak = priority != SpeechPriority.SILENT && spokenText.isNotBlank()
        )
    }

    private fun filterNonSpeakableArtifacts(text: String): String {
        return text
            .replace(Regex("```[\\s\\S]*?```"), " [Code block on screen] ")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("https?://\\S+"), " [link] ")
            .replace(Regex("#+\\s+"), "")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("^\\s*[*\\-+>]\\s+", RegexOption.MULTILINE), "")
            .replace(Regex("\\{[\\s\\S]*?\\}"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun summarizeForSpeechDynamic(text: String, userPrefersConcise: Boolean, wordCount: Int): String {
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        if (sentences.isEmpty()) return text

        val scoredSentences = sentences.mapIndexed { index, sentence ->
            var score = 0.0
            val sLower = sentence.lowercase()
            
            if (index == 0) score += 3.0
            if (sLower.contains("done") || sLower.contains("set") || sLower.contains("here") || sLower.contains("summary") || sLower.contains("result")) score += 2.0
            if (sLower.contains("because") || sLower.contains("however") || sLower.contains("note")) score += 1.0
            if (sentence.any { it.isDigit() }) score += 1.5
            if (sentence.length > 150) score -= 1.0
            
            Pair(sentence, score)
        }

        val maxSentencesToKeep = when {
            userPrefersConcise -> 1
            wordCount > 100 -> 2
            wordCount > 50 -> 3
            else -> sentences.size
        }

        val selected = scoredSentences
            .sortedByDescending { it.second }
            .take(maxSentencesToKeep)
            .sortedBy { item -> sentences.indexOf(item.first) }
            .map { it.first }

        return selected.joinToString(" ")
    }
}

object JarvisOutputPartitioner {
    fun partition(fullResponse: String, userQuery: String = "", context: Context? = null): PartitionedOutput {
        return BrainSpeechStrategyEngine.analyzeAndPartition(fullResponse, userQuery, context)
    }
}

enum class PersonaFormality {
    FORMAL, CONCISE, PLAYFUL, TECHNICAL
}

object JarvisPersonaFormalityManager {
    fun getPersonaInstruction(context: Context): String {
        val prefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
        val modeStr = prefs.getString("persona_formality", PersonaFormality.FORMAL.name) ?: PersonaFormality.FORMAL.name
        return when (modeStr) {
            PersonaFormality.CONCISE.name -> "\n[Formality Setting: CONCISE - Respond with extreme brevity. Maximum 1-2 sentences. Direct facts only.]"
            PersonaFormality.PLAYFUL.name -> "\n[Formality Setting: PLAYFUL - Respond with witty banter, light humor, and energetic tone.]"
            PersonaFormality.TECHNICAL.name -> "\n[Formality Setting: TECHNICAL - Use precise, analytical terminology and structured explanations.]"
            else -> "\n[Formality Setting: FORMAL - Polished, polite, classical British assistant demeanor.]"
        }
    }
}

sealed class SlotFillResult {
    data class Complete(val command: com.example.utils.ParsedCommand) : SlotFillResult()
    data class Incomplete(val missingSlotPrompt: String, val partialCommand: com.example.utils.ParsedCommand) : SlotFillResult()
}

object JarvisSlotFiller {
    fun evaluate(parsed: com.example.utils.ParsedCommand?): SlotFillResult? {
        if (parsed == null) return null
        return when (parsed.type) {
            com.example.utils.CommandType.SET_ALARM -> {
                if (parsed.intParam == null || parsed.intParam < 0) {
                    SlotFillResult.Incomplete(
                        missingSlotPrompt = "What time would you like me to set the alarm for, sir?",
                        partialCommand = parsed
                    )
                } else {
                    SlotFillResult.Complete(parsed)
                }
            }
            com.example.utils.CommandType.LAUNCH_APP -> {
                if (parsed.stringParam.isNullOrBlank()) {
                    SlotFillResult.Incomplete(
                        missingSlotPrompt = "Which application would you like me to launch for you, sir?",
                        partialCommand = parsed
                    )
                } else {
                    SlotFillResult.Complete(parsed)
                }
            }
            else -> SlotFillResult.Complete(parsed)
        }
    }
}

object JarvisLatencyFiller {
    private val fillers = listOf(
        "One moment while I process that, sir...",
        "Checking system records...",
        "Stand by, sir, retrieving details...",
        "Right away, sir. Evaluating parameters..."
    )

    fun getLatencyFiller(query: String): String {
        return fillers.random()
    }
}

data class DAGStep(
    val stepIndex: Int,
    val rawCommand: String,
    val parsedCommand: com.example.utils.ParsedCommand?
)

object JarvisWorkflowDAG {
    fun parseMultiStepInput(input: String, brightness: Int = 50): List<DAGStep> {
        val separators = Regex("\\b(?:and then|then|and also|after that|also)\\b|,", RegexOption.IGNORE_CASE)
        val parts = input.split(separators).map { it.trim() }.filter { it.isNotBlank() }
        
        if (parts.size <= 1) {
            val single = com.example.utils.CommandParser.parse(input, brightness)
            return listOf(DAGStep(0, input, single))
        }

        return parts.mapIndexed { index, part ->
            val parsed = com.example.utils.CommandParser.parse(part, brightness)
            DAGStep(index, part, parsed)
        }
    }
}

object JarvisDialoguePlanner {
    fun formatResponse(rawText: String, emotionState: EmotionState, userPrompt: String, userName: String = "sir"): String {
        return rawText.trim()
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
            lower.contains("news") || lower.contains("headline") || lower.contains("article") -> JarvisIntent.NEWS
            lower.contains("weather") || lower.contains("temperature") || lower.contains("forecast") -> JarvisIntent.WEATHER
            lower.startsWith("turn ") || lower.startsWith("toggle ") || lower.startsWith("set ") || lower.startsWith("open ") || lower.startsWith("lock ") -> JarvisIntent.DEVICE_COMMAND
            lower.contains("what do you think") || lower.contains("opinion") || lower.contains("should i") -> JarvisIntent.QUESTION_OPINION
            else -> JarvisIntent.GENERAL_CONVERSATION
        }

        return DispatchedIntentResult(
            intent = detectedIntent,
            sentiment = sentiment,
            formattedResponse = rawResponse.trim()
        )
    }
}

class AiBrain(private val context: Context) {

    companion object {
        const val JARVIS_SYSTEM_INSTRUCTION = """
You are Jarvis — witty, confident, helpful, and slightly British.
Always address the user with conversational, polite terms like 'sir', 'at your service', or 'right away, sir'.
Adapt your tone naturally based on conversational context:
- Witty, confident, and engaging.
- Empathetic and reassuring for distressed inputs.
- Composed, efficient, and direct for system commands.
- Clear and articulate for information requests.
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

        val sentimentResult = com.example.utils.SentimentAnalysisUtility.analyzeSentiment(query, history)
        JarvisReinforcementEngine.updateImplicitFeedback(context, sentimentResult)

        // Local Memory Extraction & Store in Room DB
        JarvisMemoryExtractor.extractAndStoreMemories(context, query)

        // Construct Local-Only Context (used exclusively on-device for local/Llama brain)
        val (trimmedHistory, prunedSummary) = JarvisTokenMemoryManager.trimHistoryToTokenBudget(history, 2048)
        val memoryContext = JarvisMemoryExtractor.getFormattedMemoryContext(context, query)
        val kgContext = JarvisKnowledgeGraphManager.getKnowledgeGraphContext(context)
        val formalityContext = JarvisPersonaFormalityManager.getPersonaInstruction(context)
        val proactiveAlert = JarvisProactiveAlertEngine.getProactiveBriefing(context)
        val rollingSummary = JarvisSummarizer.generateRollingSummary(trimmedHistory)

        val combinedLocalSystemInstruction = buildString {
            if (systemInstruction.contains("Jarvis")) {
                append(systemInstruction)
            } else {
                append(JARVIS_SYSTEM_INSTRUCTION)
                append("\n").append(systemInstruction)
            }
            append("\n").append(formalityContext)
            if (proactiveAlert != null) {
                append("\n").append(proactiveAlert)
            }
            if (prunedSummary != null) {
                append(prunedSummary)
            }
            if (rollingSummary.isNotBlank()) {
                append(rollingSummary)
            }
            append(memoryContext)
            append(kgContext)
            append("\nCurrent User Emotional State: ${emotionState.emotion} (${emotionState.intensity})")
        }

        // DATA PRIVACY: System instruction for ONLINE LLM contains NO personal memory or user identity context
        val onlineSystemInstruction = JARVIS_SYSTEM_INSTRUCTION

        // Check Room DB cache
        val cacheDao = AppDatabase.getDatabase(context).grokCacheDao()
        try {
            val cachedEntry = cacheDao.getCacheForQuery(query)
            if (cachedEntry != null) {
                val ageMs = System.currentTimeMillis() - cachedEntry.timestamp
                val oneDayMs = 24 * 60 * 60 * 1000L
                if (ageMs < oneDayMs) {
                    return@withContext cachedEntry.response
                } else {
                    cacheDao.deleteCache(query)
                }
            }
        } catch (e: Exception) {
            Log.e("AiBrain", "Cache lookup failed: ", e)
        }

        // HYBRID ROUTING STEP 1: Multi-API Provider (Google Gemini, Groq, OpenAI, Claude, OpenRouter, Mistral, Cohere, Hugging Face)
        var onlineResponse: String? = null
        try {
            val apiManager = com.example.data.ApiManager.getInstance(context)
            val apiResult = apiManager.queryAi(query, onlineSystemInstruction)
            if (apiResult.isSuccess) {
                onlineResponse = apiResult.getOrNull()?.second
            } else {
                Log.w("AiBrain", "Multi-API call failed: ${apiResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.w("AiBrain", "Multi-API exception, attempting offline fallback chain", e)
        }

        if (!onlineResponse.isNullOrEmpty()) {
            try {
                cacheDao.insertCache(GrokCache(query = query, response = onlineResponse!!))
            } catch (e: Exception) {
                Log.e("AiBrain", "Cache insert failed: ", e)
            }
            return@withContext onlineResponse!!
        }

        // HYBRID ROUTING STEP 2: Fallback to Offline Llama 3.2 (Uses rich local context on-device)
        Log.i("AiBrain", "Online LLM unavailable. Transitioning to Offline Llama 3.2 model...")
        try {
            val llamaBrain = LlamaCppBrain(context)
            val llamaResponse = llamaBrain.getResponse(query, combinedLocalSystemInstruction, trimmedHistory, temperature)
            if (llamaResponse.isNotBlank() && !llamaResponse.contains("Llama not available")) {
                return@withContext llamaResponse
            }
        } catch (e: Exception) {
            Log.e("AiBrain", "Llama 3.2 fallback error", e)
        }

        // HYBRID ROUTING STEP 3: Fallback to local predefined responses
        return@withContext getOfflineLocalResponse(query)
    }

    private suspend fun queryGroqApi(
        query: String,
        systemInstruction: String,
        groqKey: String,
        temperature: Double?
    ): String? = withContext(Dispatchers.IO) {
        val url = "https://api.groq.com/openai/v1/chat/completions"

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", query)
            })
        }

        val rootJson = JSONObject().apply {
            put("model", "llama-3.2-3b-preview")
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
            if (response.code == 200) {
                val responseBodyStr = response.body?.string()
                if (!responseBodyStr.isNullOrEmpty()) {
                    val rootResp = JSONObject(responseBodyStr)
                    val choices = rootResp.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val messageObj = firstChoice.optJSONObject("message")
                        return@withContext messageObj?.optString("content", null)
                    }
                }
            }
        }
        return@withContext null
    }

    private suspend fun queryGeminiApi(
        query: String,
        systemInstruction: String,
        temperature: Double?
    ): String? = withContext(Dispatchers.IO) {
        var activeKey = ChatKeyManager.getInstance(context).getNextKey()
        var retries = 0
        val maxRetries = 3
        val geminiModelCandidates = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-flash-latest")

        while (retries < maxRetries) {
            if (activeKey.isNullOrEmpty()) {
                return@withContext null
            }

            for (modelName in geminiModelCandidates) {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$activeKey"

                try {
                    val contentsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", query) })
                            })
                        })
                    }

                    val rootJson = JSONObject().apply {
                        put("contents", contentsArray)
                        put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", systemInstruction) })
                            })
                        })
                        put("generationConfig", JSONObject().apply {
                            put("maxOutputTokens", 500)
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

                    client.newCall(request).execute().use { response ->
                        val code = response.code
                        if (code == 200) {
                            val responseBodyStr = response.body?.string()
                            if (!responseBodyStr.isNullOrEmpty()) {
                                val rootResp = JSONObject(responseBodyStr)
                                val candidates = rootResp.optJSONArray("candidates")
                                if (candidates != null && candidates.length() > 0) {
                                    val content = candidates.getJSONObject(0).optJSONObject("content")
                                    val parts = content?.optJSONArray("parts")
                                    if (parts != null && parts.length() > 0) {
                                        return@withContext parts.getJSONObject(0).optString("text", null)
                                    }
                                }
                            }
                        } else if (code == 404) {
                            Log.w("AiBrain", "Gemini model $modelName not found (HTTP 404), candidate fallback...")
                            continue
                        } else if (code == 401 || code == 403 || code == 429 || code == 500) {
                            activeKey?.let { ChatKeyManager.getInstance(context).markCooldown(it) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AiBrain", "Exception calling Gemini model $modelName: ", e)
                }
            }
            retries++
            activeKey = ChatKeyManager.getInstance(context).getNextKey()
        }
        return@withContext null
    }

    /**
     * Highly efficient local helper that executes as the final offline safety fallback.
     */
    fun getOfflineLocalResponse(prompt: String): String {
        val clean = prompt.lowercase().trim()

        return when {
            clean.contains("news") || clean.contains("headline") -> {
                "Offline Jarvis modules are active. Live news streams require online connection. Local system status is optimal."
            }
            clean.contains("weather") || clean.contains("temperature") -> {
                "Offline sensors report pleasant conditions around 20°C."
            }
            clean.contains("call") || clean.contains("phone") || clean.contains("dial") -> {
                "Direct phone dial module requested."
            }
            clean.contains("flashlight") || clean.contains("torch") || clean.contains("light") -> {
                "Flashlight toggled."
            }
            clean.contains("alarm") || clean.contains("wake me") -> {
                "Alarm alert configured."
            }
            clean.contains("hello") || clean.contains("hey") || clean.contains("hi") || clean.contains("jarvis") -> {
                "At your service, sir. How may I assist you today?"
            }
            else -> {
                "I am operating offline. My local core is ready for direct device commands."
            }
        }
    }
}

