package com.example.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VoskLogLevel {
    INFO, WARN, ERROR, INIT_ERROR
}

data class VoskLogEntry(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: VoskLogLevel,
    val tag: String,
    val message: String,
    val throwableMessage: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}

object VoskLogManager {
    private const val MAX_LOGS = 300
    private val _logs = MutableStateFlow<List<VoskLogEntry>>(emptyList())
    val logs: StateFlow<List<VoskLogEntry>> = _logs.asStateFlow()

    private val _initErrorCount = MutableStateFlow(0)
    val initErrorCount: StateFlow<Int> = _initErrorCount.asStateFlow()

    @Synchronized
    fun log(level: VoskLogLevel, tag: String = "VoskEngine", message: String, throwable: Throwable? = null) {
        val entry = VoskLogEntry(
            level = level,
            tag = tag,
            message = message,
            throwableMessage = throwable?.let { "${it.javaClass.simpleName}: ${it.message}" }
        )

        val currentList = _logs.value.toMutableList()
        currentList.add(entry)
        if (currentList.size > MAX_LOGS) {
            currentList.removeAt(0)
        }
        _logs.value = currentList

        if (level == VoskLogLevel.INIT_ERROR) {
            _initErrorCount.value = _initErrorCount.value + 1
        }

        when (level) {
            VoskLogLevel.INFO -> Log.i(tag, message, throwable)
            VoskLogLevel.WARN -> Log.w(tag, message, throwable)
            VoskLogLevel.ERROR, VoskLogLevel.INIT_ERROR -> Log.e(tag, "$message ${throwable?.message ?: ""}", throwable)
        }
    }

    fun logInfo(message: String, tag: String = "VoskEngine") = log(VoskLogLevel.INFO, tag, message)
    fun logWarn(message: String, tag: String = "VoskEngine") = log(VoskLogLevel.WARN, tag, message)
    fun logError(message: String, throwable: Throwable? = null, tag: String = "VoskEngine") = log(VoskLogLevel.ERROR, tag, message, throwable)
    fun logInitError(message: String, throwable: Throwable? = null, tag: String = "VoskInit") = log(VoskLogLevel.INIT_ERROR, tag, message, throwable)

    fun clearLogs() {
        _logs.value = emptyList()
        _initErrorCount.value = 0
    }

    fun getFormattedLogsText(): String {
        return _logs.value.joinToString("\n") { entry ->
            val lvl = when (entry.level) {
                VoskLogLevel.INFO -> "[INFO]"
                VoskLogLevel.WARN -> "[WARN]"
                VoskLogLevel.ERROR -> "[ERROR]"
                VoskLogLevel.INIT_ERROR -> "[INIT_ERR]"
            }
            "${entry.formattedTime} $lvl [${entry.tag}] ${entry.message}" +
                    if (entry.throwableMessage != null) " (${entry.throwableMessage})" else ""
        }
    }
}
