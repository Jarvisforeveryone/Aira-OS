package com.example.domain.usecases

import com.example.domain.Result
import com.example.domain.models.Memory
import com.example.domain.repositories.MemoryRepository

class SaveMemoryUseCase(private val memoryRepository: MemoryRepository) {
    suspend operator fun invoke(factText: String, category: String = "Personal", isImportant: Boolean = false): Result<Long> {
        if (factText.isBlank()) {
            return Result.Error(IllegalArgumentException("Memory fact text cannot be empty"))
        }
        val memory = Memory(
            factText = factText.trim(),
            category = category,
            isImportant = isImportant
        )
        return memoryRepository.saveMemory(memory)
    }
}
