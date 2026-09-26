package com.example.presentation.memory

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Memory
import com.example.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MemoryUiState(
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastSavedMemory: Memory? = null
)

/**
 * Feature ViewModel dedicated to Memory Bank management, persistence,
 * search, filtering, and JSON import/export.
 */
class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val memoryDao = db.memoryDao()

    val memories: StateFlow<List<Memory>> = memoryDao.getAllMemories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    fun setSelectedCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun addMemoryManual(factText: String, category: String = "Personal", isImportant: Boolean = false) {
        if (factText.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val newMemory = Memory(
                factText = factText.trim(),
                source = "manual",
                createdAt = System.currentTimeMillis(),
                category = category,
                isImportant = isImportant
            )
            val newId = memoryDao.insertMemory(newMemory)
            val savedMem = newMemory.copy(id = newId)
            _uiState.value = _uiState.value.copy(lastSavedMemory = savedMem)
            Logger.d("MemoryViewModel", "Manual memory saved with ID: $newId")
        }
    }

    fun updateMemory(
        id: Long,
        factText: String,
        source: String,
        createdAt: Long,
        category: String = "Personal",
        isImportant: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = Memory(
                id = id,
                factText = factText.trim(),
                source = source,
                createdAt = createdAt,
                category = category,
                isImportant = isImportant
            )
            memoryDao.updateMemory(updated)
            Logger.d("MemoryViewModel", "Updated memory $id")
        }
    }

    fun toggleMemoryImportant(memory: Memory) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = memory.copy(isImportant = !memory.isImportant)
            memoryDao.updateMemory(updated)
            Logger.d("MemoryViewModel", "Toggled important for memory ${memory.id}")
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.deleteMemory(id)
            Logger.d("MemoryViewModel", "Deleted memory $id")
        }
    }

    fun clearMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.clearMemories()
            _uiState.value = _uiState.value.copy(lastSavedMemory = null)
            Logger.d("MemoryViewModel", "Cleared all memories")
        }
    }

    fun clearLastSavedMemory() {
        _uiState.value = _uiState.value.copy(lastSavedMemory = null)
    }

    suspend fun exportMemoriesToDownloads(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val list = memoryDao.getAllMemoriesList()
            if (list.isEmpty()) return@withContext "No memories to export."

            val jsonArray = JSONArray()
            for (mem in list) {
                val obj = JSONObject().apply {
                    put("id", mem.id)
                    put("factText", mem.factText)
                    put("source", mem.source)
                    put("createdAt", mem.createdAt)
                    put("category", mem.category)
                    put("isImportant", mem.isImportant)
                }
                jsonArray.put(obj)
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "AIRA_Memories_Backup_$timestamp.json"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(jsonArray.toString(4).toByteArray(Charsets.UTF_8))
            }
            "Saved ${list.size} memories to Downloads/$fileName"
        } catch (e: Exception) {
            Logger.e("MemoryViewModel", "Error exporting memories", e)
            "Export failed: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    suspend fun importMemoriesFromDownloads(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) return@withContext "Downloads folder not found."

            val backupFiles = downloadsDir.listFiles { file ->
                file.isFile && file.name.startsWith("AIRA_Memories_Backup_") && file.name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() }

            if (backupFiles.isNullOrEmpty()) {
                return@withContext "No AIRA memory backup JSON found in Downloads."
            }

            val latestBackup = backupFiles.first()
            val jsonStr = latestBackup.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(jsonStr)
            var importedCount = 0

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val fact = obj.optString("factText", "")
                if (fact.isNotBlank()) {
                    val mem = Memory(
                        factText = fact,
                        source = obj.optString("source", "backup"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        category = obj.optString("category", "Personal"),
                        isImportant = obj.optBoolean("isImportant", false)
                    )
                    memoryDao.insertMemory(mem)
                    importedCount++
                }
            }
            "Successfully restored $importedCount memories from ${latestBackup.name}"
        } catch (e: Exception) {
            Logger.e("MemoryViewModel", "Error importing memories", e)
            "Import failed: ${e.localizedMessage ?: "Invalid JSON format"}"
        }
    }
}
