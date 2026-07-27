package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "Memory")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "factText") val factText: String,
    @ColumnInfo(name = "source") val source: String, // "auto", "manual", "voice", "offline_ai", "online_ai"
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "category", defaultValue = "Personal") val category: String = "Personal", // Personal, Work, Tasks, Reminders, Preferences
    @ColumnInfo(name = "isImportant", defaultValue = "0") val isImportant: Boolean = false
)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM Memory ORDER BY isImportant DESC, createdAt DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM Memory ORDER BY isImportant DESC, createdAt DESC")
    suspend fun getAllMemoriesList(): List<Memory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory): Long

    @Update
    suspend fun updateMemory(memory: Memory)

    @Query("DELETE FROM Memory WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM Memory")
    suspend fun clearMemories()
}
