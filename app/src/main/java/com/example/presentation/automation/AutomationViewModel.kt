package com.example.presentation.automation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MacroEntity
import com.example.service.AiraAccessibilityService
import com.example.service.AiraAutomationEngine
import com.example.utils.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AutomationUiState(
    val isAccessibilityActive: Boolean = false,
    val isShizukuActive: Boolean = false,
    val lastExecutedMacro: String? = null,
    val isExecuting: Boolean = false,
    val statusMessage: String = "Ready"
)

class AutomationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val macroDao = db.macroDao()

    val macros: StateFlow<List<MacroEntity>> = macroDao.getAllMacrosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState: StateFlow<AutomationUiState> = _uiState.asStateFlow()

    init {
        refreshAutomationStatus()
    }

    fun refreshAutomationStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val a11y = AiraAccessibilityService.instance != null
            val shizuku = ShizukuManager.isShizukuAvailable()
            _uiState.value = _uiState.value.copy(
                isAccessibilityActive = a11y,
                isShizukuActive = shizuku,
                statusMessage = if (shizuku) "Device Control Active" else "Standard Automation Ready"
            )
        }
    }

    fun executeMacro(macro: MacroEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isExecuting = true, lastExecutedMacro = macro.description.ifBlank { macro.trigger })
            val automationEngine = AiraAutomationEngine(getApplication())
            automationEngine.executeIntent(macro.actionsJson)
            _uiState.value = _uiState.value.copy(isExecuting = false)
        }
    }

    fun saveMacro(name: String, triggerPhrase: String, actionsJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val macro = MacroEntity(
                trigger = triggerPhrase,
                actionsJson = actionsJson,
                description = name
            )
            macroDao.insertMacro(macro)
        }
    }

    fun deleteMacro(macro: MacroEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            macroDao.deleteMacroById(macro.id)
        }
    }
}
