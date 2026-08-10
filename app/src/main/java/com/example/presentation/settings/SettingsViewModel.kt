package com.example.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppModule
import com.example.utils.ShizukuManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "system",
    val isShizukuRunning: Boolean = false,
    val isShizukuGranted: Boolean = false,
    val speakReplies: Boolean = true,
    val currentWakeWord: String = "AIRA"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appModule = AppModule(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshShizukuStatus()
    }

    fun setThemeMode(mode: String) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun refreshShizukuStatus() {
        viewModelScope.launch {
            val isRunning = ShizukuManager.isShizukuRunning()
            val isGranted = ShizukuManager.isPermissionGranted()
            _uiState.value = _uiState.value.copy(
                isShizukuRunning = isRunning,
                isShizukuGranted = isGranted
            )
        }
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission { granted ->
            _uiState.value = _uiState.value.copy(
                isShizukuGranted = granted,
                isShizukuRunning = ShizukuManager.isShizukuRunning()
            )
        }
    }

    fun toggleSpeakReplies(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(speakReplies = enabled)
    }

    fun updateWakeWord(word: String) {
        _uiState.value = _uiState.value.copy(currentWakeWord = word)
    }
}
