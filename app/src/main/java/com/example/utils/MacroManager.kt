package com.example.utils

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.MacroEntity
import com.example.service.ShizukuVoiceExecutionService
import org.json.JSONArray
import java.util.Locale

data class MacroExecutionResult(
    val executed: Boolean,
    val macroName: String? = null,
    val summary: String = "",
    val actionResults: List<String> = emptyList()
)

object MacroManager {

    private const val TAG = "MacroManager"

    // Default pre-seeded macros in case database has none yet
    private val defaultMacros = mapOf(
        "goodnight" to listOf("dim screen to 10%", "set sound mode to silent", "set alarm for 07:00 AM"),
        "night mode" to listOf("dim screen to 10%", "set sound mode to silent"),
        "morning routine" to listOf("brighten screen to 80%", "set sound mode to normal", "turn wifi on"),
        "focus mode" to listOf("dim screen to 30%", "set sound mode to vibrate", "turn bluetooth off"),
        "leaving home" to listOf("turn wifi off", "set sound mode to normal", "turn bluetooth on")
    )

    /**
     * Checks if user input matches a registered macro trigger.
     * If matched, runs every action in the sequence atomically and returns a combined summary.
     */
    suspend fun processMacro(context: Context, input: String): MacroExecutionResult {
        val cleanInput = input.trim().lowercase(Locale.ROOT)
            .removePrefix("aira").removePrefix(",").trim()
            .removePrefix("run macro").removePrefix("macro").trim()

        if (cleanInput.isBlank()) return MacroExecutionResult(executed = false)

        val db = AppDatabase.getInstance(context)
        val dbMacro = try {
            db.macroDao().getMacroByTrigger(cleanInput)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying macro from DB: ${e.message}")
            null
        }

        val macroTrigger: String
        val actions: List<String>

        if (dbMacro != null) {
            macroTrigger = dbMacro.trigger
            actions = parseActionsJson(dbMacro.actionsJson)
        } else {
            // Check fallback default macros
            val matchedKey = defaultMacros.keys.find { key ->
                cleanInput == key || cleanInput.contains(key)
            }
            if (matchedKey != null) {
                macroTrigger = matchedKey
                actions = defaultMacros[matchedKey] ?: emptyList()
            } else {
                return MacroExecutionResult(executed = false)
            }
        }

        if (actions.isEmpty()) {
            return MacroExecutionResult(
                executed = true,
                macroName = macroTrigger,
                summary = "Macro '$macroTrigger' contains no actions to execute."
            )
        }

        Log.d(TAG, "Executing Macro '$macroTrigger' with ${actions.size} actions sequentially...")

        val actionResults = mutableListOf<String>()
        for (actionCommand in actions) {
            try {
                val res = ShizukuVoiceExecutionService.executeVoiceCommand(context, actionCommand)
                actionResults.add("${actionCommand}: ${res.responseMessage}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed executing sub-command '$actionCommand' in macro: ${e.message}", e)
                actionResults.add("${actionCommand}: Failed (${e.message})")
            }
        }

        val combinedSummary = "Macro '${macroTrigger.replaceFirstChar { it.uppercase() }}' executed (${actionResults.size} actions): " +
                actionResults.joinToString("; ") { it.substringAfter(": ") }

        return MacroExecutionResult(
            executed = true,
            macroName = macroTrigger,
            summary = combinedSummary,
            actionResults = actionResults
        )
    }

    /**
     * Helper to insert or update a custom macro in Room database.
     */
    suspend fun saveCustomMacro(context: Context, trigger: String, actionsList: List<String>, description: String = "") {
        val jsonArray = JSONArray()
        actionsList.forEach { jsonArray.put(it) }
        val entity = MacroEntity(
            trigger = trigger.trim().lowercase(Locale.ROOT),
            actionsJson = jsonArray.toString(),
            description = description
        )
        AppDatabase.getInstance(context).macroDao().insertMacro(entity)
    }

    private fun parseActionsJson(jsonStr: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val action = array.optString(i, "").trim()
                if (action.isNotBlank()) {
                    result.add(action)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing actions JSON: $jsonStr", e)
        }
        return result
    }
}
