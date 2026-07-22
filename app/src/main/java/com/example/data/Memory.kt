package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "Memory")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "factText") val factText: String,
    @ColumnInfo(name = "source") val source: String, // "auto" or "manual"
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM Memory ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM Memory ORDER BY createdAt DESC")
    suspend fun getAllMemoriesList(): List<Memory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory): Long

    @Query("DELETE FROM Memory WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM Memory")
    suspend fun clearMemories()
}
