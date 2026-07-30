package com.example.data

import android.content.Context
import android.media.AudioManager
import com.example.utils.CommandType
import com.example.utils.ParsedCommand
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * AIRA PREDEFINED RESPONSES & ASSISTANT INTELLIGENCE REPOSITORY
 * Centralized, highly expandable system for predefined responses, device commands,
 * fun interaction dialogues, processing phrases, and intelligent fallback handling.
 */
enum class ResponseCategory {
    TIME_AND_DATE,
    WEATHER,
    DEVICE_CONTROL,
    ASSISTANT_IDENTITY,
    FUN_AND_CHAT,
    GREETING,
    GRATITUDE,
    SYSTEM_STATUS,
    FALLBACK
}

data class PredefinedResponse(
    val textResponse: String,
    val spokenResponse: String = textResponse,
    val commandType: CommandType? = null,
    val parsedCommand: ParsedCommand? = null,
    val category: ResponseCategory = ResponseCategory.FUN_AND_CHAT
)

data class PredefinedCommandRule(
    val id: String,
    val keywords: List<String>,
    val regexPatterns: List<Regex> = emptyList(),
    val category: ResponseCategory,
    val generator: (context: Context, input: String, params: Map<String, Any?>) -> PredefinedResponse
)

object AiraPredefinedResponses {

    private val customRules = mutableListOf<PredefinedCommandRule>()

    // --- 1. FUN & ENGAGING PROCESSING PHRASES ---
    private val processingPhrases = listOf(
        "Consulting local neural pathways...",
        "Analyzing your request...",
        "Crunching bytes for you...",
        "Scanning system telemetry...",
        "On it, boss! One moment...",
        "Processing with local AI brain...",
        "Engaging assistant sub-routines...",
        "Querying core database..."
    )

    // --- 2. FUN & HELPFUL FALLBACK RESPONSES (WHEN AIRA DOESN'T UNDERSTAND) ---
    private val fallbackResponses = listOf(
        "I'm not quite sure about that one yet, but I can check the time, weather, or control your WiFi & Flashlight!",
        "My neural circuits missed that phrase. Try asking 'What time is it?' or 'Turn on WiFi'!",
        "I didn't catch that command, but I'm all ears! Ask me 'What can you do?' to see my features.",
        "Hmm, I'm still learning that phrase! Try asking me about the weather, setting an alarm, or toggling Bluetooth.",
        "I couldn't process that request clearly. Say 'Help' or ask me 'Status report' for system insights!",
        "That's a new one for me! Try commands like 'Turn on Flashlight', 'What's the weather?', or 'Tell me a joke'."
    )

    // --- 3. JOKES & HUMOR ---
    private val jokes = listOf(
        "Why do programmers prefer dark mode? Because light attracts bugs!",
        "There are 10 types of people in the world: those who understand binary, and those who don't.",
        "Why was the cell phone wearing glasses? Because it lost its contacts!",
        "An SQL query walks into a bar, walks up to two tables and asks... 'Can I join you?'",
        "Why did the smartphone get a job? Because it had too many applications!",
        "Hardware: The part of a computer that you can kick when the software crashes."
    )

    // --- 4. GREETINGS ---
    private val morningGreetings = listOf(
        "Good morning! Hope you have a productive day ahead. How can I assist you?",
        "Top of the morning! All systems are online and ready for your commands.",
        "Good morning! Weather and system diagnostics are updated. What's on your agenda?"
    )

    private val afternoonGreetings = listOf(
        "Good afternoon! How is your day going? I'm standing by to help.",
        "Good afternoon! Neural engine running smoothly. How can I assist?",
        "Hello there! Ready when you are."
    )

    private val eveningGreetings = listOf(
        "Good evening! Ready to wind down or tackle remaining tasks?",
        "Good evening! I'm here if you need alarms set, weather checked, or notes saved.",
        "Good evening! All system parameters normal. What can I do for you?"
    )

    private val generalGreetings = listOf(
        "Hello! I am Aira, your personal AI assistant. How can I help you today?",
        "Hey there! Ready to assist with device controls, questions, or reminders.",
        "Greetings! How can I make your day easier?",
        "Aira at your service. What's on your mind?"
    )

