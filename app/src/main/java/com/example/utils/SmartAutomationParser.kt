package com.example.utils

import android.content.Context
import com.example.data.MacroEntity
import org.json.JSONArray
import java.util.Locale

enum class TriggerType {
    VOICE_PHRASE,
    BATTERY_LEVEL,
    SCHEDULE_TIME,
    APP_OPENED,
    WIFI_STATE,
    LOCATION_EVENT
}

data class ParsedAutomation(
    val title: String,
    val triggerType: TriggerType,
    val triggerPhrase: String,
    val triggerDisplay: String,
    val actions: List<String>,
    val actionFriendlyDescriptions: List<String>,
    val summary: String
)

data class PredefinedAutomation(
    val id: String,
    val title: String,
    val iconName: String,
    val triggerPhrase: String,
    val triggerDisplay: String,
    val triggerType: TriggerType,
    val actions: List<String>,
    val actionFriendlyDescriptions: List<String>,
    val category: String,
    val description: String
)

object SmartAutomationParser {

    /**
     * Curated, zero-jargon predefined automations that users can install with a single tap.
     */
    val PREDEFINED_TEMPLATES = listOf(
        PredefinedAutomation(
            id = "template_bedtime",
            title = "Bedtime Protocol",
            iconName = "bedtime",
            triggerPhrase = "good night",
            triggerDisplay = "When I say \"Good Night\"",
            triggerType = TriggerType.VOICE_PHRASE,
            actions = listOf(
                "dim screen to 10%",
                "set sound mode to silent",
                "turn wifi off",
                "turn bluetooth off",
                "say Good night, sleep well."
            ),
            actionFriendlyDescriptions = listOf(
                "Dim screen to 10%",
                "Mute ringers & notifications",
                "Turn off Wi-Fi",
                "Turn off Bluetooth",
                "Voice: \"Good night, sleep well.\""
            ),
            category = "Sleep & Night",
            description = "Mutes everything, dims the display, cuts radios for undisturbed rest."
        ),
        PredefinedAutomation(
            id = "template_morning",
            title = "Morning Wakeup",
            iconName = "wb_sunny",
            triggerPhrase = "good morning",
            triggerDisplay = "When I say \"Good Morning\"",
            triggerType = TriggerType.VOICE_PHRASE,
            actions = listOf(
                "brighten screen to 80%",
                "set sound mode to normal",
                "turn wifi on",
                "say Good morning! Ready for today."
            ),
            actionFriendlyDescriptions = listOf(
                "Brighten display to 80%",
                "Unmute sound and ringtones",
                "Turn on Wi-Fi connection",
                "Voice: \"Good morning! Ready for today.\""
            ),
            category = "Daily Routines",
            description = "Turns on Wi-Fi, restores sound, and boosts screen brightness for the day."
        ),
        PredefinedAutomation(
            id = "template_cinema",
            title = "Cinema & Movie Mode",
            iconName = "movie",
            triggerPhrase = "movie time",
            triggerDisplay = "When I say \"Movie Time\"",
            triggerType = TriggerType.VOICE_PHRASE,
            actions = listOf(
                "dim screen to 10%",
                "set sound mode to vibrate",
                "turn bluetooth off"
            ),
            actionFriendlyDescriptions = listOf(
                "Dim screen to minimum",
                "Set sound profile to Vibrate",
                "Turn off Bluetooth"
            ),
            category = "Entertainment",
            description = "Prepares phone for theater or dark movie viewing."
        ),
        PredefinedAutomation(
            id = "template_battery_saver",
            title = "Battery Guardian",
            iconName = "battery_saver",
            triggerPhrase = "battery low",
            triggerDisplay = "When battery drops below 20%",
            triggerType = TriggerType.BATTERY_LEVEL,
            actions = listOf(
                "dim screen to 15%",
                "turn bluetooth off",
                "set sound mode to vibrate",
                "turn wifi off"
            ),
            actionFriendlyDescriptions = listOf(
                "Dim brightness to 15%",
                "Turn off Bluetooth",
                "Switch sound to Vibrate",
                "Turn off Wi-Fi"
            ),
            category = "Power & Battery",
            description = "Automatically conserves battery when power is running low."
        ),
        PredefinedAutomation(
            id = "template_drive",
            title = "Car & Drive Mode",
            iconName = "directions_car",
            triggerPhrase = "drive mode",
            triggerDisplay = "When I say \"Drive Mode\"",
            triggerType = TriggerType.VOICE_PHRASE,
            actions = listOf(
                "turn bluetooth on",
                "set sound mode to normal",
                "open app Google Maps",
                "say Drive mode active. Safe travels."
            ),
            actionFriendlyDescriptions = listOf(
                "Turn on Bluetooth for car audio",
                "Set volume to normal",
                "Open Google Maps",
                "Voice: \"Drive mode active. Safe travels.\""
            ),
            category = "Travel & Auto",
            description = "Connects car Bluetooth, launches navigation, and sets normal volume."
        ),
        PredefinedAutomation(
            id = "template_focus",
            title = "Deep Focus & Study",
            iconName = "psychology",
            triggerPhrase = "focus mode",
            triggerDisplay = "When I say \"Focus Mode\"",
            triggerType = TriggerType.VOICE_PHRASE,
            actions = listOf(
                "set sound mode to silent",
                "dim screen to 30%",
                "turn bluetooth off",
                "say Focus mode active. Zero distractions."
            ),
            actionFriendlyDescriptions = listOf(
                "Silence all notifications",
                "Dim brightness to 30%",
                "Turn off Bluetooth",
                "Voice: \"Focus mode active. Zero distractions.\""
            ),
            category = "Productivity",
            description = "Silences notifications and minimizes distractions for focused work."
        ),
        PredefinedAutomation(
            id = "template_leaving_home",
            title = "Leaving Home",
            iconName = "logout",
            triggerPhrase = "leaving home",
            triggerDisplay = "When I say \"Leaving Home\"",
            triggerType = TriggerType.VOICE_PHRASE,
            actions = listOf(
                "turn wifi off",
                "turn bluetooth on",
                "set sound mode to normal"
            ),
            actionFriendlyDescriptions = listOf(
                "Turn off Wi-Fi",
                "Turn on Bluetooth",
                "Set sound to normal"
            ),
            category = "Daily Routines",
            description = "Turns off home Wi-Fi and connects mobile Bluetooth."
        ),
        PredefinedAutomation(
            id = "template_gaming",
            title = "Gaming Blast",
            iconName = "sports_esports",
            triggerPhrase = "game on",
            triggerDisplay = "When I say \"Game On\"",
            triggerType = TriggerType.VOICE_PHRASE,
            actions = listOf(
                "brighten screen to 90%",
                "set sound mode to normal",
                "turn wifi on"
            ),
            actionFriendlyDescriptions = listOf(
                "Max brightness to 90%",
                "Turn sound to Normal",
                "Turn on Wi-Fi connection"
            ),
            category = "Entertainment",
            description = "Optimizes brightness and network for mobile gaming."
        )
    )

