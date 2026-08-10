package com.example.domain.usecases

import com.example.domain.Result
import com.example.domain.repositories.VoiceRepository

class ProcessVoiceCommandUseCase(private val voiceRepository: VoiceRepository) {
    suspend operator fun invoke(input: String): Result<String> {
        if (input.isBlank()) {
            return Result.Error(IllegalArgumentException("Voice input cannot be empty"))
        }
        return voiceRepository.processVoiceInput(input.trim())
    }
}
