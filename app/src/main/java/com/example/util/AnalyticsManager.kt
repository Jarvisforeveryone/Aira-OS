package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AnalyticsEvent(
    val eventName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val params: Map<String, String> = emptyMap()
)

/**
 * Privacy-first analytics tracker.
 * No PII recorded. Tracks screen views, feature interactions, and diagnostic metrics locally.
 */
object AnalyticsManager {
    private val _events = MutableStateFlow<List<AnalyticsEvent>>(emptyList())
    val events: StateFlow<List<AnalyticsEvent>> = _events.asStateFlow()

    fun logScreenView(screenName: String) {
        logEvent("screen_view", mapOf("screen_name" to screenName))
    }

    fun logFeatureUsed(featureName: String, detail: String = "") {
        logEvent("feature_used", mapOf("feature" to featureName, "detail" to detail))
    }

    fun logError(errorSource: String, errorMessage: String) {
        logEvent("app_error", mapOf("source" to errorSource, "message" to errorMessage))
    }

    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        val event = AnalyticsEvent(eventName = eventName, params = params)
        AppLogger.i("Analytics", "Event: $eventName | $params")
        val current = _events.value.toMutableList()
        current.add(event)
        if (current.size > 100) {
            current.removeAt(0)
        }
        _events.value = current
    }
}
