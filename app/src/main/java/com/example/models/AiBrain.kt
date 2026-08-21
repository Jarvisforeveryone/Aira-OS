package com.example.models

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.GrokCache
import com.example.data.Memory
import com.example.data.QueryCache
import com.example.network.api.ApiProvider
import com.example.network.api.ProviderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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

    suspend fun detectEmotion(text: String, provider: ApiProvider): EmotionState {
        return try {
            val prompt = """Detect the emotional state from the text: '$text'.
Choose emotion from [HAPPY, SAD, ANGRY, CONFUSED, CURIOSITY, FRUSTRATED, NEUTRAL] and intensity from [HIGH, MEDIUM, LOW].
Respond ONLY in JSON format: {"emotion": "NEUTRAL", "intensity": "MEDIUM"}"""
            val jsonStr = provider.generateResponse(prompt)
            val json = JSONObject(jsonStr.substringAfter("{").substringBeforeLast("}").let { "{$it}" })
            val emotion = UserEmotion.valueOf(json.optString("emotion", "NEUTRAL").uppercase())
            val intensity = EmotionIntensity.valueOf(json.optString("intensity", "MEDIUM").uppercase())
            lastEmotion = emotion
            EmotionState(emotion, intensity)
        } catch (e: Exception) {
            EmotionState(lastEmotion, EmotionIntensity.MEDIUM)
        }
    }
}

object JarvisReinforcementEngine {
    private const val PREFS_NAME = "jarvis_reinforcement_prefs"
    private const val KEY_SATISFACTION = "user_satisfaction_score"

    suspend fun updateImplicitFeedback(context: Context, userFeedback: String, provider: ApiProvider) {
        try {
            val prompt = """Analyze user feedback: '$userFeedback'. Provide satisfaction score adjustment delta (-0.2 to +0.2).
Respond ONLY in JSON format: {"delta": 0.05}"""
            val jsonStr = provider.generateResponse(prompt)
            val json = JSONObject(jsonStr.substringAfter("{").substringBeforeLast("}").let { "{$it}" })
            val delta = json.optDouble("delta", 0.0).toFloat()

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentScore = prefs.getFloat(KEY_SATISFACTION, 0.7f)
            val newScore = (currentScore + delta).coerceIn(0.1f, 1.0f)
            prefs.edit().putFloat(KEY_SATISFACTION, newScore).apply()
        } catch (e: Exception) {
            Log.e("JarvisReinforcementEngine", "Failed LLM feedback analysis", e)
        }
    }

    fun getSatisfactionScore(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_SATISFACTION, 0.7f)
    }
}

