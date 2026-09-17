package com.aira.assistant.presentation.chat

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aira.assistant.data.AppDatabase
import com.aira.assistant.data.ChatMessage
import com.aira.assistant.data.Memory
import com.aira.assistant.data.ResponseFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import com.aira.assistant.presentation.common.EventBus
import com.aira.assistant.presentation.common.AiraEvent

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatMessageDao()
    private val feedbackDao = db.responseFeedbackDao()
    private val memoryDao = db.memoryDao()
    private val prefs = application.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)

    val chatMessages: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatHistory: StateFlow<List<ChatMessage>> = chatMessages

    val feedbackList: StateFlow<List<ResponseFeedback>> = feedbackDao.getAllFeedback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<Memory>> = memoryDao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _userMessageInput = MutableStateFlow("")
    val userMessageInput: StateFlow<String> = _userMessageInput.asStateFlow()

    private val _smartReplies = MutableStateFlow<List<String>>(
        listOf("Tell me a fun fact", "What is the weather today?", "Set an alarm for 7 AM", "Turn on flashlight")
    )
    val smartReplies: StateFlow<List<String>> = _smartReplies.asStateFlow()

    private val _isOfflineBrain = MutableStateFlow(prefs.getBoolean("offline_brain_enabled", false))
    val isOfflineBrain: StateFlow<Boolean> = _isOfflineBrain.asStateFlow()

    val reduceAnimations: StateFlow<Boolean> = MutableStateFlow(prefs.getBoolean("reduce_animations", false)).asStateFlow()
    val themeIndex: StateFlow<Int> = MutableStateFlow(prefs.getInt("theme_index", 0)).asStateFlow()

    init {
        viewModelScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is AiraEvent.ChatMessageSubmitted -> {
                        sendMessage(event.query, isVoice = event.isVoice)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onInputChanged(text: String) {
        _userMessageInput.value = text
    }

    fun sendUserInput(query: String) {
        sendMessage(query, isVoice = false)
    }

    fun sendMessage(query: String, isVoice: Boolean = false) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _userMessageInput.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            val userMsg = ChatMessage(
                sender = "user",
                message = trimmed,
                timestamp = System.currentTimeMillis(),
                isOffline = _isOfflineBrain.value
            )
            chatDao.insertMessage(userMsg)
            _isGenerating.value = true

            EventBus.emit(AiraEvent.StatusUpdated("AIRA is thinking..."))

            // Provide immediate smart reply / local response
            val response = "Received: \"$trimmed\". Assistant engine is processing your request."
            addAssistantMessage(response, isOffline = _isOfflineBrain.value)
            _isGenerating.value = false
            EventBus.emit(AiraEvent.StatusUpdated("Ready"))
        }
    }

    fun addAssistantMessage(response: String, isOffline: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val assistantMsg = ChatMessage(
                sender = "aira",
                message = response,
                timestamp = System.currentTimeMillis(),
                isOffline = isOffline
            )
            chatDao.insertMessage(assistantMsg)
            _isGenerating.value = false
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.clearHistory()
            EventBus.emit(AiraEvent.ToastRequested("Chat history cleared"))
        }
    }

    fun toggleOfflineBrain(enabled: Boolean) {
        _isOfflineBrain.value = enabled
        prefs.edit().putBoolean("offline_brain_enabled", enabled).apply()
    }

    fun submitFeedback(
        messageId: Long?,
        query: String,
        response: String,
        isPositive: Boolean,
        comment: String? = null,
        onSubmitted: (Long) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val feedback = ResponseFeedback(
                messageId = messageId,
                query = query,
                response = response,
                feedbackType = if (isPositive) "POSITIVE" else "NEGATIVE",
                comment = comment?.ifBlank { null }
            )
            val insertedId = feedbackDao.insertFeedback(feedback)
            withContext(Dispatchers.Main) {
                onSubmitted(insertedId)
            }
        }
    }

    fun clearAllFeedback() {
        viewModelScope.launch(Dispatchers.IO) {
            feedbackDao.clearAllFeedback()
        }
    }

    fun addMemoryManual(fact: String, category: String, isImportant: Boolean = false) {
        val trimmed = fact.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val mem = Memory(
                factText = trimmed,
                source = "manual",
                category = category.ifBlank { "Personal" },
                createdAt = System.currentTimeMillis(),
                isImportant = isImportant
            )
            memoryDao.insertMemory(mem)
        }
    }

    fun updateMemory(memory: Memory) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.updateMemory(memory)
        }
    }

    fun deleteMemory(memory: Memory) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.deleteMemory(memory.id)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.deleteMemory(id)
        }
    }

    fun toggleMemoryImportant(memory: Memory) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.updateMemory(memory.copy(isImportant = !memory.isImportant))
        }
    }

    fun clearMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.clearMemories()
        }
    }

    fun exportMemoriesToDownloads(context: Context): String {
        return try {
            val all = memories.value
            val array = JSONArray()
            for (m in all) {
                val obj = JSONObject().apply {
                    put("fact", m.factText)
                    put("category", m.category)
                    put("source", m.source)
                    put("isImportant", m.isImportant)
                    put("createdAt", m.createdAt)
                }
                array.put(obj)
            }
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "aira_memories_export.json")
            file.writeText(array.toString(2))
            "Exported ${all.size} memories to Downloads"
        } catch (e: Exception) {
            "Export failed: ${e.message}"
        }
    }

    fun importMemoriesFromDownloads(context: Context): String {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "aira_memories_export.json")
            if (!file.exists()) return "File aira_memories_export.json not found in Downloads"
            val text = file.readText()
            val array = JSONArray(text)
            viewModelScope.launch(Dispatchers.IO) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    memoryDao.insertMemory(
                        Memory(
                            factText = obj.optString("fact", obj.optString("factText")),
                            source = obj.optString("source", "manual"),
                            category = obj.optString("category", "Personal"),
                            isImportant = obj.optBoolean("isImportant", false),
                            createdAt = obj.optLong("createdAt", obj.optLong("timestamp", System.currentTimeMillis()))
                        )
                    )
                }
            }
            "Imported ${array.length()} memories successfully"
        } catch (e: Exception) {
            "Export failed: ${e.message}"
        }
    }
}
