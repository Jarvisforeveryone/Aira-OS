package com.example.utils

import java.util.Locale

enum class TaskType {
    CODE,
    CREATIVE,
    COMMAND,
    RESEARCH,
    CHAT
}

object TaskDetector {

    private val codeKeywords = listOf(
        "code", "coding", "program", "programming", "bug", "function", "algorithm",
        "script", "python", "kotlin", "java", "class", "method", "refactor", "compile",
        "api", "json", "sql", "git", "gradle", "syntax", "array", "database", "loop"
    )

    private val creativeKeywords = listOf(
        "write", "story", "poem", "idea", "creative", "essay", "song", "lyrics",
        "scriptwriter", "narrative", "fiction", "plot", "rhyme", "dialogue", "novel"
    )

    private val commandKeywords = listOf(
        "device", "control", "weather", "time", "alarm", "turn on", "turn off",
        "dim", "brighten", "volume", "bluetooth", "wifi", "open", "launch", "macro",
        "goodnight", "morning", "toggle", "set sound"
    )

    private val researchKeywords = listOf(
        "explain", "compare", "deep", "analysis", "research", "summarize", "why",
        "how does", "history of", "overview", "definition", "science", "evaluate"
    )

    fun detectTaskType(input: String): TaskType {
        val lower = input.lowercase(Locale.ROOT).trim()

        if (codeKeywords.any { lower.contains(it) }) {
            return TaskType.CODE
        }
        if (commandKeywords.any { lower.contains(it) }) {
            return TaskType.COMMAND
        }
        if (creativeKeywords.any { lower.contains(it) }) {
            return TaskType.CREATIVE
        }
        if (researchKeywords.any { lower.contains(it) }) {
            return TaskType.RESEARCH
        }

        return TaskType.CHAT
    }
}
