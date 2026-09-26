package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "trained_wake_words")
data class TrainedWakeWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word") val word: String,
    @ColumnInfo(name = "quality") val quality: String, // e.g. "Excellent", "Good", "Fair"
    @ColumnInfo(name = "attempts_json") val attemptsJson: String, // JSON array of transcribed attempts
    @ColumnInfo(name = "isActive") val isActive: Boolean = false,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface TrainedWakeWordDao {
    @Query("SELECT * FROM trained_wake_words ORDER BY createdAt DESC")
    fun getAllTrainedWakeWordsFlow(): Flow<List<TrainedWakeWord>>

    @Query("SELECT * FROM trained_wake_words")
    suspend fun getAllTrainedWakeWords(): List<TrainedWakeWord>

    @Query("SELECT * FROM trained_wake_words WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveWakeWord(): TrainedWakeWord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainedWakeWord(wakeWord: TrainedWakeWord): Long

    @Query("UPDATE trained_wake_words SET isActive = 0")
    suspend fun deactivateAll()

    @Transaction
    suspend fun setActiveWakeWord(id: Long) {
        deactivateAll()
        setActiveQuery(id)
    }

    @Query("UPDATE trained_wake_words SET isActive = 1 WHERE id = :id")
    suspend fun setActiveQuery(id: Long)

    @Delete
    suspend fun deleteTrainedWakeWord(wakeWord: TrainedWakeWord)

    @Query("DELETE FROM trained_wake_words WHERE id = :id")
    suspend fun deleteById(id: Long)
}
