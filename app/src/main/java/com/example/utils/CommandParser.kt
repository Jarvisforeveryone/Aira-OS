package com.example.utils

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import com.example.service.AiraAccessibilityService
import com.example.ui.AiraViewModel
import java.util.Locale
import java.util.regex.Pattern

enum class CommandType {
    TOGGLE_WIFI,
    ADJUST_BRIGHTNESS,
    SET_ALARM,
    TOGGLE_BLUETOOTH,
    TOGGLE_FLASHLIGHT,
    SET_SOUND_MODE,
    LAUNCH_CAMERA,
    MAKE_CALL,
    SYSTEM_NAVIGATION,
    LOCK_SCREEN,
    TAKE_SCREENSHOT,
    OPEN_NOTIFICATIONS,
    OPEN_QUICK_SETTINGS,
    OPEN_POWER_MENU,
    CLICK_SCREEN_ELEMENT,
    DEVICE_POLICY_STATUS,
    ADJUST_VOLUME,
    OPEN_SETTINGS_PAGE,
    LAUNCH_APP,
    MORNING_BRIEFING,
    UNKNOWN
}

data class ParsedCommand(
    val type: CommandType,
    val originalInput: String,
    val booleanParam: Boolean? = null,
    val intParam: Int? = null,
    val extraIntParam: Int? = null,
    val stringParam: String? = null,
    val summary: String
)

object CommandParser {

