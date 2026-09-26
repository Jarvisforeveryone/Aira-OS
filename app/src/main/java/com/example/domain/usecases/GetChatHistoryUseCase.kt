package com.example.domain.usecases

import com.example.domain.models.ChatMessage
import com.example.domain.repositories.ChatRepository
import kotlinx.coroutines.flow.Flow

class GetChatHistoryUseCase(private val chatRepository: ChatRepository) {
    operator fun invoke(): Flow<List<ChatMessage>> {
        return chatRepository.getMessages()
    }
}
