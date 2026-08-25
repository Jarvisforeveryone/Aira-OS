package com.example.di

import android.content.Context
import com.example.data.*

/**
 * Module providing database and DAO dependencies.
 */
object DatabaseModule {

    fun provideDatabase(context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    fun provideMemoryDao(database: AppDatabase): MemoryDao {
        return database.memoryDao()
    }

    fun provideVoiceCommandDao(database: AppDatabase): VoiceCommandDao {
        return database.voiceCommandDao()
    }

    fun provideReminderDao(database: AppDatabase): ReminderDao {
        return database.reminderDao()
    }

    fun provideWeatherCacheDao(database: AppDatabase): WeatherCacheDao {
        return database.weatherCacheDao()
    }

    fun provideQueryCacheDao(database: AppDatabase): QueryCacheDao {
        return database.queryCacheDao()
    }

    fun provideMacroDao(database: AppDatabase): MacroDao {
        return database.macroDao()
    }

    fun provideTrainedWakeWordDao(database: AppDatabase): TrainedWakeWordDao {
        return database.trainedWakeWordDao()
    }

    fun provideResponseFeedbackDao(database: AppDatabase): ResponseFeedbackDao {
        return database.responseFeedbackDao()
    }

    fun provideVoiceCommandLogDao(database: AppDatabase): VoiceCommandLogDao {
        return database.voiceCommandLogDao()
    }
}