    /**
     * Converts a user natural language statement (spoken or typed) into a fully executable Automation.
     * Examples:
     * - "When I say goodnight, dim screen to 10%, turn off wifi and mute sound"
     * - "When battery is low, turn off bluetooth and dim screen"
     * - "When I open YouTube, unmute sound and set brightness to 80%"
     * - "Every night at 11 PM, set sound to silent and turn off flashlight"
     */
    fun parseNaturalLanguage(rawInput: String): ParsedAutomation {
        val input = rawInput.trim()
        val lower = input.lowercase(Locale.ROOT)

        // 1. Detect Trigger & Actions Clauses
        var triggerPart = ""
        var actionsPart = ""

        val splitKeywords = listOf("then", "do the following", "do this", "and then", ",", ":", "->")
        val whenPrefixes = listOf("when i say", "if i say", "when saying", "on saying", "when battery", "if battery", "when i open", "when opening", "every day at", "every night at", "at ")

        var foundSplit = false
        for (prefix in whenPrefixes) {
            if (lower.startsWith(prefix)) {
                // Find where the trigger ends and actions begin
                val afterPrefix = input.substring(prefix.length).trim()
                val commaIndex = afterPrefix.indexOf(',')
                val thenIndex = afterPrefix.lowercase(Locale.ROOT).indexOf(" then ")
                val doIndex = afterPrefix.lowercase(Locale.ROOT).indexOf(" do ")

                val splitIdx = listOf(commaIndex, thenIndex, doIndex)
                    .filter { it > 0 }
                    .minOrNull()

                if (splitIdx != null && splitIdx > 0) {
                    triggerPart = prefix + " " + afterPrefix.substring(0, splitIdx).trim()
                    actionsPart = afterPrefix.substring(splitIdx).removePrefix(",").removePrefix(" then").removePrefix(" do").trim()
                    foundSplit = true
                    break
                }
            }
        }

        if (!foundSplit) {
            if (lower.contains(":")) {
                triggerPart = input.substringBefore(":").trim()
                actionsPart = input.substringAfter(":").trim()
            } else if (lower.contains(" then ")) {
                triggerPart = input.substringBefore(" then ").trim()
                actionsPart = input.substringAfter(" then ").trim()
            } else if (lower.startsWith("when ") || lower.startsWith("if ") || lower.startsWith("every ")) {
                val parts = input.split(Regex("[,;]"), limit = 2)
                if (parts.size >= 2) {
                    triggerPart = parts[0].trim()
                    actionsPart = parts[1].trim()
                } else {
                    triggerPart = input
                    actionsPart = input
                }
            } else {
                // User just gave a list of actions or a simple command
                triggerPart = "voice command"
                actionsPart = input
            }
        }

        // 2. Identify Trigger Type and Phrase
        val triggerLower = triggerPart.lowercase(Locale.ROOT)
        val triggerType: TriggerType
        val triggerPhrase: String
        val triggerDisplay: String

        when {
            triggerLower.contains("battery") -> {
                triggerType = TriggerType.BATTERY_LEVEL
                triggerPhrase = "battery low"
                triggerDisplay = "When battery drops below 20%"
            }
            triggerLower.contains("open") || triggerLower.contains("launch") -> {
                triggerType = TriggerType.APP_OPENED
                val app = triggerPart.replace(Regex("(?i)when (i )?open|launch|opening"), "").trim()
                triggerPhrase = if (app.isNotBlank()) "open $app" else "app opened"
                triggerDisplay = "When opening ${app.ifBlank { "an app" }}"
            }
            triggerLower.contains("at ") || triggerLower.contains("o'clock") || triggerLower.contains("am") || triggerLower.contains("pm") || triggerLower.contains("every ") -> {
                triggerType = TriggerType.SCHEDULE_TIME
                val timeExtract = triggerPart.replace(Regex("(?i)every day at|every night at|every morning at|at|when it is"), "").trim()
                triggerPhrase = if (timeExtract.isNotBlank()) timeExtract else "11:00 PM"
                triggerDisplay = "Every day at ${triggerPhrase.uppercase(Locale.ROOT)}"
            }
            triggerLower.contains("wifi") || triggerLower.contains("wi-fi") -> {
                triggerType = TriggerType.WIFI_STATE
                triggerPhrase = "wifi state"
                triggerDisplay = if (triggerLower.contains("disconnect") || triggerLower.contains("off")) "When Wi-Fi disconnects" else "When Wi-Fi connects"
            }
            triggerLower.contains("leave") || triggerLower.contains("leaving") || triggerLower.contains("arrive") -> {
                triggerType = TriggerType.LOCATION_EVENT
                val isLeave = triggerLower.contains("leave") || triggerLower.contains("leaving")
                triggerPhrase = if (isLeave) "leaving home" else "arriving home"
                triggerDisplay = if (isLeave) "When leaving home" else "When arriving home"
            }
            else -> {
                triggerType = TriggerType.VOICE_PHRASE
                var cleanPhrase = triggerPart
                    .replace(Regex("(?i)^(when (i )?say|if (i )?say|on saying|voice command|voice|trigger|run|when|if)\\s*"), "")
                    .replace(Regex("[\"']"), "")
                    .trim()
                if (cleanPhrase.isBlank()) cleanPhrase = "my routine"
                triggerPhrase = cleanPhrase.lowercase(Locale.ROOT)
                triggerDisplay = "When I say \"${cleanPhrase.replaceFirstChar { it.uppercase() }}\""
            }
        }

        // 3. Extract Actions
        val rawActionSegments = actionsPart.split(Regex("(?i)\\band\\b|[,;\\+]")).map { it.trim() }.filter { it.isNotBlank() }
        val actions = mutableListOf<String>()
        val descriptions = mutableListOf<String>()

        for (segment in rawActionSegments) {
            val segLower = segment.lowercase(Locale.ROOT)
            when {
                // Brightness
                segLower.contains("dim") || (segLower.contains("brightness") && (segLower.contains("low") || segLower.contains("down") || segLower.contains("10") || segLower.contains("15") || segLower.contains("20") || segLower.contains("30"))) -> {
                    val percent = extractNumber(segLower) ?: 10
                    actions.add("dim screen to $percent%")
                    descriptions.add("Dim screen to $percent%")
                }
                segLower.contains("bright") || segLower.contains("max brightness") || segLower.contains("boost") || (segLower.contains("brightness") && (segLower.contains("high") || segLower.contains("up") || segLower.contains("80") || segLower.contains("90") || segLower.contains("100"))) -> {
                    val percent = extractNumber(segLower) ?: 80
                    actions.add("brighten screen to $percent%")
                    descriptions.add("Brighten screen to $percent%")
                }
                segLower.contains("brightness") -> {
                    val percent = extractNumber(segLower) ?: 50
                    actions.add("brighten screen to $percent%")
                    descriptions.add("Set brightness to $percent%")
                }

                // Sound & Volume
                segLower.contains("vibrate") -> {
                    actions.add("set sound mode to vibrate")
                    descriptions.add("Switch sound to Vibrate mode")
                }
                segLower.contains("unmute") || segLower.contains("normal sound") || segLower.contains("sound on") || segLower.contains("ringer on") -> {
                    actions.add("set sound mode to normal")
                    descriptions.add("Unmute sound & ringtones")
                }
                segLower.contains("silent") || (segLower.contains("mute") && !segLower.contains("unmute")) || segLower.contains("silence") -> {
                    actions.add("set sound mode to silent")
                    descriptions.add("Mute all ringers & notifications")
                }
                segLower.contains("volume") -> {
                    val vol = extractNumber(segLower) ?: 80
                    actions.add("set volume to $vol%")
                    descriptions.add("Set media volume to $vol%")
                }

                // Wi-Fi
                segLower.contains("wifi") || segLower.contains("wi-fi") -> {
                    if (segLower.contains("off") || segLower.contains("disable") || segLower.contains("turn off") || segLower.contains("disconnect")) {
                        actions.add("turn wifi off")
                        descriptions.add("Turn off Wi-Fi")
                    } else {
                        actions.add("turn wifi on")
                        descriptions.add("Turn on Wi-Fi")
                    }
                }

                // Bluetooth
                segLower.contains("bluetooth") || segLower.contains("bt") -> {
                    if (segLower.contains("off") || segLower.contains("disable") || segLower.contains("turn off") || segLower.contains("disconnect")) {
                        actions.add("turn bluetooth off")
                        descriptions.add("Turn off Bluetooth")
                    } else {
                        actions.add("turn bluetooth on")
                        descriptions.add("Turn on Bluetooth")
                    }
                }

                // Flashlight / Torch
                segLower.contains("flashlight") || segLower.contains("torch") -> {
                    if (segLower.contains("off") || segLower.contains("disable") || segLower.contains("turn off")) {
                        actions.add("turn flashlight off")
                        descriptions.add("Turn off Flashlight")
                    } else {
                        actions.add("turn flashlight on")
                        descriptions.add("Turn on Flashlight")
                    }
                }

                // Do Not Disturb
                segLower.contains("dnd") || segLower.contains("do not disturb") -> {
                    if (segLower.contains("off") || segLower.contains("disable")) {
                        actions.add("turn off do not disturb")
                        descriptions.add("Turn off Do Not Disturb")
                    } else {
                        actions.add("turn on do not disturb")
                        descriptions.add("Enable Do Not Disturb")
                    }
                }

                // Battery Saver
                segLower.contains("battery saver") || segLower.contains("power save") || segLower.contains("power saving") -> {
                    actions.add("turn on battery saver")
                    descriptions.add("Enable Battery Saver")
                }

                // Say / Speak message
                segLower.startsWith("say ") || segLower.startsWith("speak ") || segLower.startsWith("announce ") -> {
                    val msg = segment.replace(Regex("(?i)^(say|speak|announce)\\s*"), "").trim()
                    actions.add("say $msg")
                    descriptions.add("Voice: \"$msg\"")
                }

                // App Launch
                segLower.contains("open") || segLower.contains("launch") -> {
                    val app = segment.replace(Regex("(?i)^(open app|open|launch app|launch)\\s*"), "").trim()
                    if (app.isNotBlank()) {
                        actions.add("open app $app")
                        descriptions.add("Open $app")
                    }
                }

                // Alarm / Timer
                segLower.contains("alarm") -> {
                    val time = segment.replace(Regex("(?i).*(alarm for|alarm at|set alarm)\\s*"), "").trim()
                    val alarmVal = if (time.isNotBlank()) time else "07:00 AM"
                    actions.add("set alarm for $alarmVal")
                    descriptions.add("Set alarm for $alarmVal")
                }

                // Fallback direct execution string
                else -> {
                    actions.add(segment)
                    descriptions.add(segment.replaceFirstChar { it.uppercase() })
                }
            }
        }

        // If no specific action recognized, fallback to sensible defaults based on trigger
        if (actions.isEmpty()) {
            if (triggerLower.contains("night") || triggerLower.contains("sleep") || triggerLower.contains("bed")) {
                actions.addAll(listOf("dim screen to 10%", "set sound mode to silent", "turn wifi off"))
                descriptions.addAll(listOf("Dim screen to 10%", "Mute ringers & notifications", "Turn off Wi-Fi"))
            } else if (triggerLower.contains("morning") || triggerLower.contains("wake")) {
                actions.addAll(listOf("brighten screen to 80%", "set sound mode to normal", "turn wifi on"))
                descriptions.addAll(listOf("Brighten display to 80%", "Unmute sound and ringtones", "Turn on Wi-Fi connection"))
            } else {
                actions.add("say $input executed.")
                descriptions.add("Voice confirmation")
            }
        }

        // Generate friendly Title
        val title = when {
            triggerPhrase.contains("night") || triggerPhrase.contains("sleep") || triggerPhrase.contains("bed") -> "Bedtime Routine"
            triggerPhrase.contains("morning") || triggerPhrase.contains("wake") -> "Morning Rise"
            triggerPhrase.contains("movie") || triggerPhrase.contains("cinema") -> "Movie & Cinema Mode"
            triggerPhrase.contains("battery") -> "Battery Saver"
            triggerPhrase.contains("drive") || triggerPhrase.contains("car") -> "Drive & Travel Mode"
            triggerPhrase.contains("focus") || triggerPhrase.contains("study") || triggerPhrase.contains("work") -> "Deep Focus Protocol"
            triggerPhrase.contains("game") || triggerPhrase.contains("gaming") -> "Game Mode"
            triggerPhrase.contains("leave") || triggerPhrase.contains("leaving") -> "Leaving Home"
            triggerPhrase.contains("arrive") || triggerPhrase.contains("home") -> "Arriving Home"
            else -> "${triggerPhrase.replaceFirstChar { it.uppercase() }} Automation"
        }

        val summary = "${descriptions.size} steps: " + descriptions.joinToString(", ")

        return ParsedAutomation(
            title = title,
            triggerType = triggerType,
            triggerPhrase = triggerPhrase,
            triggerDisplay = triggerDisplay,
            actions = actions,
            actionFriendlyDescriptions = descriptions,
            summary = summary
        )
    }

