package com.aira.assistant.di

import android.content.Context
import com.aira.assistant.data.AppDatabase
import com.aira.assistant.data.repositories.ChatRepositoryImpl
import com.aira.assistant.data.repositories.MemoryRepositoryImpl
import com.aira.assistant.data.repositories.QueryCacheRepository
import com.aira.assistant.data.repositories.QueryCacheRepositoryImpl
import com.aira.assistant.data.repositories.VoiceRepositoryImpl
import com.aira.assistant.data.repositories.WeatherCacheRepository
import com.aira.assistant.data.repositories.WeatherCacheRepositoryImpl
import com.aira.assistant.data.repositories.ChatRepository
import com.aira.assistant.data.repositories.MemoryRepository
import com.aira.assistant.data.repositories.VoiceRepository
import com.aira.assistant.domain.usecases.GetChatHistoryUseCase
import com.aira.assistant.domain.usecases.GetWeatherUseCase
import com.aira.assistant.domain.usecases.ProcessVoiceCommandUseCase
import com.aira.assistant.domain.usecases.SaveMemoryUseCase
import com.aira.assistant.presentation.common.PermissionManager

class AppModule(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val weatherCacheRepository: WeatherCacheRepository by lazy {
        WeatherCacheRepositoryImpl(database.weatherCacheDao())
    }

    val queryCacheRepository: QueryCacheRepository by lazy {
        QueryCacheRepositoryImpl(database.queryCacheDao())
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepositoryImpl(database.chatMessageDao())
    }

    val memoryRepository: MemoryRepository by lazy {
        MemoryRepositoryImpl(database.memoryDao())
    }

    val voiceRepository: VoiceRepository by lazy {
        VoiceRepositoryImpl(context, database.voiceCommandDao())
    }

    val getChatHistoryUseCase: GetChatHistoryUseCase by lazy {
        GetChatHistoryUseCase(chatRepository)
    }

    val saveMemoryUseCase: SaveMemoryUseCase by lazy {
        SaveMemoryUseCase(memoryRepository)
    }

    val processVoiceCommandUseCase: ProcessVoiceCommandUseCase by lazy {
        ProcessVoiceCommandUseCase(voiceRepository)
    }

    val getWeatherUseCase: GetWeatherUseCase by lazy {
        GetWeatherUseCase(weatherCacheRepository = weatherCacheRepository)
    }

    val appRepository: com.aira.assistant.data.repositories.AppRepository by lazy {
        com.aira.assistant.data.repositories.AppRepositoryImpl(database)
    }

    val cacheManager: com.aira.assistant.data.CacheManager by lazy {
        com.aira.assistant.data.CacheManager.getInstance(context)
    }

    val securityManager: com.aira.assistant.core.security.SecurityManager by lazy {
        com.aira.assistant.core.security.SecurityManager.getInstance(context)
    }

    val fileManager: com.aira.assistant.utils.FileManager by lazy {
        com.aira.assistant.utils.FileManager.getInstance(context)
    }

    val permissionManager: PermissionManager by lazy {
        PermissionManager.getInstance(context)
    }
}
