package com.example.presentation.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppModule
import com.example.domain.Result
import com.example.domain.models.VoiceCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoiceUiState(
    val commands: List<VoiceCommand> = emptyList(),
    val isListening: Boolean = false,
    val lastResultText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val appModule = AppModule(application)
    private val processVoiceCommandUseCase = appModule.processVoiceCommandUseCase
    private val voiceRepository = appModule.voiceRepository

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    init {
        loadCommands()
    }

    fun loadCommands() {
        viewModelScope.launch {
            voiceRepository.getVoiceCommands().collect { cmdList ->
                _uiState.value = _uiState.value.copy(commands = cmdList)
            }
        }
    }

    fun processVoiceInput(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = processVoiceCommandUseCase(input)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        lastResultText = result.data,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        lastResultText = result.message,
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun setListeningState(isListening: Boolean) {
        _uiState.value = _uiState.value.copy(isListening = isListening)
    }
}
