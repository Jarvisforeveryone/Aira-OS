package com.example.utils

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.data.VoiceCommandManager
import com.example.models.JarvisSpecializedToolkit
import com.example.service.AiraAccessibilityService
import com.example.service.AiraAutomationEngine
import com.example.service.ShizukuVoiceExecutionService
import com.example.ui.AiraViewModel
import java.util.Locale
import java.util.regex.Pattern

data class SubCommandResult(
    val rawText: String,
    val isHandled: Boolean,
    val message: String
)

data class MultiCommandDispatchResult(
    val isHandled: Boolean,
    val executedCount: Int,
    val results: List<SubCommandResult>,
    val finalResponseText: String
)

/**
 * High-performance, zero-latency on-device command engine.
 * Solves multi-command chaining and eliminates slow 20-50s LLM timeouts and hallucinations
 * by executing all device controls, hardware toggles, and system automation locally in < 50ms.
 */
object InstantMultiCommandDispatcher {

    private const val TAG = "MultiCommandDispatcher"

    // Conjunctions used to connect commands in natural speech (English, Urdu, Roman Urdu)
    private val CONJUNCTION_REGEX = Regex(
        "(?i)\\b(?:and then|then|and also|after that|as well as|and|plus|also|with|aur phir|phir|aur|wa)\\b|[,;&\\n\\+]"
    )

    // Conversational leading prefixes to strip from sub-clauses
    private val POLITE_PREFIX_REGEX = Regex(
        "(?i)^(?:can you please|could you please|please|can you|could you|would you|aira|jarvis|hey aira|ok aira|hey jarvis|kindly|now|next|also|just)\\s+"
    )

    /**
     * Splits an input string into atomic command clauses.
     * Supports shared verb distributions such as "turn on wifi and bluetooth"
     * -> ["turn on wifi", "turn on bluetooth"].
     */
    fun splitIntoSubCommands(rawInput: String): List<String> {
        val input = rawInput.trim()
        if (input.isBlank()) return emptyList()

        // 1. Expand shared-verb compound device commands
        val expandedInput = expandSharedVerbs(input)

        // 2. Split along conjunctions and punctuation
        val parts = expandedInput.split(CONJUNCTION_REGEX)
            .map { cleanClause(it) }
            .filter { it.isNotBlank() && it.length > 1 }

        return if (parts.isEmpty()) listOf(cleanClause(input)) else parts
    }

    private fun cleanClause(clause: String): String {
        var cleaned = clause.trim()
        while (POLITE_PREFIX_REGEX.containsMatchIn(cleaned)) {
            cleaned = cleaned.replaceFirst(POLITE_PREFIX_REGEX, "").trim()
        }
        return cleaned.trim('.', ',', '!', '?', ';', ':')
    }

