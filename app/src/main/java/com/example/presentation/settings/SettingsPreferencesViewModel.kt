package com.example.presentation.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ResponseFeedback
import com.example.network.api.ApiProviderType
import com.example.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsPreferencesUiState(
    val selectedTheme: String = "system",
    val accentColorHex: String = "#6200EE",
    val isAmoled: Boolean = false,
    val isDynamicColor: Boolean = true,
    val selectedProvider: ApiProviderType = ApiProviderType.GEMINI,
    val isThinkingModeEnabled: Boolean = false,
    val wakeWordSensitivity: Float = 0.5f,
    val statusFeedback: String? = null
)

/**
 * Feature ViewModel for Themes, UI customisation, Multi-API keys,
 * Response Feedback and Performance Cache clearing.
 */
class SettingsPreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val feedbackDao = db.responseFeedbackDao()
    private val grokCacheDao = db.grokCacheDao()
    private val weatherCacheDao = db.weatherCacheDao()
    private val queryCacheDao = db.queryCacheDao()

    val feedbacks: StateFlow<List<ResponseFeedback>> = feedbackDao.getAllFeedback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(SettingsPreferencesUiState())
    val uiState: StateFlow<SettingsPreferencesUiState> = _uiState.asStateFlow()

    private val prefs = application.getSharedPreferences("aira_settings_prefs", Context.MODE_PRIVATE)

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val theme = prefs.getString("theme_mode", "system") ?: "system"
        val accent = prefs.getString("accent_color", "#6200EE") ?: "#6200EE"
        val amoled = prefs.getBoolean("is_amoled", false)
        val dynamic = prefs.getBoolean("is_dynamic", true)
        val thinking = prefs.getBoolean("enable_thinking", false)
        val sensitivity = prefs.getFloat("wake_word_sensitivity", 0.5f)

        _uiState.value = _uiState.value.copy(
            selectedTheme = theme,
            accentColorHex = accent,
            isAmoled = amoled,
            isDynamicColor = dynamic,
            isThinkingModeEnabled = thinking,
            wakeWordSensitivity = sensitivity
        )
    }

    fun setThemeMode(mode: String) {
        _uiState.value = _uiState.value.copy(selectedTheme = mode)
        prefs.edit().putString("theme_mode", mode).apply()
        Logger.d("SettingsPreferencesViewModel", "Theme set: $mode")
    }

    fun setAccentColor(hex: String) {
        _uiState.value = _uiState.value.copy(accentColorHex = hex)
        prefs.edit().putString("accent_color", hex).apply()
    }

    fun setAmoled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAmoled = enabled)
        prefs.edit().putBoolean("is_amoled", enabled).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDynamicColor = enabled)
        prefs.edit().putBoolean("is_dynamic", enabled).apply()
    }

    fun setThinkingMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isThinkingModeEnabled = enabled)
        prefs.edit().putBoolean("enable_thinking", enabled).apply()
    }

    fun setWakeWordSensitivity(sensitivity: Float) {
        _uiState.value = _uiState.value.copy(wakeWordSensitivity = sensitivity)
        prefs.edit().putFloat("wake_word_sensitivity", sensitivity).apply()
    }

    fun submitFeedback(userQuery: String, aiResponse: String, isThumbsUp: Boolean, comment: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val fb = ResponseFeedback(
                query = userQuery,
                response = aiResponse,
                feedbackType = if (isThumbsUp) "POSITIVE" else "NEGATIVE",
                comment = comment,
                timestamp = System.currentTimeMillis()
            )
            feedbackDao.insertFeedback(fb)
            Logger.d("SettingsPreferencesViewModel", "Feedback submitted: thumbsUp=$isThumbsUp")
        }
    }

    fun clearAllCaches() {
        viewModelScope.launch(Dispatchers.IO) {
            grokCacheDao.clearExpiredCaches(System.currentTimeMillis() + 1000000000L)
            weatherCacheDao.clearAll()
            queryCacheDao.clearAll()
            Logger.d("SettingsPreferencesViewModel", "Cleared all response & weather caches")
        }
    }
}