    // --- 5. REGISTERED COMMAND RULES ---
    private val builtInRules: List<PredefinedCommandRule> = listOf(

        // TIME COMMANDS
        PredefinedCommandRule(
            id = "time_query",
            keywords = listOf("time", "what time", "current time", "tell me the time", "clock"),
            regexPatterns = listOf(
                Regex("""(?i)\b(what('s|\s+is)?\s+the\s+time|what\s+time\s+is\s+it|tell\s+me\s+the\s+time|current\s+time)\b""")
            ),
            category = ResponseCategory.TIME_AND_DATE,
            generator = { _, _, _ ->
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val currentTime = timeFormat.format(Date())
                val phrase = listOf(
                    "The current time is $currentTime.",
                    "It's currently $currentTime.",
                    "Looking at the clock, it is $currentTime.",
                    "It is $currentTime right now."
                ).random()
                PredefinedResponse(textResponse = phrase, category = ResponseCategory.TIME_AND_DATE)
            }
        ),

        // DATE COMMANDS
        PredefinedCommandRule(
            id = "date_query",
            keywords = listOf("date", "today's date", "what date", "what day", "day is it"),
            regexPatterns = listOf(
                Regex("""(?i)\b(what('s|\s+is)?\s+(today'?s\s+)?date|what\s+day\s+is\s+it|current\s+date|today'?s\s+date)\b""")
            ),
            category = ResponseCategory.TIME_AND_DATE,
            generator = { _, _, _ ->
                val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                val currentDate = dateFormat.format(Date())
                val phrase = "Today is $currentDate."
                PredefinedResponse(textResponse = phrase, category = ResponseCategory.TIME_AND_DATE)
            }
        ),

        // WEATHER COMMANDS
        PredefinedCommandRule(
            id = "weather_query",
            keywords = listOf("weather", "temperature", "forecast", "rain", "outside"),
            regexPatterns = listOf(
                Regex("""(?i)\b(what('s|\s+is)?\s+the\s+weather|how('s|\s+is)\s+the\s+weather|weather\s+forecast|is\s+it\s+raining|temperature\s+outside)\b""")
            ),
            category = ResponseCategory.WEATHER,
            generator = { _, _, params ->
                val telemetry = params["weatherData"] as? String ?: "72°F, Partly Cloudy with mild humidity"
                val response = "Here is the current environmental telemetry: $telemetry."
                PredefinedResponse(textResponse = response, category = ResponseCategory.WEATHER)
            }
        ),

        // WIFI COMMANDS
        PredefinedCommandRule(
            id = "wifi_control",
            keywords = listOf("wifi", "wi-fi", "wlan", "internet"),
            regexPatterns = listOf(
                Regex("""(?i)\b(turn\s+(on|off)|enable|disable|connect|disconnect)\s+(wifi|wi-fi|wlan|internet)\b""")
            ),
            category = ResponseCategory.DEVICE_CONTROL,
            generator = { _, input, _ ->
                val lower = input.lowercase(Locale.ROOT)
                val enable = !lower.contains("off") && !lower.contains("disable") && !lower.contains("disconnect") && !lower.contains("stop")
                val text = if (enable) {
                    "Turning Wi-Fi ON. Connecting to network..."
                } else {
                    "Turning Wi-Fi OFF. Internet connection disabled."
                }
                PredefinedResponse(
                    textResponse = text,
                    commandType = CommandType.TOGGLE_WIFI,
                    parsedCommand = ParsedCommand(
                        type = CommandType.TOGGLE_WIFI,
                        originalInput = input,
                        booleanParam = enable,
                        summary = text
                    ),
                    category = ResponseCategory.DEVICE_CONTROL
                )
            }
        ),

        // BLUETOOTH COMMANDS
        PredefinedCommandRule(
            id = "bluetooth_control",
            keywords = listOf("bluetooth", "bt"),
            regexPatterns = listOf(
                Regex("""(?i)\b(turn\s+(on|off)|enable|disable)\s+bluetooth\b""")
            ),
            category = ResponseCategory.DEVICE_CONTROL,
            generator = { _, input, _ ->
                val lower = input.lowercase(Locale.ROOT)
                val enable = !lower.contains("off") && !lower.contains("disable") && !lower.contains("stop")
                val text = if (enable) "Turning Bluetooth ON." else "Turning Bluetooth OFF."
                PredefinedResponse(
                    textResponse = text,
                    commandType = CommandType.TOGGLE_BLUETOOTH,
                    parsedCommand = ParsedCommand(
                        type = CommandType.TOGGLE_BLUETOOTH,
                        originalInput = input,
                        booleanParam = enable,
                        summary = text
                    ),
                    category = ResponseCategory.DEVICE_CONTROL
                )
            }
        ),

        // FLASHLIGHT COMMANDS
        PredefinedCommandRule(
            id = "flashlight_control",
            keywords = listOf("flashlight", "torch", "light", "lights"),
            regexPatterns = listOf(
                Regex("""(?i)\b(turn\s+(on|off)|toggle|switch)\s+(flashlight|torch|light|lights)\b""")
            ),
            category = ResponseCategory.DEVICE_CONTROL,
            generator = { _, input, _ ->
                val lower = input.lowercase(Locale.ROOT)
                val enable = !lower.contains("off") && !lower.contains("stop") && !lower.contains("disable")
                val text = if (enable) "Flashlight turned ON." else "Flashlight turned OFF."
                PredefinedResponse(
                    textResponse = text,
                    commandType = CommandType.TOGGLE_FLASHLIGHT,
                    parsedCommand = ParsedCommand(
                        type = CommandType.TOGGLE_FLASHLIGHT,
                        originalInput = input,
                        booleanParam = enable,
                        summary = text
                    ),
                    category = ResponseCategory.DEVICE_CONTROL
                )
            }
        ),

        // SILENT / SOUND MODE COMMANDS
        PredefinedCommandRule(
            id = "sound_mode_control",
            keywords = listOf("silent", "mute", "unmute", "vibrate", "ringer", "sound mode"),
            regexPatterns = listOf(
                Regex("""(?i)\b(silent\s+mode|mute|unmute|vibrate\s+mode|normal\s+sound)\b""")
            ),
            category = ResponseCategory.DEVICE_CONTROL,
            generator = { _, input, _ ->
                val lower = input.lowercase(Locale.ROOT)
                val mode = when {
                    lower.contains("silent") || lower.contains("mute") -> AudioManager.RINGER_MODE_SILENT
                    lower.contains("vibrate") -> AudioManager.RINGER_MODE_VIBRATE
                    else -> AudioManager.RINGER_MODE_NORMAL
                }
                val modeName = when (mode) {
                    AudioManager.RINGER_MODE_SILENT -> "Silent Mode"
                    AudioManager.RINGER_MODE_VIBRATE -> "Vibrate Mode"
                    else -> "Normal Sound Mode"
                }
                val text = "Configuring system audio to $modeName."
                PredefinedResponse(
                    textResponse = text,
                    commandType = CommandType.SET_SOUND_MODE,
                    parsedCommand = ParsedCommand(
                        type = CommandType.SET_SOUND_MODE,
                        originalInput = input,
                        intParam = mode,
                        summary = text
                    ),
                    category = ResponseCategory.DEVICE_CONTROL
                )
            }
        ),

        // IDENTITY / NAME COMMANDS
        PredefinedCommandRule(
            id = "identity_query",
            keywords = listOf("who are you", "what is your name", "what's your name", "who made you", "your name"),
            category = ResponseCategory.ASSISTANT_IDENTITY,
            generator = { _, _, _ ->
                PredefinedResponse(
                    textResponse = "I am Aira, your intelligent AI assistant built with local offline neural processing and real-time device automation capabilities.",
                    category = ResponseCategory.ASSISTANT_IDENTITY
                )
            }
        ),

        // CAPABILITIES / HELP COMMANDS
        PredefinedCommandRule(
            id = "capabilities_query",
            keywords = listOf("what can you do", "help", "commands", "features", "capabilities"),
            category = ResponseCategory.ASSISTANT_IDENTITY,
            generator = { _, _, _ ->
                val helpText = """
                    Here is what I can do for you:
                    ⚡ Device Toggles: WiFi, Bluetooth, Flashlight, Brightness
                    ⏰ Utilities: Set Alarms, Timers, Check Time & Date
                    🌤️ Telemetry: Weather Forecasts, System Status Reports
                    🧠 Memory: Save & Recall personal notes and facts
                    💬 AI Brain: Answer questions online or 100% offline via Llama 3.2
                    📱 Navigation: Go Home, Back, Recents, and Screen Screenshots
                """.trimIndent()
                PredefinedResponse(
                    textResponse = helpText,
                    spokenResponse = "I can control device toggles like WiFi and Flashlight, set alarms, check the weather and time, recall personal memories, and answer questions offline or online.",
                    category = ResponseCategory.ASSISTANT_IDENTITY
                )
            }
        ),

        // JOKE COMMANDS
        PredefinedCommandRule(
            id = "joke_command",
            keywords = listOf("joke", "tell me a joke", "make me laugh", "funny"),
            category = ResponseCategory.FUN_AND_CHAT,
            generator = { _, _, _ ->
                PredefinedResponse(
                    textResponse = jokes.random(),
                    category = ResponseCategory.FUN_AND_CHAT
                )
            }
        ),

        // GREETING COMMANDS
        PredefinedCommandRule(
            id = "greeting_command",
            keywords = listOf("hello", "hi", "hey", "good morning", "good afternoon", "good evening", "how are you"),
            category = ResponseCategory.GREETING,
            generator = { _, input, _ ->
                val lower = input.lowercase(Locale.ROOT)
                val response = when {
                    lower.contains("morning") -> morningGreetings.random()
                    lower.contains("afternoon") -> afternoonGreetings.random()
                    lower.contains("evening") -> eveningGreetings.random()
                    lower.contains("how are you") -> "I'm operating at peak efficiency! How are you doing today?"
                    else -> generalGreetings.random()
                }
                PredefinedResponse(textResponse = response, category = ResponseCategory.GREETING)
            }
        ),

        // GRATITUDE COMMANDS
        PredefinedCommandRule(
            id = "gratitude_command",
            keywords = listOf("thank you", "thanks", "appreciate it", "good job"),
            category = ResponseCategory.GRATITUDE,
            generator = { _, _, _ ->
                val phrase = listOf(
                    "You're very welcome! Always here to assist.",
                    "Anytime! Let me know if you need anything else.",
                    "Glad I could help!",
                    "My pleasure! Standing by for your next command."
                ).random()
                PredefinedResponse(textResponse = phrase, category = ResponseCategory.GRATITUDE)
            }
        )
    )