    private fun extractNumber(text: String): Int? {
        val match = Regex("(\\d+)").find(text)
        return match?.value?.toIntOrNull()
    }

    /**
     * Converts a list of actions to a JSON string for MacroEntity storage.
     */
    fun actionsToJson(actions: List<String>): String {
        val array = JSONArray()
        actions.forEach { array.put(it) }
        return array.toString()
    }

    /**
     * Converts a stored JSON string back to list of actions.
     */
    fun parseStoredActions(json: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val act = array.optString(i, "")
                if (act.isNotBlank()) result.add(act)
            }
        } catch (e: Exception) {
            // If it's a plain string, return single element
            if (json.isNotBlank()) result.add(json)
        }
        return result
    }

    /**
     * Translates raw action commands into human-friendly bullet labels with icons.
     */
    fun getFriendlyActionLabel(actionCommand: String): Pair<String, String> {
        val lower = actionCommand.lowercase(Locale.ROOT)
        return when {
            lower.contains("dim screen") || lower.contains("brightness") && (lower.contains("10") || lower.contains("15") || lower.contains("20") || lower.contains("30")) ->
                "🔆" to "Dim screen brightness"
            lower.contains("brighten screen") || lower.contains("brightness") ->
                "🔆" to "Set brightness up"
            lower.contains("silent") || lower.contains("mute") ->
                "🔇" to "Mute all ringers & notifications"
            lower.contains("vibrate") ->
                "📳" to "Switch to Vibrate mode"
            lower.contains("normal") || lower.contains("unmute") ->
                "🔊" to "Restore normal ringers & sound"
            lower.contains("volume") ->
                "🔊" to actionCommand.replaceFirstChar { it.uppercase() }
            lower.contains("wifi off") || lower.contains("turn wifi off") ->
                "📶" to "Turn off Wi-Fi connection"
            lower.contains("wifi on") || lower.contains("turn wifi on") ->
                "📶" to "Turn on Wi-Fi connection"
            lower.contains("bluetooth off") || lower.contains("turn bluetooth off") ->
                "ᛒ" to "Turn off Bluetooth"
            lower.contains("bluetooth on") || lower.contains("turn bluetooth on") ->
                "ᛒ" to "Turn on Bluetooth"
            lower.contains("flashlight off") || lower.contains("turn flashlight off") ->
                "🔦" to "Turn off Flashlight"
            lower.contains("flashlight on") || lower.contains("turn flashlight on") ->
                "🔦" to "Turn on Flashlight"
            lower.contains("battery saver") ->
                "🔋" to "Enable Battery Saver"
            lower.contains("open app") || lower.contains("open") ->
                "📱" to actionCommand.replaceFirstChar { it.uppercase() }
            lower.startsWith("say ") || lower.startsWith("speak ") ->
                "🗣️" to "Voice announcement: \"${actionCommand.substringAfter(" ").trim()}\""
            lower.contains("alarm") ->
                "⏰" to actionCommand.replaceFirstChar { it.uppercase() }
            lower.contains("dnd") || lower.contains("do not disturb") ->
                "⛔" to "Do Not Disturb active"
            else ->
                "⚡" to actionCommand.replaceFirstChar { it.uppercase() }
        }
    }
}