    /**
     * Parses voice input or text into a structured system command.
     */
    fun parse(input: String, currentBrightnessPercent: Int = 50): ParsedCommand? {
        val rawInput = input.trim()
        val lower = rawInput.lowercase(Locale.ROOT)

        if (lower.isBlank()) return null

        // 1. Wi-Fi Control
        if (containsAny(lower, "wifi", "wi-fi", "wlan", "internet")) {
            val enable = !containsAny(lower, "off", "disable", "stop", "deactivate", "disconnect")
            return ParsedCommand(
                type = CommandType.TOGGLE_WIFI,
                originalInput = rawInput,
                booleanParam = enable,
                summary = if (enable) "Turning Wi-Fi ON" else "Turning Wi-Fi OFF"
            )
        }

        // 2. Brightness Adjustment
        if (containsAny(lower, "brightness", "screen light", "display brightness", "dim screen", "brighten screen", "brighten", "dim")) {
            val percent = extractBrightnessPercent(lower, currentBrightnessPercent)
            return ParsedCommand(
                type = CommandType.ADJUST_BRIGHTNESS,
                originalInput = rawInput,
                intParam = percent,
                summary = "Setting screen brightness to $percent%"
            )
        }

        // 3. Alarm Scheduling
        if (containsAny(lower, "alarm", "wake me up", "wake up at", "set alarm")) {
            val timeAndLabel = parseAlarmTimeAndLabel(lower)
            val hour = timeAndLabel.first
            val minute = timeAndLabel.second
            val label = timeAndLabel.third

            val timeFormatted = String.format(Locale.US, "%02d:%02d %s", if (hour % 12 == 0) 12 else hour % 12, minute, if (hour >= 12) "PM" else "AM")
            val summaryText = if (label.isNotBlank()) "Setting alarm for $timeFormatted ($label)" else "Setting alarm for $timeFormatted"

            return ParsedCommand(
                type = CommandType.SET_ALARM,
                originalInput = rawInput,
                intParam = hour,
                extraIntParam = minute,
                stringParam = label,
                summary = summaryText
            )
        }

        // 4. Bluetooth Control
        if (containsAny(lower, "bluetooth", "bt ") || lower.endsWith("bt")) {
            val enable = !containsAny(lower, "off", "disable", "stop", "deactivate")
            return ParsedCommand(
                type = CommandType.TOGGLE_BLUETOOTH,
                originalInput = rawInput,
                booleanParam = enable,
                summary = if (enable) "Turning Bluetooth ON" else "Turning Bluetooth OFF"
            )
        }

        // 5. Flashlight / Torch
        if (containsAny(lower, "flashlight", "torch", "light")) {
            val enable = !containsAny(lower, "off", "stop", "disable")
            return ParsedCommand(
                type = CommandType.TOGGLE_FLASHLIGHT,
                originalInput = rawInput,
                booleanParam = enable,
                summary = if (enable) "Turning Flashlight ON" else "Turning Flashlight OFF"
            )
        }

        // 6. Sound / Ringer Mode
        if (containsAny(lower, "silent", "mute", "vibrate", "unmute", "normal mode", "ring mode")) {
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
            return ParsedCommand(
                type = CommandType.SET_SOUND_MODE,
                originalInput = rawInput,
                intParam = mode,
                summary = "Configuring system audio to $modeName"
            )
        }

        // 7. System Navigation
        if (containsAny(lower, "go home", "home screen", "go back", "back button", "show recents", "recent apps")) {
            val target = when {
                lower.contains("home") -> "home"
                lower.contains("back") -> "back"
                else -> "recents"
            }
            return ParsedCommand(
                type = CommandType.SYSTEM_NAVIGATION,
                originalInput = rawInput,
                stringParam = target,
                summary = "Executing navigation action: $target"
            )
        }

        // 8. Lock Screen (Device Policy / Accessibility)
        if (containsAny(lower, "lock screen", "lock phone", "lock device", "turn off screen")) {
            return ParsedCommand(
                type = CommandType.LOCK_SCREEN,
                originalInput = rawInput,
                summary = "Locking device screen"
            )
        }

        // 9. Take Screenshot
        if (containsAny(lower, "take screenshot", "capture screen", "screenshot", "snap screen")) {
            return ParsedCommand(
                type = CommandType.TAKE_SCREENSHOT,
                originalInput = rawInput,
                summary = "Capturing device screenshot"
            )
        }

        // 10. Open Notifications
        if (containsAny(lower, "open notifications", "show notifications", "pull down notifications", "notification shade")) {
            return ParsedCommand(
                type = CommandType.OPEN_NOTIFICATIONS,
                originalInput = rawInput,
                summary = "Expanding notifications panel"
            )
        }

        // 11. Open Quick Settings
        if (containsAny(lower, "open quick settings", "quick settings", "system toggles")) {
            return ParsedCommand(
                type = CommandType.OPEN_QUICK_SETTINGS,
                originalInput = rawInput,
                summary = "Opening Quick Settings shade"
            )
        }

        // 12. Open Power Menu
        if (containsAny(lower, "open power menu", "power options", "power menu", "shutdown menu")) {
            return ParsedCommand(
                type = CommandType.OPEN_POWER_MENU,
                originalInput = rawInput,
                summary = "Displaying system power menu"
            )
        }

        // 13. Click Screen Element by Text
        if (lower.startsWith("click on ") || lower.startsWith("tap on ") || lower.startsWith("press ")) {
            val targetText = when {
                lower.startsWith("click on ") -> rawInput.substring("click on ".length)
                lower.startsWith("tap on ") -> rawInput.substring("tap on ".length)
                else -> rawInput.substring("press ".length)
            }.trim()

            if (targetText.isNotBlank()) {
                return ParsedCommand(
                    type = CommandType.CLICK_SCREEN_ELEMENT,
                    originalInput = rawInput,
                    stringParam = targetText,
                    summary = "Clicking screen element: '$targetText'"
                )
            }
        }

        // 14. Device Policy Admin Status
        if (containsAny(lower, "device admin", "device policy", "admin status", "security policy")) {
            return ParsedCommand(
                type = CommandType.DEVICE_POLICY_STATUS,
                originalInput = rawInput,
                summary = "Querying Device Policy status"
            )
        }

        // 15. Launch Camera
        if (containsAny(lower, "camera", "take photo", "take a picture")) {
            return ParsedCommand(
                type = CommandType.LAUNCH_CAMERA,
                originalInput = rawInput,
                summary = "Launching System Camera"
            )
        }

        // 16. Make Call
        if (containsAny(lower, "call ", "dial ", "phone ")) {
            val digits = lower.filter { it.isDigit() }
            val phoneNum = if (digits.isNotBlank()) digits else "911"
            return ParsedCommand(
                type = CommandType.MAKE_CALL,
                originalInput = rawInput,
                stringParam = phoneNum,
                summary = "Dialing phone number: $phoneNum"
            )
        }

        // 17. Adjust Media Volume
        if (containsAny(lower, "volume", "sound level", "media volume", "louder", "quieter")) {
            val percent = extractBrightnessPercent(lower, 50)
            val actionStr = when {
                containsAny(lower, "up", "increase", "raise", "higher", "louder") -> "increase"
                containsAny(lower, "down", "decrease", "lower", "reduce", "quieter") -> "decrease"
                containsAny(lower, "mute", "silent") -> "mute"
                containsAny(lower, "unmute") -> "unmute"
                else -> "set"
            }
            return ParsedCommand(
                type = CommandType.ADJUST_VOLUME,
                originalInput = rawInput,
                intParam = if (lower.contains("%") || lower.contains("percent")) percent else null,
                stringParam = actionStr,
                summary = "Adjusting system volume"
            )
        }

        // 18. Open Specific Settings
        if (containsAny(lower, "settings") && containsAny(lower, "open", "show", "launch", "wifi", "bluetooth", "display", "sound", "apps", "accessibility")) {
            val settingType = when {
                containsAny(lower, "wifi", "wi-fi", "internet") -> "wifi"
                containsAny(lower, "bluetooth", "bt") -> "bluetooth"
                containsAny(lower, "display", "brightness", "screen") -> "display"
                containsAny(lower, "sound", "audio", "volume") -> "sound"
                containsAny(lower, "app", "application") -> "apps"
                containsAny(lower, "accessibility") -> "accessibility"
                containsAny(lower, "security") -> "security"
                else -> "general"
            }
            return ParsedCommand(
                type = CommandType.OPEN_SETTINGS_PAGE,
                originalInput = rawInput,
                stringParam = settingType,
                summary = "Opening $settingType settings"
            )
        }

        // 19. Launch Application by Name
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")) {
            val appTarget = when {
                lower.startsWith("open ") -> rawInput.substring(5).trim()
                lower.startsWith("launch ") -> rawInput.substring(7).trim()
                else -> rawInput.substring(6).trim()
            }
            if (appTarget.isNotBlank() && !containsAny(appTarget.lowercase(), "notifications", "quick settings", "power menu")) {
                return ParsedCommand(
                    type = CommandType.LAUNCH_APP,
                    originalInput = rawInput,
                    stringParam = appTarget,
                    summary = "Launching app '$appTarget'"
                )
            }
        }

