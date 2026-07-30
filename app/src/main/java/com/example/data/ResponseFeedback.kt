package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "response_feedback")
data class ResponseFeedback(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "message_id") val messageId: Long? = null,
    @ColumnInfo(name = "query") val query: String,
    @ColumnInfo(name = "response") val response: String,
    @ColumnInfo(name = "feedback_type") val feedbackType: String, // "POSITIVE" or "NEGATIVE"
    @ColumnInfo(name = "comment") val comment: String? = null,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ResponseFeedbackDao {
    @Query("SELECT * FROM response_feedback ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<ResponseFeedback>>

    @Query("SELECT * FROM response_feedback WHERE message_id = :messageId LIMIT 1")
    suspend fun getFeedbackForMessage(messageId: Long): ResponseFeedback?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: ResponseFeedback): Long

    @Query("UPDATE response_feedback SET comment = :comment WHERE id = :id")
    suspend fun updateComment(id: Long, comment: String)

    @Query("DELETE FROM response_feedback WHERE id = :id")
    suspend fun deleteFeedback(id: Long)

    @Query("DELETE FROM response_feedback")
    suspend fun clearAllFeedback()
}
