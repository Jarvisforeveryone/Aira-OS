package com.example.data.repositories

import com.example.data.*
import com.example.domain.repositories.AppRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of AppRepository coordinating across Room DAOs.
 */
class AppRepositoryImpl(
    private val database: AppDatabase
) : AppRepository {

    override fun getAllChatMessagesFlow(): Flow<List<ChatMessage>> {
        return database.chatMessageDao().getAllMessages()
    }

    override suspend fun insertChatMessage(message: ChatMessage): Long {
        return database.chatMessageDao().insertMessage(message)
    }

    override suspend fun clearChatHistory() {
        database.chatMessageDao().clearHistory()
    }

    override fun getAllMemoriesFlow(): Flow<List<Memory>> {
        return database.memoryDao().getAllMemories()
    }

    override suspend fun getAllMemories(): List<Memory> {
        return database.memoryDao().getAllMemoriesList()
    }

    override suspend fun insertMemory(memory: Memory): Long {
        return database.memoryDao().insertMemory(memory)
    }

    override suspend fun deleteMemory(id: Long) {
        database.memoryDao().deleteMemory(id)
    }

    override fun getAllCommandsFlow(): Flow<List<Command>> {
        return database.voiceCommandDao().getAllCommandsFlow()
    }

    override suspend fun getAllCommands(): List<Command> {
        return database.voiceCommandDao().getAllCommands()
    }

    override suspend fun insertCommand(command: Command): Long {
        return database.voiceCommandDao().insertCommand(command)
    }

    override suspend fun insertVoiceCommandLog(log: VoiceCommandLogEntity) {
        database.voiceCommandLogDao().insertLog(log)
    }

    override fun getRecentVoiceCommandLogsFlow(): Flow<List<VoiceCommandLogEntity>> {
        return database.voiceCommandLogDao().getRecentLogsFlow()
    }

    override fun getAllRemindersFlow(): Flow<List<Reminder>> {
        return database.reminderDao().getAllReminders()
    }

    override suspend fun insertReminder(reminder: Reminder): Long {
        return database.reminderDao().insertReminder(reminder)
    }

    override suspend fun deleteReminder(reminder: Reminder) {
        database.reminderDao().deleteReminder(reminder)
    }

    override fun getAllMacrosFlow(): Flow<List<MacroEntity>> {
        return database.macroDao().getAllMacrosFlow()
    }

    override suspend fun getAllMacros(): List<MacroEntity> {
        return database.macroDao().getAllMacros()
    }

    override suspend fun insertMacro(macro: MacroEntity) {
        database.macroDao().insertMacro(macro)
    }

    override suspend fun deleteMacro(id: String) {
        database.macroDao().deleteMacroById(id)
    }
}
