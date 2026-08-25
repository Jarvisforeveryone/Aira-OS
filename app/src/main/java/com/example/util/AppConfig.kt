package com.example.util

/**
 * Centralized application configuration and constants for AIRA OS.
 */
object AppConfig {
    // App Information
    const val APP_NAME = "AIRA"
    const val APP_VERSION_NAME = "1.0.0"
    const val DEFAULT_USER_NAME = "Sir"

    // Network & AI Defaults
    const val DEFAULT_AI_PROVIDER = "Gemini"
    const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
    const val DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile"
    const val DEFAULT_OPENAI_MODEL = "gpt-4o-mini"
    const val DEFAULT_CLAUDE_MODEL = "claude-3-5-sonnet-20241022"
    const val DEFAULT_OPENROUTER_MODEL = "nvidia/nemotron-3-ultra-550b-a55b:free"

    // Timeouts
    const val NETWORK_CONNECT_TIMEOUT_SECONDS = 10L
    const val NETWORK_READ_TIMEOUT_SECONDS = 30L
    const val AI_QUERY_TIMEOUT_MS = 15000L
    const val SPEECH_SILENCE_TIMEOUT_MS = 3000L

    // Database
    const val DATABASE_NAME = "aira_database"
    const val DATABASE_VERSION = 10

    // Shared Preferences & Security
    const val PREFS_SETTINGS = "aira_settings"
    const val PREFS_SECURE_KEYS = "aira_secure_keys"
    const val KEY_SELECTED_PROVIDER = "selected_ai_provider"
    const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
    const val KEY_VOSK_MODEL_READY = "vosk_model_ready"

    // Audio & Speech
    const val DEFAULT_WAKE_WORD = "hey aira"
    const val DEFAULT_TTS_PITCH = 1.0f
    const val DEFAULT_TTS_SPEED = 1.0f
}
