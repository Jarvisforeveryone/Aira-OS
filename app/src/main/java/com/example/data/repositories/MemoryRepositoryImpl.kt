package com.example.data.repositories

import com.example.data.MemoryDao
import com.example.domain.Result
import com.example.domain.models.Memory
import com.example.domain.repositories.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoryRepositoryImpl(
    private val memoryDao: MemoryDao
) : MemoryRepository {

    override fun getMemories(): Flow<List<Memory>> {
        return memoryDao.getAllMemories().map { entities ->
            entities.map { entity ->
                Memory(
                    id = entity.id,
                    factText = entity.factText,
                    source = entity.source,
                    createdAt = entity.createdAt,
                    category = entity.category,
                    isImportant = entity.isImportant
                )
            }
        }
    }

    override suspend fun saveMemory(memory: Memory): Result<Long> {
        return try {
            val entity = com.example.data.Memory(
                id = memory.id,
                factText = memory.factText,
                source = memory.source,
                createdAt = memory.createdAt,
                category = memory.category,
                isImportant = memory.isImportant
            )
            val id = memoryDao.insertMemory(entity)
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