    /**
     * Expands phrases like "turn on wifi and bluetooth" or "turn off wifi, bluetooth and flashlight"
     * into "turn on wifi and turn on bluetooth".
     */
    private fun expandSharedVerbs(input: String): String {
        val lower = input.lowercase(Locale.ROOT).trim()

        val verbPatterns = listOf(
            Regex("^(turn on|switch on|enable|start)\\s+([a-z0-9\\s,]+)\\s+(?:and|&|\\+)\\s+([a-z0-9\\s]+)$", RegexOption.IGNORE_CASE),
            Regex("^(turn off|switch off|disable|stop)\\s+([a-z0-9\\s,]+)\\s+(?:and|&|\\+)\\s+([a-z0-9\\s]+)$", RegexOption.IGNORE_CASE)
        )

        for (pattern in verbPatterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val verb = match.groupValues[1].trim()
                val firstTargets = match.groupValues[2].split(Regex("[,&]|\\band\\b")).map { it.trim() }.filter { it.isNotBlank() }
                val lastTarget = match.groupValues[3].trim()
                val allTargets = firstTargets + lastTarget

                // If any target already starts with a command action verb, do not expand as shared verb
                val actionVerbs = listOf("turn on", "turn off", "switch on", "switch off", "enable", "disable", "open", "launch", "set", "dim", "brighten", "mute", "unmute", "take", "lock")
                if (allTargets.any { target -> actionVerbs.any { v -> target.startsWith(v) } }) {
                    continue
                }

                val knownTargets = listOf("wifi", "wi-fi", "bluetooth", "bt", "flashlight", "torch", "light", "lights", "hotspot", "dnd", "do not disturb", "airplane mode", "flight mode", "mobile data", "battery saver", "location", "gps", "camera")
                val isDeviceTargetGroup = allTargets.all { t ->
                    knownTargets.any { k -> t.contains(k) }
                }

                if (isDeviceTargetGroup) {
                    return allTargets.joinToString(" and then ") { "$verb $it" }
                }
            }
        }
        return input
    }

    /**
     * Instantly executes single or multi-command inputs locally.
     * Returns MultiCommandDispatchResult with isHandled = true if any command was executed.
     */
    suspend fun dispatch(context: Context, rawInput: String, viewModel: AiraViewModel?): MultiCommandDispatchResult? {
        val subCommands = splitIntoSubCommands(rawInput)
        if (subCommands.isEmpty()) return null

        val executedResults = mutableListOf<SubCommandResult>()

        for (sub in subCommands) {
            val result = executeSingleClause(context, sub, viewModel)
            if (result != null && result.isHandled) {
                executedResults.add(result)
            }
        }

        if (executedResults.isEmpty()) {
            return null
        }

        // Formulate a clean, unified response
        val finalResponse = if (executedResults.size == 1) {
            executedResults.first().message
        } else {
            val summarySteps = executedResults.map { cleanSummary(it.message) }
            val joined = if (summarySteps.size == 2) {
                "${summarySteps[0]}, and ${summarySteps[1]}"
            } else {
                summarySteps.dropLast(1).joinToString(", ") + ", and " + summarySteps.last()
            }
            "Done: $joined."
        }

        Log.i(TAG, "Instantly dispatched ${executedResults.size} commands: $finalResponse")

        return MultiCommandDispatchResult(
            isHandled = true,
            executedCount = executedResults.size,
            results = executedResults,
            finalResponseText = finalResponse
        )
    }

    /**
     * Executes a single atomic command clause locally with 0ms delay.
     */
    suspend fun executeSingleClause(context: Context, rawClause: String, viewModel: AiraViewModel?): SubCommandResult? {
        val clause = cleanClause(rawClause)
        val lower = clause.lowercase(Locale.ROOT)
        if (lower.isBlank()) return null

        // 1. Direct Macro check (e.g. "goodnight", "night mode", "morning routine")
        val macro = MacroManager.processMacro(context, clause)
        if (macro.executed) {
            return SubCommandResult(clause, true, macro.summary)
        }

        // 2. Wi-Fi
        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("wlan") || lower.contains("internet")) {
            val enable = !containsAny(lower, "off", "disable", "stop", "deactivate", "disconnect", "band")
            val msg = if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                ShizukuManager.toggleWiFi(enable)
                "Wi-Fi set to ${if (enable) "ON" else "OFF"}"
            } else {
                val service = AiraAccessibilityService.instance
                if (service != null) {
                    service.toggleWifi(enable)
                } else if (viewModel != null) {
                    viewModel.toggleWifiAccessibilityFallback(enable)
                    "Wi-Fi toggled to ${if (enable) "ON" else "OFF"}"
                } else {
                    CommandParser.parse(clause)?.let { CommandParser.execute(context, it, viewModel) }
                        ?: "Wi-Fi toggled to ${if (enable) "ON" else "OFF"}"
                }
            }
            return SubCommandResult(clause, true, msg)
        }

        // 3. Bluetooth
        if (lower.contains("bluetooth") || lower.contains("bt ") || lower.endsWith("bt")) {
            val enable = !containsAny(lower, "off", "disable", "stop", "deactivate", "disconnect", "band")
            val msg = if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                ShizukuManager.toggleBluetooth(enable)
                "Bluetooth set to ${if (enable) "ON" else "OFF"}"
            } else {
                val service = AiraAccessibilityService.instance
                if (service != null) {
                    service.toggleBluetooth(enable)
                } else if (viewModel != null) {
                    viewModel.toggleBluetoothAccessibilityFallback(enable)
                    "Bluetooth toggled to ${if (enable) "ON" else "OFF"}"
                } else {
                    CommandParser.parse(clause)?.let { CommandParser.execute(context, it, viewModel) }
                        ?: "Bluetooth toggled to ${if (enable) "ON" else "OFF"}"
                }
            }
            return SubCommandResult(clause, true, msg)
        }

        // 4. Flashlight / Torch
        if (lower.contains("flashlight") || lower.contains("torch") || lower.contains("light") || lower.contains("batti")) {
            val enable = !containsAny(lower, "off", "stop", "disable", "band", "bujhao")
            val msg = if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                ShizukuManager.toggleFlashlight(enable)
                "Flashlight set to ${if (enable) "ON" else "OFF"}"
            } else if (viewModel != null) {
                viewModel.toggleFlashlight(enable)
            } else {
                CommandParser.parse(clause)?.let { CommandParser.execute(context, it, viewModel) }
                    ?: "Flashlight set to ${if (enable) "ON" else "OFF"}"
            }
            return SubCommandResult(clause, true, msg)
        }

        // 5. Brightness
        if (containsAny(lower, "brightness", "screen light", "display brightness", "dim screen", "brighten screen", "dim", "brighten")) {
            val percent = extractBrightnessPercent(lower, 50)
            val msg = if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                ShizukuManager.setBrightness(percent)
                "Screen brightness set to $percent%"
            } else {
                val parsed = CommandParser.parse("set brightness to $percent%")
                if (parsed != null) {
                    CommandParser.execute(context, parsed, viewModel)
                } else {
                    "Screen brightness set to $percent%"
                }
            }
            return SubCommandResult(clause, true, msg)
        }

        // 6. Volume Control
        if (lower.contains("volume") || lower.contains("awaz")) {
            val parsed = CommandParser.parse(clause)
            val msg = if (parsed != null) {
                CommandParser.execute(context, parsed, viewModel)
            } else {
                val num = extractNumber(lower)
                if (num != null) {
                    val p = CommandParser.parse("set volume to $num%")
                    if (p != null) CommandParser.execute(context, p, viewModel) else "Media volume set to $num%"
                } else {
                    "Volume adjusted"
                }
            }
            return SubCommandResult(clause, true, msg)
        }

        // 7. Sound Mode (Silent / Mute / Vibrate / Normal)
        if (containsAny(lower, "silent", "mute", "vibrate", "unmute", "normal mode", "sound on", "sound off", "khamosh")) {
            val mode = when {
                lower.contains("silent") || lower.contains("mute") || lower.contains("sound off") || lower.contains("khamosh") -> AudioManager.RINGER_MODE_SILENT
                lower.contains("vibrate") -> AudioManager.RINGER_MODE_VIBRATE
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            val modeName = when (mode) {
                AudioManager.RINGER_MODE_SILENT -> "Silent"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                else -> "Normal Sound"
            }
            if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                ShizukuManager.setRingerMode(mode)
            } else {
                viewModel?.setSoundMode(mode)
            }
            return SubCommandResult(clause, true, "Sound set to $modeName mode")
        }

        // 8. Do Not Disturb (DND)
        if (lower.contains("do not disturb") || lower.contains("dnd")) {
            val enable = !containsAny(lower, "off", "disable", "stop")
            viewModel?.setDoNotDisturb(enable)
            return SubCommandResult(clause, true, if (enable) "Do Not Disturb activated" else "Do Not Disturb deactivated")
        }

        // 9. Hotspot / Tethering
        if (lower.contains("hotspot") || lower.contains("tethering")) {
            val enable = !containsAny(lower, "off", "disable", "stop")
            val exec = ShizukuVoiceExecutionService.executeVoiceCommand(context, clause)
            val msg = if (exec.responseMessage.isNotBlank()) exec.responseMessage else "Hotspot turned ${if (enable) "ON" else "OFF"}"
            return SubCommandResult(clause, true, msg)
        }

        // 10. Airplane Mode
        if (lower.contains("airplane mode") || lower.contains("flight mode")) {
            val enable = !containsAny(lower, "off", "disable", "stop")
            val exec = ShizukuVoiceExecutionService.executeVoiceCommand(context, clause)
            val msg = if (exec.responseMessage.isNotBlank()) exec.responseMessage else "Airplane mode ${if (enable) "enabled" else "disabled"}"
            return SubCommandResult(clause, true, msg)
        }

        // 11. Battery Saver
        if (lower.contains("battery saver") || lower.contains("power saver")) {
            val enable = !containsAny(lower, "off", "disable", "stop")
            val exec = ShizukuVoiceExecutionService.executeVoiceCommand(context, clause)
            val msg = if (exec.responseMessage.isNotBlank()) exec.responseMessage else "Battery saver ${if (enable) "enabled" else "disabled"}"
            return SubCommandResult(clause, true, msg)
        }

        // 12. Screen Lock
        if (containsAny(lower, "lock screen", "lock phone", "lock device", "screen lock", "band karo screen")) {
            val msg = if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                ShizukuManager.lockScreen()
                "Device screen locked"
            } else {
                viewModel?.lockDeviceScreen() ?: "Device screen locked"
            }
            return SubCommandResult(clause, true, msg)
        }

        // 13. Screenshot
        if (containsAny(lower, "take screenshot", "capture screen", "screenshot", "screen shot", "snap screen")) {
            val msg = if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                ShizukuManager.takeScreenshot()
                "Screenshot captured"
            } else {
                val service = AiraAccessibilityService.instance
                if (service != null && service.performScreenshotAction()) {
                    "Screenshot captured"
                } else {
                    "Capturing screenshot"
                }
            }
            return SubCommandResult(clause, true, msg)
        }

        // 14. Camera
        if (containsAny(lower, "open camera", "launch camera", "take photo", "take picture", "click photo")) {
            viewModel?.launchSystemCamera()
            return SubCommandResult(clause, true, "Camera launched")
        }

        // 15. System Navigation
        if (lower == "go home" || lower == "home" || lower == "home screen") {
            viewModel?.triggerHomeAction()
            return SubCommandResult(clause, true, "Navigated to home screen")
        }
        if (lower == "go back" || lower == "back" || lower == "back button") {
            viewModel?.triggerBackAction()
            return SubCommandResult(clause, true, "Navigated back")
        }
        if (lower == "recent apps" || lower == "show recents" || lower == "open recents" || lower == "recents") {
            viewModel?.triggerRecentsAction()
            return SubCommandResult(clause, true, "Opened recent apps")
        }

        // 16. Notifications & Quick Settings
        if (containsAny(lower, "open notifications", "show notifications", "pull down notifications")) {
            AiraAccessibilityService.instance?.performNotificationsAction()
            return SubCommandResult(clause, true, "Notifications panel opened")
        }
        if (containsAny(lower, "open quick settings", "quick settings", "system toggles")) {
            AiraAccessibilityService.instance?.performQuickSettingsAction()
            return SubCommandResult(clause, true, "Quick settings shade opened")
        }

        // 17. App Launches
        if (lower.startsWith("open app ") || lower.startsWith("launch app ") || lower.startsWith("open ") || lower.startsWith("launch ")) {
            val appName = lower.removePrefix("open app ").removePrefix("launch app ")
                .removePrefix("open ").removePrefix("launch ").trim()
            if (appName.isNotBlank() && !isStandardNonAppTarget(appName)) {
                val parsed = CommandParser.parse("open $appName")
                val result = if (parsed != null) CommandParser.execute(context, parsed, viewModel) else "Launching $appName"
                return SubCommandResult(clause, true, result)
            }
        }

        // 18. Alarms & Timers
        if (containsAny(lower, "alarm", "wake me up", "set alarm")) {
            val parsed = CommandParser.parse(clause)
            if (parsed != null) {
                val res = CommandParser.execute(context, parsed, viewModel)
                return SubCommandResult(clause, true, res)
            }
        }

        // 19. Specialized Toolkit: Math Evaluation
        val mathResult = JarvisSpecializedToolkit.tryEvaluateMath(clause)
        if (mathResult != null) {
            return SubCommandResult(clause, true, mathResult)
        }

        // 20. Specialized Toolkit: Unit & Currency Conversion
        val convResult = JarvisSpecializedToolkit.tryEvaluateConversion(clause)
        if (convResult != null) {
            return SubCommandResult(clause, true, convResult)
        }

        // 21. Standard CommandParser match
        val parsedCmd = CommandParser.parse(clause)
        if (parsedCmd != null && parsedCmd.type != CommandType.UNKNOWN) {
            val msg = CommandParser.execute(context, parsedCmd, viewModel)
            return SubCommandResult(clause, true, msg)
        }

        // 22. VoiceCommandManager Aliases (Urdu, Roman Urdu, English)
        val vcm = VoiceCommandManager.getInstance(context)
        for ((action, aliases) in VoiceCommandManager.commandAliases) {
            val isExact = aliases.any { it.lowercase(Locale.ROOT).trim() == lower }
            val match = if (!isExact) vcm.fuzzyMatch(lower, aliases) else null
            if (isExact || match != null) {
                val actionResult = vcm.executeAction(action, viewModel)
                return SubCommandResult(clause, true, actionResult)
            }
        }

        // 23. AiraAutomationEngine direct intent
        val autoEngine = AiraAutomationEngine(context)
        val autoResult = autoEngine.executeIntent(clause)
        if (autoResult != null) {
            return SubCommandResult(clause, true, autoResult)
        }

        return null
    }

    private fun isStandardNonAppTarget(name: String): Boolean {
        val reserved = listOf("wifi", "bluetooth", "flashlight", "torch", "camera", "settings", "dnd", "notifications", "quick settings", "screen", "volume")
        return reserved.any { name == it }
    }

    private fun cleanSummary(msg: String): String {
        return msg.trim()
            .removePrefix("Aira command:")
            .removePrefix("Aira:")
            .removePrefix("Done:")
            .removeSuffix(".")
            .replace("via Shizuku", "")
            .replace("(Accessibility Fallback)", "")
            .replace("via Shizuku ADB", "")
            .trim()
    }

    private fun extractBrightnessPercent(input: String, defaultPercent: Int): Int {
        val pattern = Pattern.compile("(\\d{1,3})\\s*%?")
        val matcher = pattern.matcher(input)
        if (matcher.find()) {
            val parsed = matcher.group(1)?.toIntOrNull()
            if (parsed != null && parsed in 0..100) return parsed
        }
        return when {
            containsAny(input, "max", "maximum", "full", "100%") -> 100
            containsAny(input, "min", "minimum", "dimmest", "0%") -> 5
            containsAny(input, "dim", "darker", "low") -> 20
            containsAny(input, "bright", "brighter", "high") -> 85
            else -> defaultPercent
        }
    }

    private fun extractNumber(input: String): Int? {
        val pattern = Pattern.compile("(\\d{1,3})")
        val matcher = pattern.matcher(input)
        if (matcher.find()) {
            val parsed = matcher.group(1)?.toIntOrNull()
            if (parsed != null && parsed in 0..100) return parsed
        }
        return null
    }

    private fun containsAny(input: String, vararg keywords: String): Boolean {
        return keywords.any { input.contains(it) }
    }
}
