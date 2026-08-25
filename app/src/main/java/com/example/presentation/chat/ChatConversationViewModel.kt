package com.example.presentation.chat

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.network.stream.AiStreamEvent
import com.example.network.stream.AiStreamManager
import com.example.network.stream.AiStreamState
import com.example.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatConversationUiState(
    val isGenerating: Boolean = false,
    val isThinking: Boolean = false,
    val streamingText: String = "",
    val thinkingText: String = "",
    val activeProviderName: String = "Gemini",
    val activeModelName: String = "gemini-3.5-flash",
    val errorMessage: String? = null
)

/**
 * Feature ViewModel for managing Chat conversation history, multi-turn dialogue,
 * streaming AI responses, and conversation persistence.
 */
class ChatConversationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatMessageDao()
    private val streamManager = AiStreamManager.getInstance(application)

    val messages: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ChatConversationUiState())
    val uiState: StateFlow<ChatConversationUiState> = _uiState.asStateFlow()

    private var activeStreamJob: Job? = null

    /**
     * Sends a user message and streams the AI response token-by-token asynchronously
     * using Kotlin Flow, updating StateFlow without blocking the main UI thread.
     */
    fun sendMessageWithStream(prompt: String, systemInstruction: String? = null) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank() || _uiState.value.isGenerating) return

        activeStreamJob?.cancel()

        activeStreamJob = viewModelScope.launch(Dispatchers.IO) {
            // 1. Insert user message into local Room database
            val userMsg = ChatMessage(
                sender = "user",
                message = trimmed,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(userMsg)

            // 2. Set StateFlow into generating state
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                isThinking = false,
                streamingText = "",
                thinkingText = "",
                errorMessage = null
            )

            var accumulatedResult = ""

            // 3. Stream AI tokens via reactive Flow
            streamManager.streamPrompt(trimmed, systemInstruction).collect { event ->
                when (event) {
                    is AiStreamEvent.Started -> {
                        _uiState.value = _uiState.value.copy(
                            activeProviderName = event.provider.displayName,
                            activeModelName = event.model,
                            isGenerating = true
                        )
                    }
                    is AiStreamEvent.Thinking -> {
                        _uiState.value = _uiState.value.copy(
                            isThinking = true,
                            thinkingText = event.accumulatedThoughts
                        )
                    }
                    is AiStreamEvent.Chunk -> {
                        accumulatedResult = event.accumulatedText
                        _uiState.value = _uiState.value.copy(
                            isThinking = false,
                            streamingText = event.accumulatedText
                        )
                    }
                    is AiStreamEvent.Completed -> {
                        accumulatedResult = event.fullText
                        chatDao.insertMessage(
                            ChatMessage(
                                sender = "aira",
                                message = event.fullText,
                                timestamp = System.currentTimeMillis(),
                                isOffline = false
                            )
                        )
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            isThinking = false,
                            streamingText = ""
                        )
                    }
                    is AiStreamEvent.Error -> {
                        if (accumulatedResult.isNotBlank()) {
                            chatDao.insertMessage(
                                ChatMessage(
                                    sender = "aira",
                                    message = accumulatedResult,
                                    timestamp = System.currentTimeMillis(),
                                    isOffline = false
                                )
                            )
                        }
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            isThinking = false,
                            errorMessage = event.message
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    fun cancelGeneration() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        streamManager.cancelCurrentStream()
        _uiState.value = _uiState.value.copy(
            isGenerating = false,
            isThinking = false,
            streamingText = ""
        )
    }

    fun addUserMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val userMsg = ChatMessage(
                sender = "user",
                message = text.trim(),
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(userMsg)
            Logger.d("ChatConversationViewModel", "Inserted user message: ${text.take(30)}")
        }
    }

    fun addAssistantMessage(text: String, isOffline: Boolean = false) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val botMsg = ChatMessage(
                sender = "aira",
                message = text.trim(),
                timestamp = System.currentTimeMillis(),
                isOffline = isOffline
            )
            chatDao.insertMessage(botMsg)
            Logger.d("ChatConversationViewModel", "Inserted assistant message: ${text.take(30)}")
        }
    }

    fun clearChat() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.clearHistory()
            Logger.d("ChatConversationViewModel", "Chat history cleared")
        }
    }

    fun setGenerating(generating: Boolean, thinking: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            isGenerating = generating,
            isThinking = thinking
        )
    }

    fun setStreamingText(text: String) {
        _uiState.value = _uiState.value.copy(streamingText = text)
    }

    suspend fun exportChatToDownloads(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val list = chatDao.getAllMessagesList()
            if (list.isEmpty()) return@withContext "No chat messages to export."

            val jsonArray = JSONArray()
            for (msg in list) {
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("sender", msg.sender)
                    put("message", msg.message)
                    put("timestamp", msg.timestamp)
                    put("isOffline", msg.isOffline)
                }
                jsonArray.put(obj)
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "AIRA_Chat_Export_$timestamp.json"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(jsonArray.toString(4).toByteArray(Charsets.UTF_8))
            }
            "Saved ${list.size} messages to Downloads/$fileName"
        } catch (e: Exception) {
            Logger.e("ChatConversationViewModel", "Error exporting chat", e)
            "Export failed: ${e.localizedMessage ?: "Unknown error"}"
        }
    }
}

