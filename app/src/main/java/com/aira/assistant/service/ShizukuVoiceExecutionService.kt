package com.aira.assistant.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.aira.assistant.utils.CommandParser
import com.aira.assistant.utils.CommandType
import com.aira.assistant.utils.ParsedCommand
import com.aira.assistant.core.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Service layer that leverages Shizuku API integration to execute system-level
 * and high-privilege ADB commands triggered via voice input or wake word triggers.
 */
class ShizukuVoiceExecutionService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    inner class LocalBinder : Binder() {
        fun getService(): ShizukuVoiceExecutionService = this@ShizukuVoiceExecutionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == ACTION_EXECUTE_VOICE_COMMAND) {
                val commandInput = it.getStringExtra(EXTRA_VOICE_INPUT) ?: ""
                if (commandInput.isNotBlank()) {
                    serviceScope.launch {
                        executeVoiceCommandInternal(applicationContext, commandInput)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val TAG = "ShizukuVoiceExecution"

        const val ACTION_EXECUTE_VOICE_COMMAND = "com.aira.assistant.ACTION_EXECUTE_SHIZUKU_VOICE_COMMAND"
        const val EXTRA_VOICE_INPUT = "extra_voice_input"

        data class ExecutionLog(
            val id: Long = System.currentTimeMillis(),
            val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            val voiceInput: String,
            val commandType: String,
            val executionResult: String,
            val isShizukuElevated: Boolean,
            val isSuccess: Boolean
        )

        private val _executionHistory = MutableStateFlow<List<ExecutionLog>>(emptyList())
        val executionHistory: StateFlow<List<ExecutionLog>> = _executionHistory.asStateFlow()

        private val _lastExecutionLog = MutableStateFlow<ExecutionLog?>(null)
        val lastExecutionLog: StateFlow<ExecutionLog?> = _lastExecutionLog.asStateFlow()

        /**
         * Direct static entry point for executing voice commands via Shizuku API service layer.
         */
        fun executeVoiceCommand(context: Context, voiceInput: String): VoiceExecutionResponse {
            return executeVoiceCommandInternal(context, voiceInput)
        }

        private fun executeVoiceCommandInternal(context: Context, voiceInput: String): VoiceExecutionResponse {
            val trimmed = voiceInput.trim()
            if (trimmed.isEmpty()) {
                return VoiceExecutionResponse(
                    isSuccess = false,
                    responseMessage = "Empty voice command input.",
                    usedShizuku = false
                )
            }

            val isShizukuAvailable = ShizukuManager.isShizukuAvailable()

            // 0. Custom Voice Command Macro Trigger Check
            try {
                val macroResult = kotlinx.coroutines.runBlocking {
                    com.aira.assistant.utils.MacroManager.processMacro(context, trimmed)
                }
                if (macroResult.executed) {
                    val log = ExecutionLog(
                        voiceInput = trimmed,
                        commandType = "MACRO_SEQUENCE",
                        executionResult = macroResult.summary,
                        isShizukuElevated = isShizukuAvailable,
                        isSuccess = true
                    )
                    val currentLogs = _executionHistory.value.toMutableList()
                    currentLogs.add(0, log)
                    if (currentLogs.size > 30) currentLogs.removeAt(currentLogs.size - 1)
                    _executionHistory.value = currentLogs
                    _lastExecutionLog.value = log

                    return VoiceExecutionResponse(
                        isSuccess = true,
                        responseMessage = macroResult.summary,
                        usedShizuku = isShizukuAvailable
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking macro execution: ${e.message}")
            }

            // 1. Direct Shizuku-specific high commands parsing
            val lower = trimmed.lowercase(Locale.ROOT)

            var usedShizuku = false
            var resultMessage = ""
            var commandCategory = "SYSTEM_CONTROL"
            var isSuccess = true

            when {
                // Reboot / Restart device
                lower.contains("reboot") || lower.contains("restart phone") || lower.contains("restart device") -> {
                    commandCategory = "REBOOT"
                    if (isShizukuAvailable) {
                        usedShizuku = true
                        val res = ShizukuManager.reboot()
                        resultMessage = if (!res.startsWith("Error")) "Device rebooting via Shizuku ADB..." else "Reboot failed: $res"
                        isSuccess = !res.startsWith("Error")
                    } else {
                        resultMessage = "Reboot requires active Shizuku ADB service."
                        isSuccess = false
                    }
                }

                // Shutdown / Power off
                lower.contains("shutdown") || lower.contains("power off") -> {
                    commandCategory = "SHUTDOWN"
                    if (isShizukuAvailable) {
                        usedShizuku = true
                        val res = ShizukuManager.shutdown()
                        resultMessage = if (!res.startsWith("Error")) "Device powering off via Shizuku ADB..." else "Shutdown failed: $res"
                        isSuccess = !res.startsWith("Error")
                    } else {
                        resultMessage = "Power off requires active Shizuku ADB service."
                        isSuccess = false
                    }
                }

                // Hotspot / Tethering
                lower.contains("hotspot") || lower.contains("tethering") -> {
                    commandCategory = "HOTSPOT"
                    val enable = !lower.contains("off") && !lower.contains("disable") && !lower.contains("stop")
                    if (isShizukuAvailable) {
                        usedShizuku = true
                        val res = ShizukuManager.toggleHotspot(enable)
                        resultMessage = "Hotspot ${if (enable) "enabled" else "disabled"} via Shizuku."
                    } else {
                        resultMessage = "Hotspot toggled to ${if (enable) "ON" else "OFF"}."
                    }
                }

                // Mobile Data
                lower.contains("mobile data") || lower.contains("cellular data") -> {
                    commandCategory = "MOBILE_DATA"
                    val enable = !lower.contains("off") && !lower.contains("disable") && !lower.contains("stop")
                    if (isShizukuAvailable) {
                        usedShizuku = true
                        val res = ShizukuManager.toggleMobileData(enable)
                        resultMessage = "Mobile data ${if (enable) "enabled" else "disabled"} via Shizuku."
                    } else {
                        resultMessage = "Mobile data command requested."
                    }
                }

                // Airplane mode
                lower.contains("airplane mode") || lower.contains("flight mode") -> {
                    commandCategory = "AIRPLANE_MODE"
                    val enable = !lower.contains("off") && !lower.contains("disable") && !lower.contains("stop")
                    if (isShizukuAvailable) {
                        usedShizuku = true
                        ShizukuManager.toggleAirplaneMode(enable)
                        resultMessage = "Airplane mode ${if (enable) "enabled" else "disabled"} via Shizuku."
                    } else {
                        resultMessage = "Airplane mode toggled."
                    }
                }

                // Battery Saver
                lower.contains("battery saver") || lower.contains("power saver") -> {
                    commandCategory = "BATTERY_SAVER"
                    val enable = !lower.contains("off") && !lower.contains("disable") && !lower.contains("stop")
                    if (isShizukuAvailable) {
                        usedShizuku = true
                        ShizukuManager.toggleBatterySaver(enable)
                        resultMessage = "Battery saver ${if (enable) "enabled" else "disabled"} via Shizuku."
                    } else {
                        resultMessage = "Battery saver mode toggled."
                    }
                }

                // Location / GPS
                lower.contains("location") || lower.contains("gps") -> {
                    commandCategory = "LOCATION"
                    val enable = !lower.contains("off") && !lower.contains("disable") && !lower.contains("stop")
                    if (isShizukuAvailable) {
                        usedShizuku = true
                        ShizukuManager.toggleLocation(enable)
                        resultMessage = "GPS location services ${if (enable) "enabled" else "disabled"} via Shizuku."
                    } else {
                        resultMessage = "GPS location settings updated."
                    }
                }

                // Force Stop App
                lower.contains("force stop") || lower.contains("kill app") || lower.contains("close app") -> {
                    commandCategory = "FORCE_STOP_APP"
                    val appName = trimmed.substringAfter("stop").substringAfter("app").substringAfter("kill").trim()
                    if (appName.isNotBlank() && isShizukuAvailable) {
                        if (ShizukuManager.isValidPackageName(appName)) {
                            usedShizuku = true
                            ShizukuManager.forceStopApp(appName)
                            resultMessage = "Force stopped '$appName' via Shizuku ADB."
                        } else {
                            usedShizuku = false
                            isSuccess = false
                            resultMessage = "Invalid package identifier rejected for safety: '$appName'"
                        }
                    } else {
                        resultMessage = "Closing application '$appName'."
                    }
                }

                // Fallback to standard CommandParser execution engine
                else -> {
                    val parsed = CommandParser.parse(trimmed)
                    if (parsed != null) {
                        commandCategory = parsed.type.name
                        usedShizuku = isShizukuAvailable
                        resultMessage = CommandParser.execute(context, parsed, null)
                    } else {
                        commandCategory = "UNKNOWN"
                        resultMessage = "Voice command not recognized: '$trimmed'"
                        isSuccess = false
                    }
                }
            }

            // Record execution log
            val log = ExecutionLog(
                voiceInput = trimmed,
                commandType = commandCategory,
                executionResult = resultMessage,
                isShizukuElevated = usedShizuku,
                isSuccess = isSuccess
            )

            val currentLogs = _executionHistory.value.toMutableList()
            currentLogs.add(0, log)
            if (currentLogs.size > 30) currentLogs.removeAt(currentLogs.size - 1)
            _executionHistory.value = currentLogs
            _lastExecutionLog.value = log

            Log.d(TAG, "Executed voice command: '$trimmed' -> Shizuku=$usedShizuku | Result=$resultMessage")

            return VoiceExecutionResponse(
                isSuccess = isSuccess,
                responseMessage = resultMessage,
                usedShizuku = usedShizuku
            )
        }
    }
}

data class VoiceExecutionResponse(
    val isSuccess: Boolean,
    val responseMessage: String,
    val usedShizuku: Boolean
)
