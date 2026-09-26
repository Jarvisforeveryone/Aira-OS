package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "macro_templates")
data class MacroEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val trigger: String, // Trigger phrase, e.g., "goodnight", "focus mode", "morning routine"
    val actionsJson: String, // JSON array string of individual commands
    val description: String = ""
)

@Dao
interface MacroDao {
    @Query("SELECT * FROM macro_templates ORDER BY trigger ASC")
    fun getAllMacrosFlow(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macro_templates ORDER BY trigger ASC")
    suspend fun getAllMacros(): List<MacroEntity>

    @Query("SELECT * FROM macro_templates WHERE LOWER(trigger) = LOWER(:trigger) LIMIT 1")
    suspend fun getMacroByTrigger(trigger: String): MacroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: MacroEntity)

    @Query("DELETE FROM macro_templates WHERE id = :id")
    suspend fun deleteMacroById(id: String)
}