    /**
     * Attempts to match input text against predefined response rules.
     */
    fun findPredefinedResponse(
        context: Context,
        input: String,
        params: Map<String, Any?> = emptyMap()
    ): PredefinedResponse? {
        val cleanInput = input.trim()
        val lowerInput = cleanInput.lowercase(Locale.ROOT)

        if (lowerInput.isEmpty()) return null

        // Search custom rules first
        val allRules = customRules + builtInRules

        for (rule in allRules) {
            // Check regex pattern match
            for (pattern in rule.regexPatterns) {
                if (pattern.containsMatchIn(cleanInput)) {
                    return rule.generator(context, cleanInput, params)
                }
            }

            // Check keywords match
            for (kw in rule.keywords) {
                if (lowerInput.contains(kw.lowercase(Locale.ROOT))) {
                    return rule.generator(context, cleanInput, params)
                }
            }
        }

        return null
    }

    /**
     * Easily register custom response rules at runtime.
     */
    fun registerCustomResponseRule(rule: PredefinedCommandRule) {
        customRules.add(rule)
    }

    /**
     * Get random fun processing message when Aira is thinking.
     */
    fun getRandomProcessingPhrase(): String {
        return processingPhrases.random()
    }

    /**
     * Get fun, helpful fallback response when Aira doesn't understand the input.
     */
    fun getRandomFallbackResponse(userInput: String = ""): String {
        val baseFallback = fallbackResponses.random()
        return if (userInput.isNotBlank()) {
            "$baseFallback (Input: '$userInput')"
        } else {
            baseFallback
        }
    }

    /**
     * Get contextual greeting based on current time of day.
     */
    fun getTimeContextGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> morningGreetings.random()
            in 12..16 -> afternoonGreetings.random()
            in 17..22 -> eveningGreetings.random()
            else -> "Hello! Late night owl shift? I'm here to help."
        }
    }

    /**
     * Get a random joke.
     */
    fun getRandomJoke(): String {
        return jokes.random()
    }
}
