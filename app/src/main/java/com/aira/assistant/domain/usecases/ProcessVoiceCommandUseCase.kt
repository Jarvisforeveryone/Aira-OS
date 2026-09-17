package com.aira.assistant.domain.usecases

import com.aira.assistant.domain.Result
import com.aira.assistant.data.repositories.VoiceRepository

class ProcessVoiceCommandUseCase(private val voiceRepository: VoiceRepository) {
    suspend operator fun invoke(input: String): Result<String> {
        if (input.isBlank()) {
            return Result.Error(IllegalArgumentException("Voice input cannot be empty"))
        }
        return voiceRepository.processVoiceInput(input.trim())
    }
}
