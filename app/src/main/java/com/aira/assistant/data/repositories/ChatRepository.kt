package com.aira.assistant.data.repositories

import com.aira.assistant.data.ChatMessage
import com.aira.assistant.domain.Result
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun sendMessage(message: ChatMessage): Result<Long>
    suspend fun clearHistory(): Result<Unit>
}
