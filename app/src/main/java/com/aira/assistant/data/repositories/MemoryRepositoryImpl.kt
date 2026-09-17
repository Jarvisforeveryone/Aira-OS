package com.aira.assistant.data.repositories

import com.aira.assistant.data.Memory
import com.aira.assistant.data.MemoryDao
import com.aira.assistant.domain.Result
import com.aira.assistant.data.repositories.MemoryRepository
import kotlinx.coroutines.flow.Flow

class MemoryRepositoryImpl(
    private val memoryDao: MemoryDao
) : MemoryRepository {

    override fun getMemories(): Flow<List<Memory>> {
        return memoryDao.getAllMemories()
    }

    override suspend fun saveMemory(memory: Memory): Result<Long> {
        return try {
            val id = memoryDao.insertMemory(memory)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e, "Failed to save memory")
        }
    }

    override suspend fun deleteMemory(id: Long): Result<Unit> {
        return try {
            memoryDao.deleteMemory(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete memory")
        }
    }

    override suspend fun clearAllMemories(): Result<Unit> {
        return try {
            memoryDao.clearMemories()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to clear memories")
        }
    }
}
