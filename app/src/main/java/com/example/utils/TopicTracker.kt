package com.example.utils

import java.util.ArrayDeque
import java.util.Locale

/**
 * AIRA TOPIC MEMORY & STACK SYSTEM
 * Tracks ongoing conversation topics, maintains a topic stack, detects topic shifts,
 * and enables smooth context-aware follow-up answers.
 */
class TopicTracker(private val maxStackSize: Int = 5) {

    private val topicStack = ArrayDeque<String>()

    init {
        topicStack.push("General Conversation")
    }

    /**
     * Detects conversation topic from user message input.
     */
    fun detectTopic(input: String): String {
        val lower = input.lowercase(Locale.ROOT)

        return when {
            lower.contains("weather") || lower.contains("rain") || lower.contains("temperature") ||
                    lower.contains("forecast") || lower.contains("climate") || lower.contains("sun") ->
                "Weather & Climate"

            lower.contains("wifi") || lower.contains("bluetooth") || lower.contains("flashlight") ||
                    lower.contains("torch") || lower.contains("brightness") || lower.contains("volume") ||
                    lower.contains("silent") || lower.contains("alarm") || lower.contains("timer") ||
                    lower.contains("camera") || lower.contains("device") ->
                "Device Controls & Settings"

            lower.contains("my name") || lower.contains("remember") || lower.contains("favorite") ||
                    lower.contains("preference") || lower.contains("note") || lower.contains("recall") ||
                    lower.contains("i like") || lower.contains("memory") ->
                "Personal Facts & User Preferences"

            lower.contains("llama") || lower.contains("gemini") || lower.contains("ai") ||
                    lower.contains("model") || lower.contains("code") || lower.contains("android") ||
                    lower.contains("software") || lower.contains("groq") || lower.contains("vosk") ||
                    lower.contains("piper") ->
                "Technology & Artificial Intelligence"

            lower.contains("calculate") || lower.contains("math") || lower.contains("sum") ||
                    lower.contains("count") || lower.contains("+") || lower.contains("-") ||
                    lower.contains("*") || lower.contains("/") || lower.contains("percentage") ->
                "Mathematics & Computation"

            lower.contains("joke") || lower.contains("story") || lower.contains("movie") ||
                    lower.contains("music") || lower.contains("song") || lower.contains("game") ||
                    lower.contains("funny") || lower.contains("poem") ->
                "Entertainment & Stories"

            lower.contains("health") || lower.contains("fitness") || lower.contains("exercise") ||
                    lower.contains("diet") || lower.contains("doctor") || lower.contains("sleep") ||
                    lower.contains("water") || lower.contains("steps") ->
                "Health & Wellness"

            lower.contains("news") || lower.contains("headline") || lower.contains("article") ||
                    lower.contains("current events") ->
                "News & Headlines"

            else -> "General Conversation"
        }
    }

    /**
     * Processes new input: detects topic, updates topic stack if topic shifted,
     * and returns the current topic.
     */
    @Synchronized
    fun processInput(input: String): String {
        val detected = detectTopic(input)

        // Only push if topic changed from current top topic
        if (topicStack.isEmpty() || topicStack.peek() != detected) {
            topicStack.push(detected)
            while (topicStack.size > maxStackSize) {
                topicStack.removeLast()
            }
        }

        return detected
    }

    /**
     * Returns current active top topic.
     */
    @Synchronized
    fun getCurrentTopic(): String {
        return topicStack.peek() ?: "General Conversation"
    }

    /**
     * Returns full active topic stack ordered from most recent to oldest.
     */
    @Synchronized
    fun getTopicHistory(): List<String> {
        return topicStack.toList()
    }

    /**
     * Builds system instruction prompt snippet describing active conversation topics for contextual continuity.
     */
    @Synchronized
    fun buildTopicContextPrompt(): String {
        val current = getCurrentTopic()
        val history = getTopicHistory().joinToString(" -> ")
        return "ACTIVE TOPIC CONTEXT: Current Topic: '$current'. Topic Stack: [$history]. Use this topic memory to seamlessly interpret follow-up questions and maintain conversational context."
    }

    /**
     * Clears topic stack.
     */
    @Synchronized
    fun reset() {
        topicStack.clear()
        topicStack.push("General Conversation")
    }
}
