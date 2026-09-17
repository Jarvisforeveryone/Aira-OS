package com.aira.assistant.data.repositories

import com.aira.assistant.data.Command
import com.aira.assistant.domain.Result
import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    fun getVoiceCommands(): Flow<List<Command>>
    suspend fun saveVoiceCommand(command: Command): Result<Long>
    suspend fun processVoiceInput(input: String): Result<String>
}
