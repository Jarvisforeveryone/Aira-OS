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

    private val sarcasmTriggers = setOf(
        "oh great", "thanks a lot", "yeah right", "sure thing", "brilliant job", "nice going", "wonderful work"
    )

    private val frustrationTriggers = setOf(
        "again", "still not", "never mind", "forget it", "whatever", "slow", "useless", "wrong"
    )

    fun analyzeSentiment(text: String, history: List<Pair<String, String>> = emptyList()): SentimentResult {
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

        // Contextual Sarcasm Detection
        var isSarcastic = false
        for (trigger in sarcasmTriggers) {
            if (lower.contains(trigger)) {
                isSarcastic = true
                break
            }
        }

        // Contextual Frustration Tracking from recent history
        var contextFrustrationScore = 0
        if (history.isNotEmpty()) {
            val lastUserTurn = history.lastOrNull { it.first.isNotBlank() }?.first?.lowercase() ?: ""
            if (frustrationTriggers.any { lower.contains(it) || lastUserTurn.contains(it) }) {
                contextFrustrationScore += 2
            }
        }

        if (lower.contains("!") || lower.contains("!!")) {
            intensityScore += 2
        }

        val totalLexiconHits = posCount + negCount
        var rawValence = if (totalLexiconHits > 0) {
            (posCount - negCount).toFloat() / totalLexiconHits.toFloat()
        } else {
            0.0f
        }

        if (isSarcastic) {
            rawValence = -0.6f
            foundKeywords.add("sarcasm_detected")
            intensityScore += 2
        }

        if (contextFrustrationScore > 0) {
            rawValence -= 0.3f
            intensityScore += contextFrustrationScore
        }

        val intensity = when {
            intensityScore >= 3 || hasHighModifier -> EmotionIntensity.HIGH
            intensityScore in 1..2 -> EmotionIntensity.MEDIUM
            else -> EmotionIntensity.LOW
        }

        val detectedEmotion = when {
            isSarcastic -> UserEmotion.ANGRY
            rawValence > 0.3f -> UserEmotion.HAPPY
            rawValence < -0.3f && (tokens.contains("angry") || tokens.contains("hate") || tokens.contains("furious") || tokens.contains("annoyed") || contextFrustrationScore > 0) -> UserEmotion.ANGRY
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
