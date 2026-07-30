package com.example.utils

import java.util.Locale

/**
 * AIRA EMOTION & TONE DETECTION ENGINE
 * Detects user emotional state from text and dynamically selects optimal AI tone & model temperature.
 */
enum class UserEmotion {
    HAPPY,
    SAD,
    ANGRY,
    NEUTRAL
}

data class EmotionToneResult(
    val emotion: UserEmotion,
    val toneInstruction: String,
    val recommendedTemperature: Double
)

object EmotionDetector {

    private val happyKeywords = setOf(
        "happy", "excited", "awesome", "great", "wonderful", "love", "yay", "haha", "lol",
        "amazing", "glad", "delighted", "fantastic", "cool", "super", "brilliant", "enjoy",
        "cheerful", "thrilled", "blessed", "good news", "celebrate", "perfect"
    )

    private val sadKeywords = setOf(
        "sad", "unhappy", "depressed", "sorry", "cry", "crying", "lonely", "upset", "miss",
        "grief", "heartbroken", "bad day", "miserable", "disappointed", "hurt", "gloomy",
        "down", "hopeless", "tragedy", "painful", "feeling blue"
    )

    private val angryKeywords = setOf(
        "angry", "annoyed", "mad", "frustrated", "hate", "furious", "stuck", "terrible",
        "horrible", "stop it", "stupid", "idiot", "useless", "irritated", "outraged",
        "worst", "disgusting", "nonsense", "shut up", "fail", "broken"
    )

    /**
     * Detects user emotional intent and returns tone instructions and suggested temperature.
     */
    fun detectEmotion(input: String): EmotionToneResult {
        val lower = input.lowercase(Locale.ROOT)

        var happyScore = 0
        var sadScore = 0
        var angryScore = 0

        val words = lower.split(Regex("""\W+"""))
        for (word in words) {
            if (happyKeywords.contains(word)) happyScore++
            if (sadKeywords.contains(word)) sadScore++
            if (angryKeywords.contains(word)) angryScore++
        }

        val emotion = when {
            angryScore > 0 && angryScore >= happyScore && angryScore >= sadScore -> UserEmotion.ANGRY
            sadScore > 0 && sadScore >= happyScore -> UserEmotion.SAD
            happyScore > 0 -> UserEmotion.HAPPY
            else -> UserEmotion.NEUTRAL
        }

        val toneInstruction = when (emotion) {
            UserEmotion.HAPPY -> "USER TONE DETECTED: Happy/Enthusiastic. Respond with high energy, warmth, and cheerful enthusiasm."
            UserEmotion.SAD -> "USER TONE DETECTED: Sad/Gloomy. Respond with deep empathy, comforting warmth, and patient support."
            UserEmotion.ANGRY -> "USER TONE DETECTED: Frustrated/Angry. Respond with calm composure, soothing reassurance, and direct efficiency."
            UserEmotion.NEUTRAL -> "USER TONE DETECTED: Neutral. Respond in a refined, succinct, executive assistant tone."
        }

        val temp = getTemperatureForQuery(input)

        return EmotionToneResult(
            emotion = emotion,
            toneInstruction = toneInstruction,
            recommendedTemperature = temp
        )
    }

    /**
     * Calculates model temperature based on query category:
     * - Facts / Commands: Low temperature (0.3) for high precision
     * - Creative chat / Jokes / Stories: High temperature (0.8) for creativity
     * - Default: Medium temperature (0.6)
     */
    fun getTemperatureForQuery(input: String): Double {
        val lower = input.lowercase(Locale.ROOT)

        // Low temperature (0.3) for precise commands, math, device controls, facts, status
        val isFactOrCommand = lower.contains("time") || lower.contains("date") ||
                lower.contains("wifi") || lower.contains("bluetooth") || lower.contains("flashlight") ||
                lower.contains("alarm") || lower.contains("timer") || lower.contains("calculate") ||
                lower.contains("+") || lower.contains("-") || lower.contains("*") || lower.contains("/") ||
                lower.contains("status") || lower.contains("weather") || lower.contains("brightness") ||
                lower.contains("volume") || lower.contains("silent") || lower.contains("setting")

        if (isFactOrCommand) return 0.3

        // High temperature (0.8) for creative tasks, jokes, poems, stories, fun chat
        val isCreative = lower.contains("joke") || lower.contains("story") || lower.contains("poem") ||
                lower.contains("creative") || lower.contains("funny") || lower.contains("imagine") ||
                lower.contains("riddle") || lower.contains("game") || lower.contains("song") ||
                lower.contains("fun") || lower.contains("tell me about yourself")

        if (isCreative) return 0.8

        // Default medium (0.6)
        return 0.6
    }
}
