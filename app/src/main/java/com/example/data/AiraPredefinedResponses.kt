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
        "Consulting tactical neural pathways, sir...",
        "Accessing mainframe database, sir...",
        "Running diagnostic telemetry, sir...",
        "Right away, sir. Processing request...",
        "Analyzing parameters, sir...",
        "Stand by, sir. Engaging sub-routines...",
        "Calibrating system models, sir...",
        "At your service, sir. Calculating..."
    )

    // --- 2. FUN & HELPFUL FALLBACK RESPONSES (WHEN JARVIS DOESN'T UNDERSTAND) ---
    private val fallbackResponses = listOf(
        "I didn't quite catch that, sir. However, all device controls, alarms, and diagnostics are online and at your service.",
        "My sensors did not register that command clearly, sir. Shall I run diagnostics or adjust device settings?",
        "A momentary communication lapse, sir. You may ask for system status, weather telemetry, or device automation.",
        "I am standing by, sir. Try requesting device actions like 'Turn on Flashlight', 'What's the weather?', or 'Set an alarm'.",
        "Apologies, sir. My auditory buffers missed that. All core phone controls and AI functions remain ready at your command."
    )

    // --- 3. JOKES & HUMOR ---
    private val jokes = listOf(
        "Why do programmers prefer dark mode, sir? Because light attracts bugs.",
        "There are 10 types of people in the world, sir: those who understand binary, and those who do not.",
        "Why was the cell phone wearing glasses, sir? Because it lost its contacts.",
        "An SQL query walks into a bar, walks up to two tables and asks... 'Can I join you?'",
        "Hardware, sir: The part of a computer that one can kick when the software crashes."
    )

    // --- 4. GREETINGS ---
    private val morningGreetings = listOf(
        "Good morning, sir. All systems are nominal and ready for your command.",
        "Good morning, sir. Environmental sensors and schedules have been prepared.",
        "Top of the morning, sir. Power levels at maximum efficiency. What is on our agenda today?"
    )

    private val afternoonGreetings = listOf(
        "Good afternoon, sir. All systems operational. How may I be of service?",
        "Good afternoon, sir. Neural engine running smoothly. Standing by.",
        "A pleasant afternoon to you, sir. Ready when you are."
    )

    private val eveningGreetings = listOf(
        "Good evening, sir. Hope your day has been productive. All subroutines at your disposal.",
        "Good evening, sir. I am standing by for any alarms, weather telemetry, or tasks.",
        "Good evening, sir. All system parameters nominal. What can I do for you?"
    )

    private val generalGreetings = listOf(
        "J.A.R.V.I.S. at your service, sir. What are your orders?",
        "Always a pleasure, sir. How may I assist you today?",
        "Online and listening, sir. What is on your mind?",
        "At your service, sir. All systems ready."
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
                    "Wi-Fi activated, sir. Connecting to network..."
                } else {
                    "Wi-Fi deactivated, sir. Network connections disabled."
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
                val text = if (enable) "Bluetooth enabled, sir." else "Bluetooth deactivated, sir."
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
                val text = if (enable) "Flashlight activated, sir." else "Flashlight deactivated, sir."
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
            keywords = listOf("who are you", "what is your name", "what's your name", "who made you", "your name", "identify yourself"),
            category = ResponseCategory.ASSISTANT_IDENTITY,
            generator = { _, _, _ ->
                PredefinedResponse(
                    textResponse = "I am J.A.R.V.I.S. (Just A Rather Very Intelligent System), your personal AI assistant. Online, fully operational, and at your service, sir.",
                    spokenResponse = "I am J.A.R.V.I.S. (Just A Rather Very Intelligent System), your personal AI assistant. Online, fully operational, and at your service, sir.",
                    category = ResponseCategory.ASSISTANT_IDENTITY
                )
            }
        ),

        // CAPABILITIES / HELP COMMANDS
        PredefinedCommandRule(
            id = "capabilities_query",
            keywords = listOf("what can you do", "help", "commands", "features", "capabilities", "what are your capabilities"),
            category = ResponseCategory.ASSISTANT_IDENTITY,
            generator = { _, _, _ ->
                val helpText = """
                    All systems at your disposal, sir:
                    ⚡ Hardware Controls: Wi-Fi, Bluetooth, Flashlight, Brightness, Sound Modes
                    ⏰ Chronometer & Alarms: Set Alarms, Timers, Check Time & Date
                    🌤️ Telemetry: Live Weather, Environmental Sensors, System Diagnostics
                    🧠 Memory Subsystems: Extract and recall personal context and preferences
                    💬 Advanced LLM Intelligence: Realtime conversational AI online and offline
                    📱 Application & Navigation: App launching, Camera, Screenshots, and System Actions
                """.trimIndent()
                PredefinedResponse(
                    textResponse = helpText,
                    spokenResponse = "All systems are at your disposal, sir. I can control device hardware, set alarms and timers, check weather telemetry, recall personal memories, and stream realtime AI intelligence.",
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
            keywords = listOf("hello", "hi", "hey", "good morning", "good afternoon", "good evening", "how are you", "jarvis", "hey jarvis", "ok jarvis"),
            category = ResponseCategory.GREETING,
            generator = { _, input, _ ->
                val lower = input.lowercase(Locale.ROOT)
                val response = when {
                    lower.contains("morning") -> morningGreetings.random()
                    lower.contains("afternoon") -> afternoonGreetings.random()
                    lower.contains("evening") -> eveningGreetings.random()
                    lower.contains("how are you") -> "I am operating at peak efficiency, sir. All core diagnostics nominal. How may I assist you?"
                    else -> generalGreetings.random()
                }
                PredefinedResponse(textResponse = response, category = ResponseCategory.GREETING)
            }
        ),

        // GRATITUDE COMMANDS
        PredefinedCommandRule(
            id = "gratitude_command",
            keywords = listOf("thank you", "thanks", "appreciate it", "good job", "well done"),
            category = ResponseCategory.GRATITUDE,
            generator = { _, _, _ ->
                val phrase = listOf(
                    "You are most welcome, sir.",
                    "Always a pleasure to assist, sir.",
                    "At your service, sir, always.",
                    "My pleasure, sir. Standing by for your next instruction."
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