object JarvisMemoryExtractor {
    suspend fun extractAndStoreMemories(context: Context, userInput: String, provider: ApiProvider) {
        try {
            val db = AppDatabase.getDatabase(context)
            val memoryDao = db.memoryDao()

            val prompt = """Extract key personal facts, preferences, or identity details from this text: '$userInput'.
Respond ONLY in JSON format: {"facts": [{"factText": "User loves coffee", "category": "Preferences"}]}"""
            val jsonStr = provider.generateResponse(prompt)
            val json = JSONObject(jsonStr.substringAfter("{").substringBeforeLast("}").let { "{$it}" })
            val factsArray = json.optJSONArray("facts") ?: JSONArray()

            val existing = memoryDao.getAllMemoriesList()
            for (i in 0 until factsArray.length()) {
                val item = factsArray.getJSONObject(i)
                val factText = item.optString("factText", "").trim()
                val category = item.optString("category", "General")
                if (factText.isNotBlank() && existing.none { it.factText.equals(factText, ignoreCase = true) }) {
                    memoryDao.insertMemory(
                        Memory(
                            factText = factText,
                            source = "llm_jarvis_brain",
                            category = category,
                            isImportant = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("JarvisMemoryExtractor", "Failed LLM memory extraction", e)
        }
    }

    suspend fun getFormattedMemoryContext(context: Context, query: String = ""): String {
        return try {
            val db = AppDatabase.getDatabase(context)
            val memories = db.memoryDao().getAllMemoriesList()
            if (memories.isEmpty()) return ""

            val facts = memories.take(5).joinToString("; ") { it.factText }
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
    suspend fun getKnowledgeGraphContext(context: Context, provider: ApiProvider): String {
        return try {
            val db = AppDatabase.getDatabase(context)
            val memories = db.memoryDao().getAllMemoriesList()
            if (memories.isEmpty()) return ""

            val rawFacts = memories.take(6).joinToString("; ") { it.factText }
            val prompt = """Extract semantic knowledge graph relationships from these user facts: '$rawFacts'.
Format each as (Entity1)-[RELATION]->(Entity2).
Respond ONLY in JSON format: {"triples": ["(User)-[PREFERS]->(Coffee)"]}"""
            val jsonStr = provider.generateResponse(prompt)
            val json = JSONObject(jsonStr.substringAfter("{").substringBeforeLast("}").let { "{$it}" })
            val array = json.optJSONArray("triples") ?: JSONArray()
            val triplesList = mutableListOf<String>()
            for (i in 0 until array.length()) {
                triplesList.add(array.getString(i))
            }
            if (triplesList.isEmpty()) "" else "\n[Structured Knowledge Graph Triples: ${triplesList.joinToString("; ")}]\n"
        } catch (e: Exception) {
            ""
        }
    }
}

object JarvisProactiveAlertEngine {
    suspend fun getProactiveBriefing(context: Context, provider: ApiProvider): String? {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            val batteryLevel = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            val prompt = """Given system status (Battery: $batteryLevel%), decide if a proactive warning is required.
Respond ONLY in JSON format: {"should_speak": true, "message": "Proactive Alert: Battery level low."}"""
            val jsonStr = provider.generateResponse(prompt)
            val json = JSONObject(jsonStr.substringAfter("{").substringBeforeLast("}").let { "{$it}" })
            if (json.optBoolean("should_speak", false)) {
                val msg = json.optString("message", "")
                if (msg.isNotBlank()) msg else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

object JarvisSummarizer {
    suspend fun generateRollingSummary(history: List<Pair<String, String>>, provider: ApiProvider): String {
        if (history.size < 4) return ""
        val historyText = history.takeLast(6).joinToString("\n") { "User: ${it.first}\nAI: ${it.second}" }
        val prompt = "Summarize the recent conversation briefly in 1-2 sentences:\n$historyText"
        val summary = provider.generateResponse(prompt).trim()
        return if (summary.isNotBlank()) "\n[Hierarchical Rolling Summary: $summary]\n" else ""
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

        return PartitionedOutput(
            displayContent = rawDisplay,
            speechContent = rawDisplay,
            priority = SpeechPriority.NORMAL,
            speechSpeed = 1.0f,
            shouldSpeak = rawDisplay.isNotBlank()
        )
    }

    suspend fun analyzeAndPartitionWithLlm(
        fullResponse: String,
        userQuery: String,
        context: Context,
        provider: ApiProvider
    ): PartitionedOutput {
        val rawDisplay = fullResponse.trim()
        try {
            val prompt = """Analyze query and response for speech priority, speed (0.8 to 1.3), and spoken text.
User Query: '$userQuery'
Response: '$rawDisplay'
Respond ONLY in JSON format: {"priority": "NORMAL", "speed": 1.0, "spokenText": "$rawDisplay"}"""
            val jsonStr = provider.generateResponse(prompt)
            val json = JSONObject(jsonStr.substringAfter("{").substringBeforeLast("}").let { "{$it}" })

            val priority = try {
                SpeechPriority.valueOf(json.optString("priority", "NORMAL").uppercase())
            } catch (e: Exception) {
                SpeechPriority.NORMAL
            }
            val speed = json.optDouble("speed", 1.0).toFloat()
            val spokenText = json.optString("spokenText", rawDisplay)

            return PartitionedOutput(
                displayContent = rawDisplay,
                speechContent = spokenText,
                priority = priority,
                speechSpeed = speed,
                shouldSpeak = priority != SpeechPriority.SILENT && spokenText.isNotBlank()
            )
        } catch (e: Exception) {
            return analyzeAndPartition(fullResponse, userQuery, context)
        }
    }
}

object JarvisOutputPartitioner {
    fun partition(
        fullResponse: String,
        userQuery: String = "",
        context: Context? = null
    ): PartitionedOutput {
        return BrainSpeechStrategyEngine.analyzeAndPartition(fullResponse, userQuery, context)
    }

    suspend fun partitionWithLlm(
        fullResponse: String,
        userQuery: String,
        context: Context,
        provider: ApiProvider
    ): PartitionedOutput {
        return BrainSpeechStrategyEngine.analyzeAndPartitionWithLlm(fullResponse, userQuery, context, provider)
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
            PersonaFormality.CONCISE.name -> "\n[Formality Setting: CONCISE - Respond with extreme brevity. Maximum 1-2 sentences.]"
            PersonaFormality.PLAYFUL.name -> "\n[Formality Setting: PLAYFUL - Respond with witty banter and energetic tone.]"
            PersonaFormality.TECHNICAL.name -> "\n[Formality Setting: TECHNICAL - Use precise, analytical terminology.]"
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
    val formattedResponse: String
)

object JarvisIntentDispatcher {

    suspend fun dispatchAndFormat(
        userInput: String,
        rawResponse: String,
        provider: ApiProvider
    ): DispatchedIntentResult {
        return try {
            val prompt = """Classify the intent of user input: '$userInput'.
Options: [NEWS, WEATHER, DEVICE_COMMAND, QUESTION_OPINION, GENERAL_CONVERSATION].
Respond ONLY in JSON format: {"intent": "GENERAL_CONVERSATION"}"""
            val jsonStr = provider.generateResponse(prompt)
            val json = JSONObject(jsonStr.substringAfter("{").substringBeforeLast("}").let { "{$it}" })
            val intent = JarvisIntent.valueOf(json.optString("intent", "GENERAL_CONVERSATION").uppercase())
            DispatchedIntentResult(intent, rawResponse.trim())
        } catch (e: Exception) {
            DispatchedIntentResult(JarvisIntent.GENERAL_CONVERSATION, rawResponse.trim())
        }
    }
}

class AiBrain(private val context: Context) {

    companion object {
        const val JARVIS_SYSTEM_INSTRUCTION = """You are Jarvis — witty, confident, helpful, and slightly British.
Always address the user with conversational, polite terms like 'sir', 'at your service', or 'right away, sir'."""
    }

    private val providerManager = ProviderManager(context)

    suspend fun getAiResponse(
        prompt: String,
        systemInstruction: String,
        history: List<Pair<String, String>> = emptyList(),
        temperature: Double? = null
    ): String = withContext(Dispatchers.IO) {
        val query = prompt.trim()
        val provider = providerManager.getProvider()

        val emotionState = JarvisEmotionEngine.detectEmotion(query, provider)
        JarvisReinforcementEngine.updateImplicitFeedback(context, query, provider)
        JarvisMemoryExtractor.extractAndStoreMemories(context, query, provider)

        val (trimmedHistory, prunedSummary) = JarvisTokenMemoryManager.trimHistoryToTokenBudget(history, 2048)
        val memoryContext = JarvisMemoryExtractor.getFormattedMemoryContext(context, query)
        val kgContext = JarvisKnowledgeGraphManager.getKnowledgeGraphContext(context, provider)
        val formalityContext = JarvisPersonaFormalityManager.getPersonaInstruction(context)
        val proactiveAlert = JarvisProactiveAlertEngine.getProactiveBriefing(context, provider)
        val rollingSummary = JarvisSummarizer.generateRollingSummary(trimmedHistory, provider)

        val combinedSystemInstruction = buildString {
            append(JARVIS_SYSTEM_INSTRUCTION)
            if (systemInstruction.isNotBlank()) {
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

        // Check Room local cache layer first (normalized query + hit tracking)
        val db = AppDatabase.getDatabase(context)
        val queryCacheDao = db.queryCacheDao()
        val grokCacheDao = db.grokCacheDao()
        val normalized = QueryCache.normalize(query)

        try {
            val cachedQuery = queryCacheDao.getCacheForQuery(normalized)
            if (cachedQuery != null && !cachedQuery.isExpired()) {
                queryCacheDao.incrementHitCount(normalized, System.currentTimeMillis())
                return@withContext cachedQuery.response
            }

            val cachedEntry = grokCacheDao.getCacheForQuery(query)
            if (cachedEntry != null) {
                val ageMs = System.currentTimeMillis() - cachedEntry.timestamp
                val oneDayMs = 24 * 60 * 60 * 1000L
                if (ageMs < oneDayMs) {
                    return@withContext cachedEntry.response
                } else {
                    grokCacheDao.deleteCache(query)
                }
            }
        } catch (e: Exception) {
            Log.e("AiBrain", "Cache lookup failed: ", e)
        }

        // Execute query via ApiProvider (LLM) with prioritized fallback chain: Gemini -> Groq -> OpenAI -> Claude -> OpenRouter
        val fallbackChain = providerManager.getFallbackChain()
        var response = ""
        var successfulProvider: ApiProvider? = null

        for (candidateProvider in fallbackChain) {
            try {
                Log.d("AiBrain", "Executing query via provider: ${candidateProvider.javaClass.simpleName}")
                val candidateResponse = candidateProvider.generateResponse(query, combinedSystemInstruction).trim()
                if (candidateResponse.isNotBlank()) {
                    response = candidateResponse
                    successfulProvider = candidateProvider
                    Log.d("AiBrain", "Query succeeded via ${candidateProvider.javaClass.simpleName}")
                    break
                }
            } catch (e: Exception) {
                Log.w("AiBrain", "Provider ${candidateProvider.javaClass.simpleName} failed: ${e.message}. Attempting next in chain...")
            }
        }

        if (response.isNotBlank() && successfulProvider != null) {
            try {
                queryCacheDao.insertCache(
                    QueryCache(
                        normalizedQuery = normalized,
                        originalQuery = query,
                        response = response,
                        provider = successfulProvider.javaClass.simpleName,
                        hitCount = 1,
                        timestamp = System.currentTimeMillis(),
                        lastAccessed = System.currentTimeMillis()
                    )
                )
                grokCacheDao.insertCache(GrokCache(query = query, response = response))
            } catch (e: Exception) {
                Log.e("AiBrain", "Cache insert failed: ", e)
            }
            return@withContext response
        }

        return@withContext getOfflineLocalResponse(query)
    }

    fun getOfflineLocalResponse(prompt: String): String {
        return "I am operating offline at your service, sir. All core local systems are ready."
    }
}