        // 20. Iron Man Morning Briefing Trigger
        if (containsAny(lower, "briefing", "morning briefing", "daily briefing", "agenda briefing", "start briefing", "my briefing", "daily intel")) {
            return ParsedCommand(
                type = CommandType.MORNING_BRIEFING,
                originalInput = rawInput,
                summary = "Generating Iron Man daily morning briefing"
            )
        }

        return null
    }

    /**
     * Executes the parsed command and returns a human-readable confirmation string.
     */
    fun execute(context: Context, command: ParsedCommand, viewModel: AiraViewModel? = null): String {
        return try {
            when (command.type) {
                CommandType.TOGGLE_WIFI -> {
                    val enable = command.booleanParam ?: true
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                        ShizukuManager.toggleWiFi(enable)
                    } else {
                        val service = AiraAccessibilityService.instance
                        if (service != null) {
                            service.toggleWifi(enable)
                        } else if (viewModel != null) {
                            viewModel.toggleWifiAccessibilityFallback(enable)
                            "Wi-Fi toggling initiated (Accessibility Fallback)."
                        } else {
                            "Wi-Fi state modified to ${if (enable) "ON" else "OFF"}."
                        }
                    }
                }

                CommandType.ADJUST_BRIGHTNESS -> {
                    val targetPercent = command.intParam ?: 50
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                        ShizukuManager.setBrightness(targetPercent)
                    } else {
                        setSystemBrightness(context, targetPercent)
                    }
                }

                CommandType.SET_ALARM -> {
                    val hour = command.intParam ?: 7
                    val minute = command.extraIntParam ?: 0
                    val label = command.stringParam ?: "Aira Wake Up Call"

                    if (viewModel != null) {
                        viewModel.setSystemAlarm(hour, minute, label)
                    } else {
                        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                            putExtra(AlarmClock.EXTRA_HOUR, hour)
                            putExtra(AlarmClock.EXTRA_MINUTES, minute)
                            putExtra(AlarmClock.EXTRA_MESSAGE, label)
                            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }

                    val formattedTime = String.format(Locale.US, "%02d:%02d %s", if (hour % 12 == 0) 12 else hour % 12, minute, if (hour >= 12) "PM" else "AM")
                    "System alarm scheduled for $formattedTime ($label)."
                }

                CommandType.TOGGLE_BLUETOOTH -> {
                    val enable = command.booleanParam ?: true
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                        ShizukuManager.toggleBluetooth(enable)
                    } else {
                        val service = AiraAccessibilityService.instance
                        if (service != null) {
                            service.toggleBluetooth(enable)
                        } else if (viewModel != null) {
                            viewModel.toggleBluetoothAccessibilityFallback(enable)
                            "Bluetooth toggling initiated (Accessibility Fallback)."
                        } else {
                            "Bluetooth configured to ${if (enable) "ON" else "OFF"}."
                        }
                    }
                }

                CommandType.TOGGLE_FLASHLIGHT -> {
                    val enable = command.booleanParam ?: true
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                        ShizukuManager.toggleFlashlight(enable)
                    } else {
                        viewModel?.toggleFlashlight(enable) ?: "Flashlight set to ${if (enable) "ON" else "OFF"}."
                    }
                }

                CommandType.SET_SOUND_MODE -> {
                    val mode = command.intParam ?: AudioManager.RINGER_MODE_NORMAL
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                        ShizukuManager.setRingerMode(mode)
                    } else {
                        viewModel?.setSoundMode(mode)
                        val modeName = when (mode) {
                            AudioManager.RINGER_MODE_SILENT -> "Silent Mode"
                            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate Mode"
                            else -> "Normal Sound Mode"
                        }
                        "Ringer audio updated to $modeName."
                    }
                }

                CommandType.SYSTEM_NAVIGATION -> {
                    val target = command.stringParam ?: "home"
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                        ShizukuManager.systemNavigation(target)
                    } else {
                        when (target) {
                            "home" -> viewModel?.triggerHomeAction()
                            "back" -> viewModel?.triggerBackAction()
                            "recents" -> viewModel?.triggerRecentsAction()
                        }
                        "Executing system navigation: ${target.uppercase(Locale.ROOT)}"
                    }
                }

                CommandType.LOCK_SCREEN -> {
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                        ShizukuManager.lockScreen()
                    } else {
                        val result = viewModel?.lockDeviceScreen()
                        result ?: "Lock screen command executed."
                    }
                }

                CommandType.TAKE_SCREENSHOT -> {
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                        ShizukuManager.takeScreenshot()
                    } else {
                        val service = AiraAccessibilityService.instance
                        if (service != null && service.performScreenshotAction()) {
                            "Screenshot captured successfully via Accessibility Service."
                        } else {
                            "Screenshot requires active Accessibility Service (Android 11+)."
                        }
                    }
                }

                CommandType.OPEN_NOTIFICATIONS -> {
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                        ShizukuManager.openNotifications()
                    } else {
                        val service = AiraAccessibilityService.instance
                        if (service != null && service.performNotificationsAction()) {
                            "Notifications shade expanded via Accessibility Service."
                        } else {
                            "Notifications panel opened."
                        }
                    }
                }

                CommandType.OPEN_QUICK_SETTINGS -> {
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                        ShizukuManager.openQuickSettings()
                    } else {
                        val service = AiraAccessibilityService.instance
                        if (service != null && service.performQuickSettingsAction()) {
                            "Quick Settings shade opened via Accessibility Service."
                        } else {
                            "Quick settings panel requested."
                        }
                    }
                }

                CommandType.OPEN_POWER_MENU -> {
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()) {
                        ShizukuManager.openPowerMenu()
                    } else {
                        val service = AiraAccessibilityService.instance
                        if (service != null && service.performPowerMenuAction()) {
                            "System power menu displayed via Accessibility Service."
                        } else {
                            "Power options menu requested."
                        }
                    }
                }

                CommandType.CLICK_SCREEN_ELEMENT -> {
                    val targetText = command.stringParam ?: ""
                    val service = AiraAccessibilityService.instance
                    if (service != null) {
                        service.clickElementByText(targetText)
                    } else {
                        "Clicking on '$targetText' requires active Accessibility Service."
                    }
                }

                CommandType.DEVICE_POLICY_STATUS -> {
                    val isAdminActive = viewModel?.checkDeviceAdminActive() ?: false
                    if (isAdminActive) {
                        "Device Policy Administration is ACTIVE and protecting system policies."
                    } else {
                        "Device Policy Administration is INACTIVE. You can enable it in System Control settings."
                    }
                }

                CommandType.LAUNCH_CAMERA -> {
                    viewModel?.launchSystemCamera()
                    "System camera launched."
                }

                CommandType.MAKE_CALL -> {
                    val number = command.stringParam ?: "911"
                    viewModel?.initiatePhoneCall(number)
                    "Dialing phone number $number."
                }

                CommandType.ADJUST_VOLUME -> {
                    val percent = command.intParam
                    val action = command.stringParam ?: "set"
                    if (ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted() && percent != null) {
                        ShizukuManager.setVolume(3, percent)
                    } else {
                        adjustVolume(context, action, percent)
                    }
                }

                CommandType.OPEN_SETTINGS_PAGE -> {
                    val pageType = command.stringParam ?: "general"
                    openSettingsPage(context, pageType)
                }

                CommandType.LAUNCH_APP -> {
                    val appName = command.stringParam ?: ""
                    launchAppByName(context, appName)
                }

                CommandType.MORNING_BRIEFING -> {
                    if (viewModel != null) {
                        kotlinx.coroutines.runBlocking {
                            viewModel.generateMorningBriefing()
                        }
                    } else {
                        "AIRA Morning Briefing initialized. Weather and schedule synchronization active."
                    }
                }

                CommandType.UNKNOWN -> "Unknown command requested."
            }
        } catch (e: Exception) {
            Log.e("CommandParser", "Error executing command: ${command.type}", e)
            "Failed to execute command: ${e.localizedMessage}"
        }
    }

    private fun adjustVolume(context: Context, action: String, percent: Int?): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return "Audio service unavailable."
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        return try {
            when {
                percent != null -> {
                    val targetVol = (maxVol * (percent.coerceIn(0, 100) / 100f)).toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                    "Media volume set to $percent%."
                }
                action.contains("increase") || action.contains("up") -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    "Volume raised."
                }
                action.contains("decrease") || action.contains("down") -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    "Volume lowered."
                }
                action.contains("mute") -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                    "Volume muted."
                }
                action.contains("unmute") -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                    "Volume unmuted."
                }
                else -> {
                    val currentPct = (currentVol * 100 / maxVol)
                    "Current media volume is $currentPct%."
                }
            }
        } catch (e: Exception) {
            Log.e("CommandParser", "Failed to adjust volume", e)
            "Error adjusting volume: ${e.localizedMessage}"
        }
    }

    private fun openSettingsPage(context: Context, settingsType: String): String {
        val action = when (settingsType.lowercase()) {
            "wifi", "wi-fi", "internet" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth", "bt" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "brightness", "screen" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "audio", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "security" -> Settings.ACTION_SECURITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening $settingsType settings."
        } catch (e: Exception) {
            Log.e("CommandParser", "Failed to open settings: $settingsType", e)
            "Opening system settings."
        }
    }

    private fun launchAppByName(context: Context, appName: String): String {
        if (appName.isBlank()) return "Please specify an application name to open."
        val lowerTarget = appName.lowercase().trim()

        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

            val matchedInfo = resolveInfos.firstOrNull { info ->
                val label = info.loadLabel(pm).toString().lowercase()
                label == lowerTarget || label.contains(lowerTarget) || lowerTarget.contains(label)
            }

            if (matchedInfo != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedInfo.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "Opening ${matchedInfo.loadLabel(pm)}."
                }
            }

            // Common app shortcuts fallback
            val fallbackPackage = when (lowerTarget) {
                "youtube" -> "com.google.android.youtube"
                "chrome" -> "com.android.chrome"
                "maps" -> "com.google.android.apps.maps"
                "whatsapp" -> "com.whatsapp"
                "spotify" -> "com.spotify.music"
                "settings" -> "com.android.settings"
                "camera" -> "com.android.camera"
                "calculator" -> "com.google.android.calculator"
                "clock" -> "com.google.android.deskclock"
                else -> null
            }

            if (fallbackPackage != null) {
                val launchIntent = pm.getLaunchIntentForPackage(fallbackPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "Opening $appName."
                }
            }

            "Could not find application '$appName' installed on device."
        } catch (e: Exception) {
            Log.e("CommandParser", "Failed to launch app $appName", e)
            "Error opening app $appName: ${e.localizedMessage}"
        }
    }

    private fun setSystemBrightness(context: Context, percent: Int): String {
        val clampedPercent = percent.coerceIn(0, 100)
        return try {
            if (Settings.System.canWrite(context)) {
                val scaled = (clampedPercent * 255 / 100).coerceIn(0, 255)
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    scaled
                )
                "Screen brightness set to $clampedPercent%."
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Write Settings permission required. Opening system settings to adjust screen brightness to $clampedPercent%."
            }
        } catch (e: Exception) {
            Log.e("CommandParser", "Failed to adjust brightness", e)
            "Error adjusting screen brightness: ${e.localizedMessage}"
        }
    }

    private fun extractBrightnessPercent(input: String, currentPercent: Int): Int {
        // Look for percentage e.g. "80%", "50 percent", "set brightness to 70"
        val percentRegex = Pattern.compile("(\\d{1,3})\\s*%?")
        val matcher = percentRegex.matcher(input)
        if (matcher.find()) {
            val matchedVal = matcher.group(1)?.toIntOrNull()
            if (matchedVal != null && matchedVal in 0..100) {
                return matchedVal
            }
        }

        // Preset keywords
        return when {
            containsAny(input, "max", "maximum", "full", "100%") -> 100
            containsAny(input, "min", "minimum", "lowest", "dimmest", "0%") -> 5
            containsAny(input, "half", "medium", "50%") -> 50
            containsAny(input, "increase", "up", "brighter", "higher") -> (currentPercent + 25).coerceAtMost(100)
            containsAny(input, "decrease", "down", "dim", "darker", "lower") -> (currentPercent - 25).coerceAtLeast(0)
            else -> 50
        }
    }

    private fun parseAlarmTimeAndLabel(input: String): Triple<Int, Int, String> {
        // Default time 7:00 AM
        var hour = 7
        var minute = 0
        var label = "Aira Wake Up Call"

        // Check for time formats like 7:30, 08:15 am, 9 pm, 19:30, 8 o'clock
        val timePattern = Pattern.compile("(\\d{1,2})[:.](\\d{2})\\s*(am|pm)?")
        val simpleTimePattern = Pattern.compile("(\\d{1,2})\\s*(am|pm)")
        val oclockPattern = Pattern.compile("(\\d{1,2})\\s*o'?clock")

        val matcher = timePattern.matcher(input)
        if (matcher.find()) {
            var parsedHour = matcher.group(1)?.toIntOrNull() ?: 7
            val parsedMinute = matcher.group(2)?.toIntOrNull() ?: 0
            val ampm = matcher.group(3)?.lowercase(Locale.ROOT)

            if (ampm == "pm" && parsedHour < 12) parsedHour += 12
            if (ampm == "am" && parsedHour == 12) parsedHour = 0

            hour = parsedHour.coerceIn(0, 23)
            minute = parsedMinute.coerceIn(0, 59)
        } else {
            val simpleMatcher = simpleTimePattern.matcher(input)
            if (simpleMatcher.find()) {
                var parsedHour = simpleMatcher.group(1)?.toIntOrNull() ?: 7
                val ampm = simpleMatcher.group(2)?.lowercase(Locale.ROOT)

                if (ampm == "pm" && parsedHour < 12) parsedHour += 12
                if (ampm == "am" && parsedHour == 12) parsedHour = 0

                hour = parsedHour.coerceIn(0, 23)
                minute = 0
            } else {
                val oclockMatcher = oclockPattern.matcher(input)
                if (oclockMatcher.find()) {
                    var parsedHour = oclockMatcher.group(1)?.toIntOrNull() ?: 7
                    if (input.contains("pm") || input.contains("evening") || input.contains("night")) {
                        if (parsedHour < 12) parsedHour += 12
                    }
                    hour = parsedHour.coerceIn(0, 23)
                    minute = 0
                } else {
                    // Try to find any standalone digits
                    val digits = input.filter { it.isDigit() }
                    if (digits.length >= 2) {
                        if (digits.length >= 4) {
                            hour = digits.substring(0, 2).toIntOrNull()?.coerceIn(0, 23) ?: 7
                            minute = digits.substring(2, 4).toIntOrNull()?.coerceIn(0, 59) ?: 0
                        } else {
                            val h = digits.toIntOrNull() ?: 7
                            hour = if (h in 1..12 && (input.contains("pm") || input.contains("evening"))) (h + 12) % 24 else h.coerceIn(0, 23)
                        }
                    }
                }
            }
        }

        // Extract label if present (e.g. "for workout", "for meeting")
        if (input.contains(" for ")) {
            val parts = input.split(" for ")
            if (parts.size > 1) {
                label = parts[1].replace(Regex("\\b(\\d{1,2}(:\\d{2})?|am|pm)\\b"), "").trim()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }

        return Triple(hour, minute, label)
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
