package com.example.domain.repositories

import com.example.data.*
import kotlinx.coroutines.flow.Flow

/**
 * Unified repository interface for AIRA OS data layer.
 */
interface AppRepository {
    // Chat
    fun getAllChatMessagesFlow(): Flow<List<ChatMessage>>
    suspend fun insertChatMessage(message: ChatMessage): Long
    suspend fun clearChatHistory()

    // Memories
    fun getAllMemoriesFlow(): Flow<List<Memory>>
    suspend fun getAllMemories(): List<Memory>
    suspend fun insertMemory(memory: Memory): Long
    suspend fun deleteMemory(id: Long)

    // Voice Commands & Logs
    fun getAllCommandsFlow(): Flow<List<Command>>
    suspend fun getAllCommands(): List<Command>
    suspend fun insertCommand(command: Command): Long
    suspend fun insertVoiceCommandLog(log: VoiceCommandLogEntity)
    fun getRecentVoiceCommandLogsFlow(): Flow<List<VoiceCommandLogEntity>>

    // Reminders
    fun getAllRemindersFlow(): Flow<List<Reminder>>
    suspend fun insertReminder(reminder: Reminder): Long
    suspend fun deleteReminder(reminder: Reminder)

    // Macros
    fun getAllMacrosFlow(): Flow<List<MacroEntity>>
    suspend fun getAllMacros(): List<MacroEntity>
    suspend fun insertMacro(macro: MacroEntity)
    suspend fun deleteMacro(id: String)
}
