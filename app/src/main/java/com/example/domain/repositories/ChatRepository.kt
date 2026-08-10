package com.example.domain.repositories

import com.example.domain.Result
import com.example.domain.models.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun sendMessage(message: ChatMessage): Result<Long>
    suspend fun clearHistory(): Result<Unit>
}
