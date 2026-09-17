package com.aira.assistant.domain.usecases

import com.aira.assistant.data.ChatMessage
import com.aira.assistant.data.repositories.ChatRepository
import kotlinx.coroutines.flow.Flow

class GetChatHistoryUseCase(private val chatRepository: ChatRepository) {
    operator fun invoke(): Flow<List<ChatMessage>> {
        return chatRepository.getMessages()
    }
}
