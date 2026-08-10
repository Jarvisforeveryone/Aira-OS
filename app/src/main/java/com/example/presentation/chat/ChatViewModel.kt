package com.example.presentation.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppModule
import com.example.domain.Result
import com.example.domain.models.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val appModule = AppModule(application)
    private val getChatHistoryUseCase = appModule.getChatHistoryUseCase
    private val chatRepository = appModule.chatRepository

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadChatHistory()
    }

    fun loadChatHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getChatHistoryUseCase().collect { history ->
                _uiState.value = _uiState.value.copy(
                    messages = history,
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    fun sendMessage(text: String, isUser: Boolean = true) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val msg = ChatMessage(
                sender = if (isUser) "user" else "aira",
                message = text.trim()
            )
            val result = chatRepository.sendMessage(msg)
            if (result is Result.Error) {
                _uiState.value = _uiState.value.copy(errorMessage = result.message)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val result = chatRepository.clearHistory()
            if (result is Result.Error) {
                _uiState.value = _uiState.value.copy(errorMessage = result.message)
            }
        }
    }
}
