package com.aira.assistant.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "reminders",
    indices = [Index(value = ["timestamp"])]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "time_label") val timeLabel: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY timestamp DESC LIMIT 50")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("UPDATE reminders SET is_completed = :completed WHERE id = :id")
    suspend fun updateCompletion(id: Long, completed: Boolean)
}
