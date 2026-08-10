package com.example.data.repositories

import com.example.data.ChatMessageDao
import com.example.domain.Result
import com.example.domain.models.ChatMessage
import com.example.domain.repositories.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(
    private val chatMessageDao: ChatMessageDao
) : ChatRepository {

    override fun getMessages(): Flow<List<ChatMessage>> {
        return chatMessageDao.getAllMessages().map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    sender = entity.sender,
                    message = entity.message,
                    timestamp = entity.timestamp,
                    isOffline = entity.isOffline
                )
            }
        }
    }

    override suspend fun sendMessage(message: ChatMessage): Result<Long> {
        return try {
            val entity = com.example.data.ChatMessage(
                id = message.id,
                sender = message.sender,
                message = message.message,
                timestamp = message.timestamp,
                isOffline = message.isOffline
            )
            val id = chatMessageDao.insertMessage(entity)
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
