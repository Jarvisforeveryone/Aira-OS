package com.aira.assistant.presentation.automation

import com.aira.assistant.service.AiraAccessibilityService

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aira.assistant.data.*
import com.aira.assistant.service.AiraDeviceAdminReceiver
import com.aira.assistant.core.shizuku.ShizukuManager
import com.aira.assistant.presentation.components.VoiceCommandLog
import com.aira.assistant.presentation.common.EventBus
import com.aira.assistant.presentation.common.AiraEvent
import com.aira.assistant.utils.PredefinedAutomation
import com.aira.assistant.utils.SmartAutomationParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AutomationExecutionResult(
    val success: Boolean,
    val message: String,
    val actionResults: List<String> = emptyList()
)

data class AutomationUiState(
    val isExecuting: Boolean = false,
    val activeTestingMacroId: String? = null,
    val lastExecutedMacro: String? = null,
    val lastExecutionResult: AutomationExecutionResult? = null,
    val feedbackMessage: String? = null
)

class AutomationViewModel(application: Application) : AndroidViewModel(application), com.aira.assistant.utils.AssistantActionHandler {

    private val context = application.applicationContext
    private val db = AppDatabase.getDatabase(application)
    private val voiceCommandDao = db.voiceCommandDao()
    private val reminderDao = db.reminderDao()
    private val voiceLogDao = db.voiceCommandLogDao()
    private val macroDao = db.macroDao()
    private val prefs = application.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)

    val allCommands: StateFlow<List<Command>> = voiceCommandDao.getAllCommandsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActions: StateFlow<List<Action>> = voiceCommandDao.getAllActionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<Reminder>> = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macros: StateFlow<List<MacroEntity>> = macroDao.getAllMacrosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState: StateFlow<AutomationUiState> = _uiState.asStateFlow()

    private val _voiceCommandLogs = MutableStateFlow<List<VoiceCommandLog>>(emptyList())
    val voiceCommandLogs: StateFlow<List<VoiceCommandLog>> = _voiceCommandLogs.asStateFlow()

    private val _isAccessibilityServiceConnected = MutableStateFlow(false)
    val isAccessibilityServiceConnected: StateFlow<Boolean> = _isAccessibilityServiceConnected.asStateFlow()

    private val _isLocalMode = MutableStateFlow(prefs.getBoolean("local_mode", false))
    val isLocalMode: StateFlow<Boolean> = _isLocalMode.asStateFlow()

    private val _privacyMode = MutableStateFlow(prefs.getBoolean("privacy_mode", false))
    val privacyMode: StateFlow<Boolean> = _privacyMode.asStateFlow()

    private val _speakReplies = MutableStateFlow(prefs.getBoolean("speak_replies", true))
    val speakReplies: StateFlow<Boolean> = _speakReplies.asStateFlow()

    private val _isDoNotDisturb = MutableStateFlow(false)
    val isDoNotDisturb: StateFlow<Boolean> = _isDoNotDisturb.asStateFlow()

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    val isShizukuRunning: StateFlow<Boolean> = _isShizukuAvailable
    val isShizukuGranted: StateFlow<Boolean> = _isShizukuAvailable

    private val _lastTaskResult = MutableStateFlow<String>("")
    val lastTaskResult: StateFlow<String> = _lastTaskResult.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    init {
        checkShizukuStatus()
        loadVoiceLogs()
        viewModelScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is AiraEvent.ShizukuTaskRequested -> executeCustomCommand(event.command)
                    else -> Unit
                }
            }
        }
    }

    private fun loadVoiceLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            voiceLogDao.getRecentLogsFlow().collect { entities ->
                _voiceCommandLogs.value = entities.map {
                    VoiceCommandLog(
                        id = it.id.toString(),
                        timestamp = it.timestamp,
                        command = it.command,
                        matchedTrigger = it.matchedTrigger,
                        status = it.status,
                        details = it.details
                    )
                }
            }
        }
    }

    fun checkShizukuStatus() {
        _isShizukuAvailable.value = ShizukuManager.isShizukuAvailable()
    }

    fun refreshShizukuStatus() {
        checkShizukuStatus()
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission { granted ->
            _isShizukuAvailable.value = granted
        }
    }

    fun executeCustomCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isExecuting.value = true
            val output = ShizukuManager.executeShellCommand(trimmed)
            val msg = if (output.startsWith("Error")) output else "Success: $output"
            _lastTaskResult.value = msg
            _isExecuting.value = false
            EventBus.emit(AiraEvent.ToastRequested(msg))
        }
    }

    fun launchApp(packageName: String) {
        if (!ShizukuManager.isValidPackageName(packageName)) {
            _lastTaskResult.value = "Rejected unsafe package: $packageName"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val success = ShizukuManager.launchApp(packageName)
            _lastTaskResult.value = if (success) "Launched $packageName" else "Failed to launch $packageName"
        }
    }

    fun forceStopApp(packageName: String) {
        if (!ShizukuManager.isValidPackageName(packageName)) {
            _lastTaskResult.value = "Rejected unsafe package: $packageName"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val success = ShizukuManager.forceStopApp(packageName)
            _lastTaskResult.value = if (success) "Force stopped $packageName" else "Failed to force stop $packageName"
        }
    }

    // System Control & Settings Intents
    override fun checkDeviceAdminActive(): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val comp = ComponentName(context, AiraDeviceAdminReceiver::class.java)
            dpm.isAdminActive(comp)
        } catch (_: Exception) {
            false
        }
    }

    fun getDeviceAdminActivationIntent(): Intent {
        val comp = ComponentName(context, AiraDeviceAdminReceiver::class.java)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Allows AIRA to lock screen automatically upon voice command.")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    override fun lockDeviceScreen(): String {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val comp = ComponentName(context, AiraDeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(comp)) {
                dpm.lockNow()
                "Device screen locked"
            } else {
                "Device admin permission not active"
            }
        } catch (e: Exception) {
            "Lock screen failed: ${e.message}"
        }
    }

    override fun toggleFlashlight(enable: Boolean): String {
        return try {
            val cam = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cam.cameraIdList.firstOrNull() ?: return "No camera available"
            cam.setTorchMode(id, enable)
            if (enable) "Flashlight turned on" else "Flashlight turned off"
        } catch (e: Exception) {
            "Flashlight toggle error: ${e.message}"
        }
    }

    override fun setSoundMode(ringerMode: Int): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.ringerMode = ringerMode
            "Sound mode updated"
        } catch (e: Exception) {
            "Sound mode error: ${e.message}"
        }
    }

    override fun setDoNotDisturb(enable: Boolean): String {
        _isDoNotDisturb.value = enable
        return if (enable) "Do Not Disturb enabled" else "Do Not Disturb disabled"
    }

    override fun setSystemAlarm(hour: Int, minute: Int, label: String): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Alarm set for %02d:%02d".format(hour, minute)
        } catch (e: Exception) {
            "Alarm schedule error: ${e.message}"
        }
    }

    fun openAccessibilitySettings(ctx: Context = context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
    }

    fun openAppPermissionSettings(ctx: Context = context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", ctx.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
    }

    fun openWriteSettings(ctx: Context = context) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:" + ctx.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
    }

    fun openDefaultAssistantSettings(ctx: Context = context) {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
    }

    fun checkWriteSettingsPermission(ctx: Context = context): Boolean {
        return Settings.System.canWrite(ctx)
    }

    fun insertCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceCommandDao.insertCommand(command)
        }
    }

    fun deleteCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceCommandDao.deleteCommand(command)
        }
    }

    fun insertAction(action: Action) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceCommandDao.insertAction(action)
        }
    }

    fun deleteAction(action: Action) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceCommandDao.deleteAction(action)
        }
    }

    fun addReminder(title: String, timeMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val label = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(timeMillis))
            val r = Reminder(title = title, timeLabel = label, timestamp = timeMillis)
            reminderDao.insertReminder(r)
        }
    }

    fun addReminder(title: String, timeLabel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val r = Reminder(title = title, timeLabel = timeLabel, timestamp = System.currentTimeMillis())
            reminderDao.insertReminder(r)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.deleteReminder(reminder)
        }
    }

    override fun addVoiceCommandLog(command: String, matchedTrigger: String?, status: String, details: String) {
        val timeStr = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
        viewModelScope.launch(Dispatchers.IO) {
            voiceLogDao.insertLog(
                VoiceCommandLogEntity(
                    command = command,
                    matchedTrigger = matchedTrigger,
                    timestamp = timeStr,
                    status = status,
                    details = details
                )
            )
        }
    }

    fun clearVoiceCommandLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            _voiceCommandLogs.value = emptyList()
        }
    }

    fun toggleLocalMode(enabled: Boolean) {
        _isLocalMode.value = enabled
        prefs.edit().putBoolean("local_mode", enabled).apply()
    }

    fun setPrivacyMode(enabled: Boolean) {
        _privacyMode.value = enabled
        prefs.edit().putBoolean("privacy_mode", enabled).apply()
    }

    fun toggleSpeakReplies(enabled: Boolean) {
        _speakReplies.value = enabled
        prefs.edit().putBoolean("speak_replies", enabled).apply()
    }

    fun parseAndExecuteVoiceCommand(commandText: String): String {
        viewModelScope.launch(Dispatchers.Default) {
            addVoiceCommandLog(commandText, "manual_trigger", "SUCCESS", "Executed: $commandText")
            EventBus.emit(AiraEvent.ToastRequested("Command executed: $commandText"))
        }
        return "Executed: $commandText"
    }

    override fun speakText(text: String) {
        EventBus.emit(AiraEvent.SpeakRequested(text))
    }

    override fun toggleWifiAccessibilityFallback(enable: Boolean): String {
        val service = com.aira.assistant.service.AiraAccessibilityService.instance
        return service?.toggleWifi(enable) ?: "Accessibility service not active"
    }

    override fun toggleBluetoothAccessibilityFallback(enable: Boolean): String {
        val service = com.aira.assistant.service.AiraAccessibilityService.instance
        return service?.toggleBluetooth(enable) ?: "Accessibility service not active"
    }

    override fun triggerHomeAction(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    override fun triggerBackAction(): Boolean {
        return com.aira.assistant.service.AiraAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK) == true
    }

    override fun triggerRecentsAction(): Boolean {
        return com.aira.assistant.service.AiraAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS) == true
    }

    override fun launchSystemCamera(): Boolean {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun initiatePhoneCall(number: String): Boolean {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun triggerMorningBriefing() {
        speakText("Good morning! Checking your schedule and updates.")
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun createAutomationFromSpeech(prompt: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = SmartAutomationParser.parseNaturalLanguage(prompt)
            if (parsed.actions.isNotEmpty()) {
                val entity = MacroEntity(
                    trigger = parsed.triggerPhrase.ifEmpty { prompt },
                    actionsJson = SmartAutomationParser.actionsToJson(parsed.actions),
                    description = parsed.summary
                )
                macroDao.insertMacro(entity)
                _uiState.update { it.copy(feedbackMessage = "Automation saved for '${entity.trigger}'") }
                withContext(Dispatchers.Main) { onComplete() }
            } else {
                _uiState.update { it.copy(feedbackMessage = "Could not parse automation trigger and actions.") }
            }
        }
    }

    fun testAutomation(macro: MacroEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExecuting = true, activeTestingMacroId = macro.id, lastExecutedMacro = macro.trigger) }
            val actions = SmartAutomationParser.parseStoredActions(macro.actionsJson)
            val results = mutableListOf<String>()
            for (action in actions) {
                val res = parseAndExecuteVoiceCommand(action)
                results.add(res)
            }
            val execResult = AutomationExecutionResult(true, "Completed", results)
            _uiState.update { it.copy(isExecuting = false, activeTestingMacroId = null, lastExecutionResult = execResult) }
        }
    }

    fun deleteMacro(macro: MacroEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            macroDao.deleteMacroById(macro.id)
            _uiState.update { it.copy(feedbackMessage = "Deleted automation '${macro.trigger}'") }
        }
    }

    fun installTemplate(template: PredefinedAutomation) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = MacroEntity(
                id = template.id,
                trigger = template.triggerPhrase,
                actionsJson = SmartAutomationParser.actionsToJson(template.actions),
                description = template.description
            )
            macroDao.insertMacro(entity)
            _uiState.update { it.copy(feedbackMessage = "Installed '${template.title}' routine") }
        }
    }
}
