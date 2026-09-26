package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "voice_command_logs")
data class VoiceCommandLogEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val matchedTrigger: String?,
    val timestamp: String,
    val status: String, // "SUCCESS", "FAILED", "ABORTED"
    val details: String
)

@Dao
interface VoiceCommandLogDao {
    @Query("SELECT * FROM voice_command_logs ORDER BY rowid DESC LIMIT 30")
    fun getRecentLogsFlow(): Flow<List<VoiceCommandLogEntity>>

    @Query("SELECT * FROM voice_command_logs ORDER BY rowid DESC LIMIT 30")
    suspend fun getRecentLogs(): List<VoiceCommandLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VoiceCommandLogEntity)

    @Query("DELETE FROM voice_command_logs")
    suspend fun clearLogs()
}
