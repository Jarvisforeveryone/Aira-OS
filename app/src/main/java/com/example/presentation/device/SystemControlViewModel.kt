package com.example.presentation.device

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Action
import com.example.data.AppDatabase
import com.example.data.Command
import com.example.service.AiraAccessibilityService
import com.example.service.AiraDeviceAdminReceiver
import com.example.util.Logger
import com.example.utils.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SystemControlUiState(
    val isAccessibilityConnected: Boolean = false,
    val isShizukuRunning: Boolean = false,
    val isShizukuGranted: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val statusMessage: String = "Ready"
)

/**
 * Feature ViewModel for system device control, gestures, accessibility integration,
 * Shizuku privilege actions, and macro/action management.
 */
class SystemControlViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val voiceCommandDao = db.voiceCommandDao()

    val actionsList: StateFlow<List<Action>> = voiceCommandDao.getAllActionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commandsList: StateFlow<List<Command>> = voiceCommandDao.getAllCommandsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(SystemControlUiState())
    val uiState: StateFlow<SystemControlUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val isAcc = AiraAccessibilityService.instance != null
        val isShizukuRun = ShizukuManager.isShizukuRunning()
        val isShizukuPerm = ShizukuManager.isPermissionGranted()
        val isDevAdmin = checkDeviceAdminActive()

        _uiState.value = _uiState.value.copy(
            isAccessibilityConnected = isAcc,
            isShizukuRunning = isShizukuRun,
            isShizukuGranted = isShizukuPerm,
            isDeviceAdminActive = isDevAdmin
        )
    }

    fun triggerBackAction() {
        val service = AiraAccessibilityService.instance
        if (service != null) {
            service.performBackAction()
            _uiState.value = _uiState.value.copy(statusMessage = "Triggered Back gesture")
        } else {
            _uiState.value = _uiState.value.copy(statusMessage = "Accessibility service not connected")
        }
    }

    fun triggerHomeAction() {
        val service = AiraAccessibilityService.instance
        if (service != null) {
            service.performHomeAction()
            _uiState.value = _uiState.value.copy(statusMessage = "Triggered Home gesture")
        } else {
            _uiState.value = _uiState.value.copy(statusMessage = "Accessibility service not connected")
        }
    }

    fun triggerRecentsAction() {
        val service = AiraAccessibilityService.instance
        if (service != null) {
            service.performRecentsAction()
            _uiState.value = _uiState.value.copy(statusMessage = "Triggered Recents gesture")
        } else {
            _uiState.value = _uiState.value.copy(statusMessage = "Accessibility service not connected")
        }
    }

    fun lockDeviceScreen(): String {
        val app = getApplication<Application>()
        val dpm = app.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(app, AiraDeviceAdminReceiver::class.java)
        return if (dpm != null && dpm.isAdminActive(adminComponent)) {
            try {
                dpm.lockNow()
                "Device screen locked via Device Admin"
            } catch (e: Exception) {
                Logger.e("SystemControlViewModel", "Failed to lock device", e)
                "Failed to lock: ${e.message}"
            }
        } else {
            val service = AiraAccessibilityService.instance
            if (service != null) {
                val locked = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                if (locked) "Device screen locked via Accessibility Service" else "Device Admin permission not granted"
            } else {
                "Device Admin permission not granted"
            }
        }
    }

    fun checkDeviceAdminActive(): Boolean {
        val app = getApplication<Application>()
        val dpm = app.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(app, AiraDeviceAdminReceiver::class.java)
        return dpm?.isAdminActive(adminComponent) == true
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission { granted ->
            _uiState.value = _uiState.value.copy(
                isShizukuGranted = granted,
                isShizukuRunning = ShizukuManager.isShizukuRunning()
            )
        }
    }

    fun openAccessibilitySettings() {
        val app = getApplication<Application>()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        app.startActivity(intent)
    }

    fun openWriteSettings() {
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            app.startActivity(intent)
        }
    }

    fun openDefaultAssistantSettings() {
        val app = getApplication<Application>()
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            app.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                app.startActivity(fallback)
            } catch (e2: Exception) {
                Logger.e("SystemControlViewModel", "Failed to open assistant settings", e2)
            }
        }
    }

    fun openAppPermissionSettings() {
        val app = getApplication<Application>()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", app.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        app.startActivity(intent)
    }

    // Action and Command entity management
    fun insertAction(action: Action) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceCommandDao.insertAction(action)
        }
    }

    fun updateAction(action: Action) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceCommandDao.updateAction(action)
        }
    }

    fun deleteAction(action: Action) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceCommandDao.deleteAction(action)
        }
    }

    fun insertCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceCommandDao.insertCommand(command)
        }
    }

    fun updateCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceCommandDao.updateCommand(command)
        }
    }

    fun deleteCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceCommandDao.deleteCommand(command)
        }
    }
}
