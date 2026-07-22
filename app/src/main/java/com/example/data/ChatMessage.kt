package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sender") val sender: String, // "user" or "aira"
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_offline") val isOffline: Boolean = false
)

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}
