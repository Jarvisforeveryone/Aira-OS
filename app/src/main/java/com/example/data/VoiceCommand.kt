package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "actions")
data class Action(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: String, // SYSTEM_API, INTENT, SHELL, DELAY
    @ColumnInfo(name = "params_json") val paramsJson: String // JSON string to hold action parameters
)

@Entity(tableName = "commands")
data class Command(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "trigger_phrase") val triggerPhrase: String,
    @ColumnInfo(name = "action_ids_json") val actionIdsJson: String, // " [1, 2, 3] "
    @ColumnInfo(name = "priority") val priority: Int = 1,
    @ColumnInfo(name = "conditions_json") val conditionsJson: String = "", // e.g. "{"batteryLt":20, "timeRange":"NIGHT"}"
    @ColumnInfo(name = "use_count", defaultValue = "0") val useCount: Int = 0
)

@Dao
interface VoiceCommandDao {
    @Query("SELECT * FROM actions ORDER BY name ASC")
    fun getAllActionsFlow(): Flow<List<Action>>

    @Query("SELECT * FROM actions")
    suspend fun getAllActions(): List<Action>

    @Query("SELECT * FROM actions WHERE id = :id")
    suspend fun getActionById(id: Long): Action?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: Action): Long

    @Update
    suspend fun updateAction(action: Action)

    @Delete
    suspend fun deleteAction(action: Action)

    @Query("SELECT * FROM commands ORDER BY priority DESC, id DESC")
    fun getAllCommandsFlow(): Flow<List<Command>>

    @Query("SELECT * FROM commands")
    suspend fun getAllCommands(): List<Command>

    @Query("SELECT * FROM commands WHERE id = :id")
    suspend fun getCommandById(id: Long): Command?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: Command): Long

    @Update
    suspend fun updateCommand(command: Command)

    @Delete
    suspend fun deleteCommand(command: Command)
}
