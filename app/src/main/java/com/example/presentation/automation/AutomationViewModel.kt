package com.example.presentation.automation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MacroEntity
import com.example.service.AiraAccessibilityService
import com.example.utils.MacroExecutionResult
import com.example.utils.MacroManager
import com.example.utils.PredefinedAutomation
import com.example.utils.ShizukuManager
import com.example.utils.SmartAutomationParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class AutomationUiState(
    val isAccessibilityActive: Boolean = false,
    val isShizukuActive: Boolean = false,
    val lastExecutedMacro: String? = null,
    val isExecuting: Boolean = false,
    val statusMessage: String = "Ready",
    val lastExecutionResult: MacroExecutionResult? = null,
    val activeTestingMacroId: String? = null,
    val currentInputPrompt: String = "",
    val feedbackMessage: String? = null
)

class AutomationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val macroDao = db.macroDao()

    val macros: StateFlow<List<MacroEntity>> = macroDao.getAllMacrosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState: StateFlow<AutomationUiState> = _uiState.asStateFlow()

    init {
        refreshAutomationStatus()
        seedDefaultTemplatesIfEmpty()
    }

    private fun seedDefaultTemplatesIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = macroDao.getAllMacros()
            if (existing.isEmpty()) {
                // Seed initial high-value automations so the app is instantly rich & ready
                for (template in SmartAutomationParser.PREDEFINED_TEMPLATES.take(4)) {
                    val entity = MacroEntity(
                        id = template.id,
                        trigger = template.triggerPhrase,
                        actionsJson = SmartAutomationParser.actionsToJson(template.actions),
                        description = template.title
                    )
                    macroDao.insertMacro(entity)
                }
            }
        }
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

    fun setInputPrompt(text: String) {
        _uiState.value = _uiState.value.copy(currentInputPrompt = text)
    }

    /**
     * Creates an automation directly from user speech or typed sentence with zero technical jargon.
     */
    fun createAutomationFromSpeech(spokenText: String, onComplete: ((String) -> Unit)? = null) {
        if (spokenText.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = SmartAutomationParser.parseNaturalLanguage(spokenText)
            val jsonActions = SmartAutomationParser.actionsToJson(parsed.actions)
            val entity = MacroEntity(
                trigger = parsed.triggerPhrase,
                actionsJson = jsonActions,
                description = parsed.title
            )
            macroDao.insertMacro(entity)
            _uiState.value = _uiState.value.copy(
                currentInputPrompt = "",
                feedbackMessage = "Created \"${parsed.title}\" with ${parsed.actions.size} actions!"
            )
            onComplete?.invoke(parsed.title)
        }
    }

    /**
     * Installs a pre-made automation template with 1 tap.
     */
    fun installTemplate(template: PredefinedAutomation) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = MacroEntity(
                id = template.id,
                trigger = template.triggerPhrase,
                actionsJson = SmartAutomationParser.actionsToJson(template.actions),
                description = template.title
            )
            macroDao.insertMacro(entity)
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Installed \"${template.title}\"!"
            )
        }
    }

    /**
     * Executes the automation immediately and returns live step-by-step results.
     */
    fun testAutomation(macro: MacroEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val title = macro.description.ifBlank { macro.trigger.replaceFirstChar { it.uppercase() } }
            _uiState.value = _uiState.value.copy(
                isExecuting = true,
                activeTestingMacroId = macro.id,
                lastExecutedMacro = title
            )

            val result = MacroManager.processMacro(getApplication(), macro.trigger)

            _uiState.value = _uiState.value.copy(
                isExecuting = false,
                activeTestingMacroId = null,
                lastExecutionResult = result,
                feedbackMessage = if (result.executed) "Executed $title successfully!" else "Execution finished: ${result.summary}"
            )
        }
    }

    fun deleteMacro(macro: MacroEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            macroDao.deleteMacroById(macro.id)
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Removed \"${macro.description.ifBlank { macro.trigger }}\""
            )
        }
    }

    fun clearFeedbackMessage() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }
}
