package com.aira.assistant.domain.usecases

import com.aira.assistant.domain.Result
import com.aira.assistant.data.Memory
import com.aira.assistant.data.repositories.MemoryRepository

class SaveMemoryUseCase(private val memoryRepository: MemoryRepository) {
    suspend operator fun invoke(factText: String, category: String = "Personal", isImportant: Boolean = false): Result<Long> {
        if (factText.isBlank()) {
            return Result.Error(IllegalArgumentException("Memory fact text cannot be empty"))
        }
        val memory = Memory(
            factText = factText.trim(),
            source = "manual",
            category = category,
            isImportant = isImportant
        )
        return memoryRepository.saveMemory(memory)
    }
}
