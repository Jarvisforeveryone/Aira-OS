package com.example.di

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.repositories.ChatRepositoryImpl
import com.example.data.repositories.MemoryRepositoryImpl
import com.example.data.repositories.VoiceRepositoryImpl
import com.example.domain.repositories.ChatRepository
import com.example.domain.repositories.MemoryRepository
import com.example.domain.repositories.VoiceRepository
import com.example.domain.usecases.GetChatHistoryUseCase
import com.example.domain.usecases.GetWeatherUseCase
import com.example.domain.usecases.ProcessVoiceCommandUseCase
import com.example.domain.usecases.SaveMemoryUseCase
import com.example.presentation.common.PermissionManager

class AppModule(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
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
        GetWeatherUseCase()
    }

    val permissionManager: PermissionManager by lazy {
        PermissionManager.getInstance(context)
    }
}
