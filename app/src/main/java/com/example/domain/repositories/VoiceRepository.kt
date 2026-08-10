package com.example.domain.repositories

import com.example.domain.Result
import com.example.domain.models.VoiceCommand
import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    fun getVoiceCommands(): Flow<List<VoiceCommand>>
    suspend fun saveVoiceCommand(command: VoiceCommand): Result<Long>
    suspend fun processVoiceInput(input: String): Result<String>
}
