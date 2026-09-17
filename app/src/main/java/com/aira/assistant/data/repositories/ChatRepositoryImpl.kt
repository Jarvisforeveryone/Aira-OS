package com.aira.assistant.data.repositories

import com.aira.assistant.data.ChatMessage
import com.aira.assistant.data.ChatMessageDao
import com.aira.assistant.domain.Result
import com.aira.assistant.data.repositories.ChatRepository
import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(
    private val chatMessageDao: ChatMessageDao
) : ChatRepository {

    override fun getMessages(): Flow<List<ChatMessage>> {
        return chatMessageDao.getAllMessages()
    }

    override suspend fun sendMessage(message: ChatMessage): Result<Long> {
        return try {
            val id = chatMessageDao.insertMessage(message)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e, "Failed to insert chat message")
        }
    }

    override suspend fun clearHistory(): Result<Unit> {
        return try {
            chatMessageDao.clearHistory()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to clear chat history")
        }
    }
}
