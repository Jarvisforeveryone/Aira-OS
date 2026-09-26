package com.example.domain.repositories

import com.example.domain.Result
import com.example.domain.models.Memory
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun getMemories(): Flow<List<Memory>>
    suspend fun saveMemory(memory: Memory): Result<Long>
    suspend fun deleteMemory(id: Long): Result<Unit>
    suspend fun clearAllMemories(): Result<Unit>
}
