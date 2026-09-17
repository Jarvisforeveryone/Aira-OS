package com.aira.assistant.data.repositories

import com.aira.assistant.data.Memory
import com.aira.assistant.domain.Result
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun getMemories(): Flow<List<Memory>>
    suspend fun saveMemory(memory: Memory): Result<Long>
    suspend fun deleteMemory(id: Long): Result<Unit>
    suspend fun clearAllMemories(): Result<Unit>
}
