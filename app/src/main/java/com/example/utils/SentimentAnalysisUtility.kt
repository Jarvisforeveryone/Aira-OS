package com.example.utils

import com.example.models.EmotionIntensity
import com.example.models.EmotionState
import com.example.models.UserEmotion

data class SentimentResult(
    val emotion: UserEmotion,
    val valence: Float, // Range -1.0 (very negative) to +1.0 (very positive)
    val intensity: EmotionIntensity,
    val dominantKeywords: List<String>
)

object SentimentAnalysisUtility {

    private val positiveLexicon = setOf(
        "happy", "excited", "awesome", "great", "yay", "love", "good", "fantastic",
        "wonderful", "delighted", "cheer", "brilliant", "thrilled", "glad", "enjoy",
        "splendid", "marvellous", "capital", "excellent", "superb", "pleased", "thanks", "thank"
    )

    private val negativeLexicon = setOf(
        "sad", "depressed", "upset", "sorry", "lonely", "crying", "hurt", "bad",
        "miserable", "grief", "unhappy", "disappointed", "heartbroken", "angry",
        "furious", "mad", "hate", "frustrated", "annoyed", "stupid", "idiot", "terrible",
        "awful", "horrible", "fail", "failed", "pain", "worst"
    )

    private val highIntensityModifiers = setOf(
        "very", "extremely", "so", "really", "super", "absolutely", "totally", "deeply", "immensely"
    )

    fun analyzeSentiment(text: String): SentimentResult {
        val lower = text.lowercase().trim()
        val tokens = lower.split(Regex("[\\s,.!?\"';:]+")).filter { it.isNotEmpty() }

        var posCount = 0
        var negCount = 0
        var intensityScore = 0
        val foundKeywords = mutableListOf<String>()

        var hasHighModifier = false

        for (i in tokens.indices) {
            val token = tokens[i]
            if (highIntensityModifiers.contains(token)) {
                hasHighModifier = true
                intensityScore += 2
            }
            if (positiveLexicon.contains(token)) {
                posCount++
                foundKeywords.add(token)
            }
            if (negativeLexicon.contains(token)) {
                negCount++
                foundKeywords.add(token)
            }
        }

        if (lower.contains("!") || lower.contains("!!")) {
            intensityScore += 2
        }

        val totalLexiconHits = posCount + negCount
        val rawValence = if (totalLexiconHits > 0) {
            (posCount - negCount).toFloat() / totalLexiconHits.toFloat()
        } else {
            0.0f
        }

        val intensity = when {
            intensityScore >= 3 || hasHighModifier -> EmotionIntensity.HIGH
            intensityScore in 1..2 -> EmotionIntensity.MEDIUM
            else -> EmotionIntensity.LOW
        }

        val detectedEmotion = when {
            rawValence > 0.3f -> UserEmotion.HAPPY
            rawValence < -0.3f && (tokens.contains("angry") || tokens.contains("hate") || tokens.contains("furious") || tokens.contains("annoyed")) -> UserEmotion.ANGRY
            rawValence < -0.3f -> UserEmotion.SAD
            tokens.contains("confused") || tokens.contains("huh") || lower.contains("don't understand") -> UserEmotion.CONFUSED
            tokens.contains("why") || tokens.contains("how") || tokens.contains("curious") -> UserEmotion.CURIOSITY
            else -> UserEmotion.NEUTRAL
        }

        return SentimentResult(
            emotion = detectedEmotion,
            valence = rawValence.coerceIn(-1.0f, 1.0f),
            intensity = intensity,
            dominantKeywords = foundKeywords.distinct()
        )
    }

    fun toEmotionState(sentiment: SentimentResult): EmotionState {
        return EmotionState(
            emotion = sentiment.emotion,
            intensity = sentiment.intensity
        )
    }
}
