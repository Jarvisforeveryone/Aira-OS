package com.aira.assistant.data.repositories

import android.content.Context
import com.aira.assistant.data.Command
import com.aira.assistant.data.VoiceCommandDao
import com.aira.assistant.domain.Result
import com.aira.assistant.data.repositories.VoiceRepository
import com.aira.assistant.service.ShizukuVoiceExecutionService
import kotlinx.coroutines.flow.Flow

class VoiceRepositoryImpl(
    private val context: Context,
    private val voiceCommandDao: VoiceCommandDao
) : VoiceRepository {

    override fun getVoiceCommands(): Flow<List<Command>> {
        return voiceCommandDao.getAllCommandsFlow()
    }

    override suspend fun saveVoiceCommand(command: Command): Result<Long> {
        return try {
            val id = voiceCommandDao.insertCommand(command)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e, "Failed to save voice command")
        }
    }

    override suspend fun processVoiceInput(input: String): Result<String> {
        return try {
            val response = ShizukuVoiceExecutionService.executeVoiceCommand(context, input)
            if (response.isSuccess) {
                Result.Success(response.responseMessage)
            } else {
                Result.Error(Exception(response.responseMessage), response.responseMessage)
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to execute voice command: ${e.localizedMessage}")
        }
    }
}
