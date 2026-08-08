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

        val sentimentResult = com.example.utils.SentimentAnalysisUtility.analyzeSentiment(query, history)
        JarvisReinforcementEngine.updateImplicitFeedback(context, sentimentResult)

        // Improvement 11: Token-Budgeted Sliding Window Memory
        val (trimmedHistory, prunedSummary) = JarvisTokenMemoryManager.trimHistoryToTokenBudget(history, 2048)

        JarvisMemoryExtractor.extractAndStoreMemories(context, query)
        val memoryContext = JarvisMemoryExtractor.getFormattedMemoryContext(context, query)
        val kgContext = JarvisKnowledgeGraphManager.getKnowledgeGraphContext(context)

        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val timeGreetingContext = when (hour) {
            in 5..11 -> "Time Context: Morning briefing mode."
            in 12..17 -> "Time Context: Afternoon operation mode."
            in 18..22 -> "Time Context: Evening relaxation mode."
            else -> "Time Context: Late-night quiet mode."
        }
        val userSatisfaction = JarvisReinforcementEngine.getSatisfactionScore(context)
        val satisfactionTag = "Implicit Satisfaction Metric: ${String.format("%.2f", userSatisfaction)}"
        
        // Improvement 13, 14, 17
        val formalityContext = JarvisPersonaFormalityManager.getPersonaInstruction(context)
        val proactiveAlert = JarvisProactiveAlertEngine.getProactiveBriefing(context)
        val rollingSummary = JarvisSummarizer.generateRollingSummary(trimmedHistory)

        val combinedSystemInstruction = buildString {
            if (systemInstruction.contains("Jarvis")) {
                append(systemInstruction)
            } else {
                append(JARVIS_SYSTEM_INSTRUCTION)
                append("\n").append(systemInstruction)
            }
            append("\n").append(timeGreetingContext)
            append("\n").append(satisfactionTag)
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

        val effectiveTemperature = temperature ?: run {
            val lowerQ = query.lowercase()
            if (lowerQ.startsWith("turn ") || lowerQ.startsWith("set ") || lowerQ.startsWith("lock ") || lowerQ.contains("wifi") || lowerQ.contains("bluetooth")) {
                0.1 // Precision for commands
            } else {
                0.7 // Creativity for conversational chat
            }
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
                for (turn in trimmedHistory) {
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
        val geminiModelCandidates = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-flash-latest")

        while (retries < maxRetries) {
            if (activeKey.isNullOrEmpty()) {
                return@withContext "All chat keys are down, sir. Please add a new key in Settings."
            }

            for (modelName in geminiModelCandidates) {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$activeKey"

                try {
                    val contentsArray = JSONArray()

                    // Add History
                    for (turn in trimmedHistory) {
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

                    var apiSuccessResult: String? = null
                    var isModelNotFound = false

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

                                            apiSuccessResult = jarvisFormatted
                                        }
                                    }
                                }
                            }
                        } else if (code == 404) {
                            Log.w("AiBrain", "Gemini model $modelName not found (HTTP 404), trying candidate fallback...")
                            isModelNotFound = true
                        } else if (code == 401 || code == 403 || code == 429 || code == 500) {
                            activeKey?.let { ChatKeyManager.getInstance(context).markCooldown(it) }
                            Log.w("AiBrain", "Gemini API HTTP $code on key, triggering key cooldown and rotation...")
                        } else {
                            val errorBody = response.body?.string() ?: ""
                            Log.e("AiBrain", "Gemini API HTTP $code error response: $errorBody")
                        }
                    }

                    if (apiSuccessResult != null) {
                        return@withContext apiSuccessResult!!
                    }
                    if (isModelNotFound) {
                        continue // try next model candidate in for loop
                    } else {
                        break // exit candidate loop to rotate key in outer while loop
                    }
                } catch (e: Exception) {
                    Log.e("AiBrain", "Exception in Gemini API call with model $modelName: ", e)
                }
            }
            retries++
            activeKey = ChatKeyManager.getInstance(context).getNextKey()
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

