package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.data.Action
import com.example.data.Command
import com.example.data.VoiceCommandDao
import com.example.data.TrainedWakeWord
import com.example.data.TrainedWakeWordDao

@Database(entities = [ChatMessage::class, Reminder::class, GrokCache::class, Action::class, Command::class, Memory::class, TrainedWakeWord::class, ResponseFeedback::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun reminderDao(): ReminderDao
    abstract fun grokCacheDao(): GrokCacheDao
    abstract fun voiceCommandDao(): VoiceCommandDao
    abstract fun memoryDao(): MemoryDao
    abstract fun trainedWakeWordDao(): TrainedWakeWordDao
    abstract fun responseFeedbackDao(): ResponseFeedbackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aira_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
