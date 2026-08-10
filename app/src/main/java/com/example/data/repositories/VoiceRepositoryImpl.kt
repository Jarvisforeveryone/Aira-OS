package com.example.data.repositories

import android.content.Context
import com.example.data.VoiceCommandDao
import com.example.domain.Result
import com.example.domain.models.VoiceCommand
import com.example.domain.repositories.VoiceRepository
import com.example.service.ShizukuVoiceExecutionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VoiceRepositoryImpl(
    private val context: Context,
    private val voiceCommandDao: VoiceCommandDao
) : VoiceRepository {

    override fun getVoiceCommands(): Flow<List<VoiceCommand>> {
        return voiceCommandDao.getAllCommandsFlow().map { entities ->
            entities.map { entity ->
                VoiceCommand(
                    id = entity.id,
                    phrase = entity.triggerPhrase,
                    action = entity.actionIdsJson,
                    isEnabled = true
                )
            }
        }
    }

    override suspend fun saveVoiceCommand(command: VoiceCommand): Result<Long> {
        return try {
            val entity = com.example.data.Command(
                id = command.id,
                triggerPhrase = command.phrase,
                actionIdsJson = command.action
            )
            val id = voiceCommandDao.insertCommand(entity)
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
