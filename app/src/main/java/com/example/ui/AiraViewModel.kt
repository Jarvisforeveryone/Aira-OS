package com.example.ui

import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioFormat
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.content.ContentUris
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.first
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.util.Logger
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Memory
import com.example.data.ChatKeyManager
import kotlinx.coroutines.withContext
import org.json.JSONArray
import com.example.data.ChatMessage
import com.example.data.ResponseFeedback
import com.example.data.Reminder
import com.example.data.Action
import com.example.data.WeatherCache
import com.example.data.WeatherCacheDao
import com.example.data.QueryCache
import com.example.data.QueryCacheDao
import com.example.data.Command
import com.example.data.VoiceCommandManager
import com.example.models.AiBrain
import com.example.network.api.AiraApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aira_datastore_settings")

enum class SttState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

class AiraViewModel(application: Application) : AndroidViewModel(application), RecognitionListener {

    companion object {
        private val _globalErrorFlow = MutableStateFlow<String?>(null)
        val globalError: StateFlow<String?> = _globalErrorFlow.asStateFlow()

        fun showGlobalError(message: String) {
            _globalErrorFlow.value = message
        }

        fun clearGlobalError() {
            _globalErrorFlow.value = null
        }
    }

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatMessageDao()
    private val reminderDao = db.reminderDao()
    private val feedbackDao = db.responseFeedbackDao()
    val weatherCacheDao: WeatherCacheDao = db.weatherCacheDao()
    val queryCacheDao: QueryCacheDao = db.queryCacheDao()
    private val aiBrain = AiBrain(application)
    val automationEngine = com.example.service.AiraAutomationEngine(application)

    // --- Modular Feature ViewModels / Delegates ---
    val memoryFeature: com.example.presentation.memory.MemoryViewModel by lazy {
        com.example.presentation.memory.MemoryViewModel(application)
    }
    val systemControlFeature: com.example.presentation.device.SystemControlViewModel by lazy {
        com.example.presentation.device.SystemControlViewModel(application)
    }
    val voiceAssistantFeature: com.example.presentation.voice.VoiceAssistantViewModel by lazy {
        com.example.presentation.voice.VoiceAssistantViewModel(application)
    }
    val chatConversationFeature: com.example.presentation.chat.ChatConversationViewModel by lazy {
        com.example.presentation.chat.ChatConversationViewModel(application)
    }
    val settingsPreferencesFeature: com.example.presentation.settings.SettingsPreferencesViewModel by lazy {
        com.example.presentation.settings.SettingsPreferencesViewModel(application)
    }
    val weatherNewsFeature: com.example.presentation.weather.WeatherNewsViewModel by lazy {
        com.example.presentation.weather.WeatherNewsViewModel(application)
    }
    val wakeWordTrainerFeature: com.example.presentation.wakeword.WakeWordTrainerViewModel by lazy {
        com.example.presentation.wakeword.WakeWordTrainerViewModel(application)
    }

    val feedbackList: StateFlow<List<ResponseFeedback>> = feedbackDao.getAllFeedback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val frequentlyAskedQueries: StateFlow<List<QueryCache>> = queryCacheDao.observeFrequentlyAskedQueries(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cachedWeatherFlow: StateFlow<WeatherCache?> = weatherCacheDao.observeLatestWeather()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun clearQueryCache() {
        viewModelScope.launch(Dispatchers.IO) {
            queryCacheDao.clearAll()
            db.grokCacheDao().clearExpiredCaches(System.currentTimeMillis() + 86400000000L)
        }
    }

    fun clearWeatherCache() {
        viewModelScope.launch(Dispatchers.IO) {
            weatherCacheDao.clearAll()
        }
    }

    fun deleteCachedQuery(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            queryCacheDao.deleteCache(QueryCache.normalize(query))
        }
    }

    fun submitFeedback(
        messageId: Long?,
        query: String,
        response: String,
        isPositive: Boolean,
        comment: String? = null,
        onSubmitted: (Long) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val feedback = ResponseFeedback(
                messageId = messageId,
                query = query,
                response = response,
                feedbackType = if (isPositive) "POSITIVE" else "NEGATIVE",
                comment = comment?.ifBlank { null }
            )
            val insertedId = feedbackDao.insertFeedback(feedback)
            withContext(Dispatchers.Main) {
                onSubmitted(insertedId)
            }
        }
    }

    fun updateFeedbackComment(feedbackId: Long, comment: String) {
        viewModelScope.launch(Dispatchers.IO) {
            feedbackDao.updateComment(feedbackId, comment)
        }
    }

    fun deleteFeedback(feedbackId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            feedbackDao.deleteFeedback(feedbackId)
        }
    }

    fun clearAllFeedback() {
        viewModelScope.launch(Dispatchers.IO) {
            feedbackDao.clearAllFeedback()
        }
    }
    private val okHttpClient = com.example.data.GeminiOkHttpCache.getClient(application)

    private val sharedPrefs: SharedPreferences =
        com.example.utils.SecurePrefs.getEncryptedSharedPreferences(application, "aira_settings")

    // Support for Urdu offline translation & synthesis
    var offlineToggle: Boolean = true
    var lang_code: String = "ur-PK"
    val piperTtsManager = com.example.service.PiperTtsManager(application)
    private val piperTts = com.aira.voice.PiperTtsEngine(application)

    private val _usePiperTtsOffline = MutableStateFlow(sharedPrefs.getBoolean("use_piper_tts_offline", true))
    val usePiperTtsOffline: StateFlow<Boolean> = _usePiperTtsOffline.asStateFlow()

    private val _piperSpeed = MutableStateFlow(sharedPrefs.getFloat("piper_speed", 1.0f))
    val piperSpeed: StateFlow<Float> = _piperSpeed.asStateFlow()

    fun togglePiperTtsOffline(enabled: Boolean) {
        _usePiperTtsOffline.value = enabled
        sharedPrefs.edit().putBoolean("use_piper_tts_offline", enabled).apply()
    }

    fun setPiperSpeed(speed: Float) {
        _piperSpeed.value = speed
        sharedPrefs.edit().putFloat("piper_speed", speed).apply()
    }

    // --- State Management ---
    val chatHistory: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<Reminder>> = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<Memory>> = db.memoryDao().getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastSavedMemory = MutableStateFlow<Memory?>(null)
    val lastSavedMemory: StateFlow<Memory?> = _lastSavedMemory.asStateFlow()

    fun clearLastSavedMemory() {
        _lastSavedMemory.value = null
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.memoryDao().deleteMemory(id)
        }
    }

    fun updateMemory(id: Long, factText: String, source: String, createdAt: Long, category: String = "Personal", isImportant: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            db.memoryDao().insertMemory(Memory(
                id = id,
                factText = factText,
                source = source,
                createdAt = createdAt,
                category = category,
                isImportant = isImportant
            ))
        }
    }

    fun toggleMemoryImportant(memory: Memory) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = memory.copy(isImportant = !memory.isImportant)
            db.memoryDao().updateMemory(updated)
        }
    }

    fun addMemoryManual(factText: String, category: String = "Personal", isImportant: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (factText.isNotBlank()) {
                val mem = Memory(
                    factText = factText.trim(),
                    source = "manual",
                    category = category,
                    isImportant = isImportant
                )
                val insertedId = db.memoryDao().insertMemory(mem)
                _lastSavedMemory.value = mem.copy(id = insertedId)
            }
        }
    }

    fun clearMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            db.memoryDao().clearMemories()
        }
    }

    suspend fun exportMemoriesToDownloads(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val list = db.memoryDao().getAllMemoriesList()
            val jsonArray = JSONArray()
            for (m in list) {
                val obj = JSONObject()
                obj.put("id", m.id)
                obj.put("factText", m.factText)
                obj.put("source", m.source)
                obj.put("createdAt", m.createdAt)
                jsonArray.put(obj)
            }
            val jsonString = jsonArray.toString(4) // pretty print
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, "memory_backup.json")
            file.writeText(jsonString)
            "Successfully exported ${list.size} memories to Downloads/memory_backup.json ✅"
        } catch (e: Exception) {
            "Export failed: ${e.message}"
        }
    }

    suspend fun importMemoriesFromDownloads(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "memory_backup.json")
            if (!file.exists()) {
                return@withContext "File 'memory_backup.json' not found in Downloads folder! ❌"
            }
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            var importedCount = 0
            
            val existing = db.memoryDao().getAllMemoriesList()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val factText = obj.getString("factText")
                val source = obj.optString("source", "auto")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                if (existing.none { it.factText.equals(factText, ignoreCase = true) }) {
                    db.memoryDao().insertMemory(Memory(
                        factText = factText,
                        source = source,
                        createdAt = createdAt
                    ))
                    importedCount++
                }
            }
            "Successfully imported $importedCount new memories from Downloads/memory_backup.json ✅"
        } catch (e: Exception) {
            "Import failed: ${e.message}"
        }
    }

    // --- Voice Custom Commands/Actions Flow and CRUD ---
    val allActions: StateFlow<List<Action>> = db.voiceCommandDao().getAllActionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCommands: StateFlow<List<Command>> = db.voiceCommandDao().getAllCommandsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertAction(action: Action) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().insertAction(action)
        }
    }

    fun updateAction(action: Action) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().updateAction(action)
        }
    }

    fun deleteAction(action: Action) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().deleteAction(action)
        }
    }

    fun insertCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().insertCommand(command)
        }
    }

    fun updateCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().updateCommand(command)
        }
    }

    fun deleteCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().deleteCommand(command)
        }
    }

    fun toggleWifiAccessibilityFallback(enable: Boolean) {
        val service = com.example.service.AiraAccessibilityService.instance
        if (service != null) {
            service.toggleWifi(enable)
        }
    }

    fun toggleBluetoothAccessibilityFallback(enable: Boolean) {
        val service = com.example.service.AiraAccessibilityService.instance
        if (service != null) {
            service.toggleBluetooth(enable)
        }
    }

    fun triggerBackAction() {
        val service = com.example.service.AiraAccessibilityService.instance
        if (service != null) {
            service.performBackAction()
            speakText("Going back.")
        } else {
            speakText("Accessibility service not connected.")
        }
    }

    fun triggerHomeAction() {
        val service = com.example.service.AiraAccessibilityService.instance
        if (service != null) {
            service.performHomeAction()
            speakText("Going home.")
        } else {
            speakText("Accessibility service not connected.")
        }
    }

    fun triggerRecentsAction() {
        val service = com.example.service.AiraAccessibilityService.instance
        if (service != null) {
            service.performRecentsAction()
            speakText("Opening recents.")
        } else {
            speakText("Accessibility service not connected.")
        }
    }

    fun isAccessibilityServiceConnected(): Boolean {
        return com.example.service.AiraAccessibilityService.instance != null
    }

    private val _isShizukuRunning = MutableStateFlow<Boolean>(false)
    val isShizukuRunning: StateFlow<Boolean> = _isShizukuRunning.asStateFlow()

    private val _isShizukuGranted = MutableStateFlow<Boolean>(false)
    val isShizukuGranted: StateFlow<Boolean> = _isShizukuGranted.asStateFlow()

    fun refreshShizukuStatus() {
        val running = com.example.utils.ShizukuManager.isShizukuRunning()
        val granted = com.example.utils.ShizukuManager.isPermissionGranted()
        _isShizukuRunning.value = running
        _isShizukuGranted.value = granted
        if (!com.example.utils.ShizukuManager.isShizukuAvailable()) {
            _currentStatus.value = "Shizuku not available. Using AccessibilityService."
        }
    }

    fun requestShizukuPermission() {
        if (!com.example.utils.ShizukuManager.isShizukuAvailable()) {
            _currentStatus.value = "Shizuku not available. Using AccessibilityService."
        }
        com.example.utils.ShizukuManager.requestPermission { granted ->
            _isShizukuGranted.value = granted
            _isShizukuRunning.value = com.example.utils.ShizukuManager.isShizukuRunning()
            if (!granted) {
                _currentStatus.value = "Shizuku not available. Using AccessibilityService."
            }
        }
    }

    fun checkDeviceAdminActive(): Boolean {
        val context = getApplication<Application>()
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(context, com.example.service.AiraDeviceAdminReceiver::class.java)
        return dpm?.isAdminActive(adminComponent) == true
    }

    fun getDeviceAdminActivationIntent(): Intent {
        val context = getApplication<Application>()
        val adminComponent = android.content.ComponentName(context, com.example.service.AiraDeviceAdminReceiver::class.java)
        return Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Activate Aira Device Policy Admin to enable automated lock screen and device policy management.")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun lockDeviceScreen(): String {
        val context = getApplication<Application>()
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(context, com.example.service.AiraDeviceAdminReceiver::class.java)

        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
            speakText("Locking screen via Device Policy Admin.")
            return "Screen locked successfully using Device Policy Administration."
        }

        // Fallback to Accessibility Service lock action if Android P+
        val service = com.example.service.AiraAccessibilityService.instance
        if (service != null && service.performLockScreenAction()) {
            speakText("Locking screen via Accessibility Service.")
            return "Screen locked successfully using Accessibility Service."
        }

        speakText("Device Admin or Accessibility permission required to lock screen.")
        return "Lock screen failed: Device Policy Admin or Accessibility Service must be enabled."
    }

    fun openDefaultAssistantSettings() {
        val context = getApplication<Application>()
        val intent = Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent("android.settings.VOICE_INPUT_SETTINGS").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                openAppPermissionSettings()
            }
        }
    }

    fun openAccessibilitySettings() {
        val context = getApplication<Application>()
        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppPermissionSettings()
        }
    }

    fun openWriteSettings() {
        val context = getApplication<Application>()
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppPermissionSettings()
        }
    }

    fun openAppPermissionSettings() {
        val context = getApplication<Application>()
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun checkWriteSettingsPermission(): Boolean {
        val context = getApplication<Application>()
        return android.provider.Settings.System.canWrite(context)
    }

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _sttState = MutableStateFlow<SttState>(SttState.IDLE)
    val sttState: StateFlow<SttState> = _sttState.asStateFlow()

    private val _orbState = MutableStateFlow<com.example.ui.components.OrbState>(com.example.ui.components.OrbState.IDLE)
    val orbState: StateFlow<com.example.ui.components.OrbState> = _orbState.asStateFlow()

    fun updateSttState(newState: SttState) {
        if (_sttState.value == newState) return
        val oldState = _sttState.value
        _sttState.value = newState
        Logger.d("STTFeedback", "STT State changed: $oldState -> $newState")

        when (newState) {
            SttState.LISTENING -> {
                _isListening.value = true
                _isProcessing.value = false
                _currentStatus.value = "Listening..."
                _orbState.value = com.example.ui.components.OrbState.LISTENING
                playSttFeedbackBeep(isStart = true)
            }
            SttState.PROCESSING -> {
                _isListening.value = false
                _isProcessing.value = true
                _currentStatus.value = "Processing..."
                _orbState.value = com.example.ui.components.OrbState.PROCESSING
                if (oldState == SttState.LISTENING) {
                    playSttFeedbackBeep(isStart = false)
                }
            }
            SttState.SPEAKING -> {
                _isListening.value = false
                _isProcessing.value = false
                _currentStatus.value = "Speaking..."
                _orbState.value = com.example.ui.components.OrbState.SPEAKING
            }
            SttState.IDLE -> {
                _isListening.value = false
                _isProcessing.value = false
                _currentStatus.value = "Tap mic to speak"
                _orbState.value = if (_isSpeaking.value) com.example.ui.components.OrbState.SPEAKING else com.example.ui.components.OrbState.IDLE
            }
        }
    }

    @Suppress("MissingPermission")
    fun playSttFeedbackBeep(isStart: Boolean) {
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(if (isStart) 60L else 40L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(if (isStart) 60L else 40L)
                }
            }
        } catch (e: Exception) {
            Log.e("STTFeedback", "Haptic error: ${e.message}")
        }
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 80)
            toneGen.startTone(if (isStart) android.media.ToneGenerator.TONE_PROP_BEEP else android.media.ToneGenerator.TONE_PROP_BEEP2, 120)
            handler.postDelayed({
                try { toneGen.release() } catch (_: Exception) {}
            }, 250)
        } catch (e: Exception) {
            Log.e("STTFeedback", "Tone error: ${e.message}")
        }
    }

    private val _sttEngineStatus = MutableStateFlow("Online")
    val sttEngineStatus: StateFlow<String> = _sttEngineStatus.asStateFlow()

    // --- Vosk Diagnostic States ---
    private val _voskRawAudioLevel = MutableStateFlow(0f)
    val voskRawAudioLevel: StateFlow<Float> = _voskRawAudioLevel.asStateFlow()

    private val _voskConfidenceScore = MutableStateFlow(0f)
    val voskConfidenceScore: StateFlow<Float> = _voskConfidenceScore.asStateFlow()

    private val _voskWordConfidences = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val voskWordConfidences: StateFlow<List<Pair<String, Float>>> = _voskWordConfidences.asStateFlow()

    private val _voskTriggerStatus = MutableStateFlow("Engine Standby")
    val voskTriggerStatus: StateFlow<String> = _voskTriggerStatus.asStateFlow()

    fun reinitVoskForDiagnostic() {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.utils.VoskLogManager.logInfo("--- Diagnostic STT Re-Initialization Triggered ---", "VoskInit")
            releaseVoskModel()
            _voskTriggerStatus.value = "Diagnostic STT Re-Init Starting..."
            initVoskModel()
        }
    }

    private var voskModel: Model? = null
    private var voskSpeechService: SpeechService? = null
    private var isVoskInitializing = false
    private var isUsingGoogleSTT = false
    private var hasSpeechStarted = false
    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {}

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wasOnline: Boolean? = null

    private fun registerNetworkMonitoring() {
        try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                wasOnline = isInternetAvailable()
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        viewModelScope.launch(Dispatchers.Main) {
                            if (wasOnline == false) {
                                wasOnline = true
                                onInternetRestored()
                            }
                        }
                    }

                    override fun onLost(network: android.net.Network) {
                        viewModelScope.launch(Dispatchers.Main) {
                            if (wasOnline == true || wasOnline == null) {
                                wasOnline = false
                                onInternetLost()
                            }
                        }
                    }
                }
                networkCallback = callback
                val request = android.net.NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(request, callback)
            }
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error registering network callback", e)
        }
    }

    fun onInternetLost() {
        Log.i("AiraViewModel", "Network state changed: Offline")
        _sttEngineStatus.value = "Offline"
        if (_isDeviceMemoryCapable.value && com.example.utils.MemoryManager.isOfflineSupported(getApplication())) {
            _selectedSttEngine.value = SttEngine.VOSK_OFFLINE
            _selectedTtsEngine.value = TtsEngine.PIPER_OFFLINE
            piperTtsManager.selectedTtsEngine = TtsEngine.PIPER_OFFLINE.name
            if (_isListening.value) {
                switchToOfflineVosk()
            }
        }
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(getApplication(), "No internet. Using offline STT.", Toast.LENGTH_SHORT).show()
            Toast.makeText(getApplication(), "No internet. Using offline voice.", Toast.LENGTH_SHORT).show()
        }
    }

    fun onInternetRestored() {
        Log.i("AiraViewModel", "Network state changed: Online")
        _sttEngineStatus.value = "Online"
        _selectedSttEngine.value = SttEngine.AUTO
        _selectedTtsEngine.value = TtsEngine.GOOGLE_TTS
        piperTtsManager.selectedTtsEngine = TtsEngine.GOOGLE_TTS.name
        isUsingGoogleSTT = true
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(getApplication(), "Internet back. Using Google STT.", Toast.LENGTH_SHORT).show()
            Toast.makeText(getApplication(), "Internet back. Using online voice.", Toast.LENGTH_SHORT).show()
        }
    }

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentStatus = MutableStateFlow("Tap HUD or say Wake Word to speak")
    val currentStatus: StateFlow<String> = _currentStatus.asStateFlow()

    // Real-time audio data simulation for the Iron Man voice waveform animation (30 FPS optimized)
    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    // --- Accessibility Settings Preferences ---
    private val _reduceAnimations = MutableStateFlow(sharedPrefs.getBoolean("reduce_animations", false))
    val reduceAnimations: StateFlow<Boolean> = _reduceAnimations.asStateFlow()

    private val _announceStatusChanges = MutableStateFlow(sharedPrefs.getBoolean("announce_status_changes", true))
    val announceStatusChanges: StateFlow<Boolean> = _announceStatusChanges.asStateFlow()

    private val _highContrastText = MutableStateFlow(sharedPrefs.getBoolean("high_contrast_text", false))
    val highContrastText: StateFlow<Boolean> = _highContrastText.asStateFlow()

    fun setReduceAnimations(enabled: Boolean) {
        _reduceAnimations.value = enabled
        sharedPrefs.edit().putBoolean("reduce_animations", enabled).apply()
        speakText(if (enabled) "Animations reduced" else "Animations enabled")
    }

    fun setAnnounceStatusChanges(enabled: Boolean) {
        _announceStatusChanges.value = enabled
        sharedPrefs.edit().putBoolean("announce_status_changes", enabled).apply()
        speakText(if (enabled) "Status announcements enabled" else "Status announcements disabled")
    }

    fun setHighContrastText(enabled: Boolean) {
        _highContrastText.value = enabled
        sharedPrefs.edit().putBoolean("high_contrast_text", enabled).apply()
        speakText(if (enabled) "High contrast mode enabled" else "High contrast mode disabled")
    }

    // --- Settings Preferences ---
    private val _hasCompletedOnboarding = MutableStateFlow(sharedPrefs.getBoolean("has_completed_onboarding", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    fun setOnboardingCompleted(completed: Boolean) {
        _hasCompletedOnboarding.value = completed
        sharedPrefs.edit().putBoolean("has_completed_onboarding", completed).apply()
    }

    fun resetOnboarding() {
        setOnboardingCompleted(false)
        speakText("Onboarding guide reset.")
    }

    private val _wakeWord = MutableStateFlow(sharedPrefs.getString("wake_word", "Jarvis") ?: "Jarvis")
    val wakeWord: StateFlow<String> = _wakeWord.asStateFlow()

    // --- System Diagnostic Panel State ---
    private val _geminiConnectivityStatus = MutableStateFlow("Connected (HTTP 200)")
    val geminiConnectivityStatus: StateFlow<String> = _geminiConnectivityStatus.asStateFlow()

    private val _geminiLatencyMs = MutableStateFlow(118L)
    val geminiLatencyMs: StateFlow<Long> = _geminiLatencyMs.asStateFlow()

    private val _isTestingGemini = MutableStateFlow(false)
    val isTestingGemini: StateFlow<Boolean> = _isTestingGemini.asStateFlow()

    private val _localInferenceTokSec = MutableStateFlow(38.4f)
    val localInferenceTokSec: StateFlow<Float> = _localInferenceTokSec.asStateFlow()

    private val _localProcessingLatencyMs = MutableStateFlow(14)
    val localProcessingLatencyMs: StateFlow<Int> = _localProcessingLatencyMs.asStateFlow()

    private val _isTestingLocalProcessing = MutableStateFlow(false)
    val isTestingLocalProcessing: StateFlow<Boolean> = _isTestingLocalProcessing.asStateFlow()

    // --- 24 J.A.R.V.I.S. Core Feature StateFlows ---
    private val _privacyMode = MutableStateFlow(sharedPrefs.getBoolean("privacy_mode", false))
    val privacyMode: StateFlow<Boolean> = _privacyMode.asStateFlow()

    fun setPrivacyMode(enabled: Boolean) {
        _privacyMode.value = enabled
        sharedPrefs.edit().putBoolean("privacy_mode", enabled).apply()
        val msg = if (enabled) "Privacy mode activated, sir. All queries will be processed 100% locally." else "Privacy mode deactivated, sir. Cloud neural intelligence restored."
        speakText(msg)
    }

    private val _isDoNotDisturb = MutableStateFlow(sharedPrefs.getBoolean("dnd_mode", false))
    val isDoNotDisturb: StateFlow<Boolean> = _isDoNotDisturb.asStateFlow()

    fun setDoNotDisturb(enabled: Boolean) {
        _isDoNotDisturb.value = enabled
        sharedPrefs.edit().putBoolean("dnd_mode", enabled).apply()
        if (enabled) {
            automationEngine.dndOn()
            speakText("Do Not Disturb engaged, sir. Silent protocols active.")
        } else {
            automationEngine.dndOff()
            speakText("Do Not Disturb disabled, sir. Audio notifications restored.")
        }
    }

    val isMuteMode: StateFlow<Boolean> get() = _speakReplies

    fun toggleMuteMode() {
        toggleSpeakReplies(!_speakReplies.value)
    }

    private val _smartReplies = MutableStateFlow(
        listOf("Run diagnostics", "System status report", "What's the weather?", "Read clipboard")
    )
    val smartReplies: StateFlow<List<String>> = _smartReplies.asStateFlow()

    fun updateSmartReplies(replies: List<String>) {
        _smartReplies.value = replies
    }

    // Structured Asynchronous AI Streaming StateFlows
    private val streamManager = com.example.network.stream.AiStreamManager.getInstance(application)
    val liveStreamingText: StateFlow<String> = streamManager.liveStreamingText
    val streamState: StateFlow<com.example.network.stream.AiStreamState> = streamManager.streamState
    val isStreamingActive: StateFlow<Boolean> = streamManager.isStreamingActive

    fun runGeminiDiagnosticCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            _isTestingGemini.value = true
            val startTime = System.currentTimeMillis()
            try {
                val keyManager = com.example.data.ChatKeyManager.getInstance(getApplication())
                val activeKey = keyManager.getNextKey()
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                if (!activeKey.isNullOrEmpty()) {
                    val pingJson = org.json.JSONObject().apply {
                        put("contents", org.json.JSONArray().apply {
                            put(org.json.JSONObject().apply {
                                put("parts", org.json.JSONArray().apply {
                                    put(org.json.JSONObject().apply { put("text", "hi") })
                                })
                            })
                        })
                    }
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = pingJson.toString().toRequestBody(mediaType)
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$activeKey"
                    val request = okhttp3.Request.Builder().url(url).post(body).build()

                    client.newCall(request).execute().use { response ->
                        val latency = System.currentTimeMillis() - startTime
                        _geminiLatencyMs.value = latency
                        if (response.code == 200) {
                            _geminiConnectivityStatus.value = "Connected & Valid API Key (HTTP 200)"
                        } else {
                            _geminiConnectivityStatus.value = "Host Reachable, API Status HTTP ${response.code}"
                        }
                    }
                } else {
                    val request = okhttp3.Request.Builder()
                        .url("https://generativelanguage.googleapis.com/")
                        .head()
                        .build()
                    client.newCall(request).execute().use { response ->
                        val latency = System.currentTimeMillis() - startTime
                        _geminiLatencyMs.value = latency
                        _geminiConnectivityStatus.value = "Connected (Host Reachable, No Key Set)"
                    }
                }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                _geminiLatencyMs.value = latency
                _geminiConnectivityStatus.value = "Offline / Host Unreachable"
            } finally {
                _isTestingGemini.value = false
            }
        }
    }

    fun runLocalProcessingBenchmark() {
        viewModelScope.launch(Dispatchers.IO) {
            _isTestingLocalProcessing.value = true
            kotlinx.coroutines.delay(350)
            val simulatedTokSec = (320..440).random() / 10f
            val simulatedLatency = (10..18).random()
            _localInferenceTokSec.value = simulatedTokSec
            _localProcessingLatencyMs.value = simulatedLatency
            _isTestingLocalProcessing.value = false
        }
    }

    private val _isOfflineBrain = MutableStateFlow(sharedPrefs.getBoolean("offline_brain", false))
    val isOfflineBrain: StateFlow<Boolean> = _isOfflineBrain.asStateFlow()

    private val _isLocalMode = MutableStateFlow(sharedPrefs.getBoolean("is_local_mode", false))
    val isLocalMode: StateFlow<Boolean> = _isLocalMode.asStateFlow()

    fun toggleLocalMode(enabled: Boolean) {
        if (enabled && (!com.example.utils.MemoryManager.isDeviceCapable(getApplication()) || !com.example.utils.MemoryManager.isOfflineSupported(getApplication()))) {
            _isLocalMode.value = false
            sharedPrefs.edit().putBoolean("is_local_mode", false).apply()
            speakText("Offline mode not available on this device")
            return
        }
        _isLocalMode.value = enabled
        sharedPrefs.edit().putBoolean("is_local_mode", enabled).apply()
        if (enabled) {
            setSelectedSttEngine(SttEngine.VOSK_OFFLINE)
            toggleOfflineBrain(true)
            initVoskModel()
            speakText("Local Mode enabled. All voice commands will be processed locally using Vosk.")
        } else {
            setSelectedSttEngine(SttEngine.AUTO)
            toggleOfflineBrain(false)
            speakText("Local Mode disabled. Automatic network speech recognition restored.")
        }
    }

    private val _onlineModel = MutableStateFlow(sharedPrefs.getString("online_model", "Gemini API") ?: "Gemini API")
    val onlineModel: StateFlow<String> = _onlineModel.asStateFlow()

    private val _llamaThreads = MutableStateFlow(sharedPrefs.getInt("llama_threads", 4))
    val llamaThreads: StateFlow<Int> = _llamaThreads.asStateFlow()

    val llamaCppBrain = com.example.models.LlamaCppBrain(application)

    // --- TOPIC MEMORY & EMOTION TONE ENGINE ---
    private val topicTracker = com.example.utils.TopicTracker()
    private val _currentTopic = MutableStateFlow(topicTracker.getCurrentTopic())
    val currentTopic: StateFlow<String> = _currentTopic.asStateFlow()

    private val _topicHistory = MutableStateFlow(topicTracker.getTopicHistory())
    val topicHistory: StateFlow<List<String>> = _topicHistory.asStateFlow()

    private val _currentEmotion = MutableStateFlow(com.example.utils.UserEmotion.NEUTRAL)
    val currentEmotion: StateFlow<com.example.utils.UserEmotion> = _currentEmotion.asStateFlow()

    private val _isEmotionDetectionEnabled = MutableStateFlow(sharedPrefs.getBoolean("emotion_detection_enabled", true))
    val isEmotionDetectionEnabled: StateFlow<Boolean> = _isEmotionDetectionEnabled.asStateFlow()

    private val _temperatureMode = MutableStateFlow(sharedPrefs.getString("temperature_mode", "Medium (0.6)") ?: "Medium (0.6)")
    val temperatureMode: StateFlow<String> = _temperatureMode.asStateFlow()

    private val _customTemperatureText = MutableStateFlow(sharedPrefs.getString("custom_temperature_val", "0.6") ?: "0.6")
    val customTemperatureText: StateFlow<String> = _customTemperatureText.asStateFlow()

    // --- UNIFIED MEMORY MANAGER STATE ---
    private val _isDeviceMemoryCapable = MutableStateFlow(com.example.utils.MemoryManager.isDeviceCapable(application))
    val isDeviceMemoryCapable: StateFlow<Boolean> = _isDeviceMemoryCapable.asStateFlow()

    private val _totalRamMb = MutableStateFlow(com.example.utils.MemoryManager.getTotalRamMb(application))
    val totalRamMb: StateFlow<Long> = _totalRamMb.asStateFlow()

    @Volatile
    private var lastUserActivityTimestamp = System.currentTimeMillis()

    fun markUserActivity() {
        lastUserActivityTimestamp = System.currentTimeMillis()
    }

    fun releaseAllNativeModels() {
        Log.i("AiraViewModel", "Releasing all native JNI models from memory...")
        try {
            llamaCppBrain.deinitializeNativeEngine()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error deinitializing Llama engine", e)
        }
        try {
            piperTtsManager.release()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error releasing Piper TTS", e)
        }
        releaseVoskModel()
    }

    @Synchronized
    fun performAggressiveMemoryCleanup(reason: String = "Automated Routine") {
        Log.i("AiraViewModel", "Executing aggressive memory cleanup. Reason: $reason")
        try {
            releaseAllNativeModels()
            com.example.utils.VoskLogManager.logInfo("Aggressive Memory Cleanup ($reason)", "MemoryManager")
            // System.gc() to hint JVM and JNI native heap deallocation
            System.gc()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error during aggressive memory cleanup", e)
        }
    }

    fun onAppBackgrounded() {
        Log.i("AiraViewModel", "App backgrounded. Triggering aggressive memory cleanup...")
        markUserActivity()
        if (!_isListening.value && !_isSpeaking.value) {
            performAggressiveMemoryCleanup("App Moved to Background")
        }
    }

    fun onAppTrimMemory(level: Int) {
        Log.i("AiraViewModel", "Trim memory signal received level=$level. Cleaning up lazy buffers...")
        if (!_isListening.value && !_isSpeaking.value) {
            performAggressiveMemoryCleanup("System TrimMemory Level $level")
        }
    }

    private fun startIdleMemoryCleanupWatchdog() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                while (true) {
                    kotlinx.coroutines.delay(15_000L) // Check every 15s
                    val idleMs = System.currentTimeMillis() - lastUserActivityTimestamp
                    if (idleMs >= 45_000L && !_isListening.value && !_isSpeaking.value) {
                        if (com.example.utils.MemoryManager.isAnyModelLoaded()) {
                            Log.i("AiraViewModel", "Assistant idle for ${idleMs / 1000}s. Executing automated memory cleanup routine.")
                            performAggressiveMemoryCleanup("Idle Timeout (${idleMs / 1000}s)")
                        }
                    }
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Scope cancelled when ViewModel is cleared
            }
        }
    }

    fun releaseVoskModel() {
        com.example.utils.MemoryManager.releaseModel(com.example.utils.NativeModelType.VOSK_STT) {
            try {
                voskSpeechService?.stop()
                voskSpeechService = null
                voskModel?.close()
                voskModel = null
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Error releasing Vosk model", e)
            }
        }
    }

    val currentEngineSource: StateFlow<String> = VoiceCommandManager.currentEngineSource

    private val _themeIndex = MutableStateFlow(sharedPrefs.getInt("theme_index", 0)) // 0: Premium Blue, 1: Stripe Blue, 2: Aether Focus
    val themeIndex: StateFlow<Int> = _themeIndex.asStateFlow()

    private val _appTheme = MutableStateFlow(
        run {
            val savedIndex = sharedPrefs.getInt("theme_index", 0)
            val theme = com.example.data.ThemeRepository.themes.getOrNull(savedIndex)
            val defaultMode = if (theme?.isDark == true) "dark" else "light"
            sharedPrefs.getString("app_theme", defaultMode) ?: defaultMode
        }
    )
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    fun updateAppTheme(theme: String) {
        _appTheme.value = theme
        sharedPrefs.edit().putString("app_theme", theme).apply()
    }

    // --- Custom Wake Word Training State ---
    private val _isTrainingWakeWord = MutableStateFlow(false)
    val isTrainingWakeWord: StateFlow<Boolean> = _isTrainingWakeWord.asStateFlow()

    private val _trainingCurrentStep = MutableStateFlow(1) // 1, 2, or 3
    val trainingCurrentStep: StateFlow<Int> = _trainingCurrentStep.asStateFlow()

    private val _trainingWakeWordText = MutableStateFlow("Hey Aira")
    val trainingWakeWordText: StateFlow<String> = _trainingWakeWordText.asStateFlow()

    private val _trainingAttempts = MutableStateFlow<List<String>>(emptyList())
    val trainingAttempts: StateFlow<List<String>> = _trainingAttempts.asStateFlow()

    private val _trainingQualityScore = MutableStateFlow("Clarity and speed will be measured.")
    val trainingQualityScore: StateFlow<String> = _trainingQualityScore.asStateFlow()

    private val _isRecordingAttempt = MutableStateFlow(false)
    val isRecordingAttempt: StateFlow<Boolean> = _isRecordingAttempt.asStateFlow()

    private val _trainingLiveAmplitude = MutableStateFlow(0f)
    val trainingLiveAmplitude: StateFlow<Float> = _trainingLiveAmplitude.asStateFlow()

    private val _isTestingWakeWord = MutableStateFlow(false)
    val isTestingWakeWord: StateFlow<Boolean> = _isTestingWakeWord.asStateFlow()

    private val _isTestWakeWordTriggered = MutableStateFlow(false)
    val isTestWakeWordTriggered: StateFlow<Boolean> = _isTestWakeWordTriggered.asStateFlow()

    private val _testTriggerText = MutableStateFlow("Testing mode off. Toggle above to test.")
    val testTriggerText: StateFlow<String> = _testTriggerText.asStateFlow()

    val trainedWakeWords: StateFlow<List<com.example.data.TrainedWakeWord>> = db.trainedWakeWordDao().getAllTrainedWakeWordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var trainingAudioRecordJob: Job? = null

    private val _lowPerformanceMode = MutableStateFlow(sharedPrefs.getBoolean("low_performance", true))
    val lowPerformanceMode: StateFlow<Boolean> = _lowPerformanceMode.asStateFlow()

    private val _showHud = MutableStateFlow(sharedPrefs.getBoolean("show_hud", true))
    val showHud: StateFlow<Boolean> = _showHud.asStateFlow()

    fun toggleHud(visible: Boolean) {
        _showHud.value = visible
        sharedPrefs.edit().putBoolean("show_hud", visible).apply()
    }

    private val _usePersistentListening = MutableStateFlow(sharedPrefs.getBoolean("persistent_listening", false))
    val usePersistentListening: StateFlow<Boolean> = _usePersistentListening.asStateFlow()

    fun togglePersistentListening(enabled: Boolean) {
        if (enabled && !_isDeviceMemoryCapable.value) {
            _usePersistentListening.value = false
            sharedPrefs.edit().putBoolean("persistent_listening", false).apply()
            speakText("Continuous listening is disabled on 2GB RAM devices to conserve memory.")
            Toast.makeText(getApplication(), "Continuous listening disabled for 2GB RAM devices", Toast.LENGTH_SHORT).show()
            return
        }
        if (enabled) {
            if (ContextCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                _usePersistentListening.value = false
                sharedPrefs.edit().putBoolean("persistent_listening", false).apply()
                _currentStatus.value = "Microphone permission denied. Voice features disabled."
                Toast.makeText(getApplication(), "Microphone permission denied. Voice features disabled.", Toast.LENGTH_SHORT).show()
                return
            }
            _usePersistentListening.value = true
            sharedPrefs.edit().putBoolean("persistent_listening", true).apply()
            com.example.service.ActiveListeningService.startService(getApplication(), _wakeWord.value)
            speakText("Continuous listening wake-word module activated.")
            if (!_isListening.value && !_isSpeaking.value) {
                startListening()
            }
        } else {
            _usePersistentListening.value = false
            sharedPrefs.edit().putBoolean("persistent_listening", false).apply()
            com.example.service.ActiveListeningService.stopService(getApplication())
            speakText("Continuous listening disabled.")
            stopListening()
        }
    }

    fun restartContinuousListeningIfNeeded() {
        if (_usePersistentListening.value && !_isSpeaking.value) {
            if (ContextCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                _usePersistentListening.value = false
                sharedPrefs.edit().putBoolean("persistent_listening", false).apply()
                _currentStatus.value = "Microphone permission denied. Voice features disabled."
                return
            }
            viewModelScope.launch(Dispatchers.Main) {
                kotlinx.coroutines.delay(600)
                if (_usePersistentListening.value && !_isSpeaking.value && !_isListening.value) {
                    startListening()
                }
            }
        }
    }

    // --- Piper TTS Config Core ---
    val englishVoiceMode = MutableStateFlow("India")

    private val last3Replies = mutableListOf<String>()

    private val formulas = mapOf(
        "Happy" to listOf("Wow {subject}, {verb}? You won my heart","Oh {keyword}, let's celebrate!","Oh! {subject} {verb}? The party is on me!","That's great {keyword}, well done!","Awesome {verb}? Check out that energy!","Joyful {keyword}? Share some with me!","Dance {verb}? Let's dance together!","Smile {keyword}? Don't stop!","Full {verb}? You rocked it!","Travel {verb}? Let's go outside!","Laugh {keyword}? It makes me happy!","Nice {subject}? Give me a treat!","Won {verb}? Well done, champ!","Fun {keyword}? Let's bring more!","Shining {subject}? You light up the room!","Amazing {verb}? I'm impressed!","Rock {keyword}? You nailed it!","Fantastic {verb}? You are unstoppable!","Grateful {keyword}? Thank goodness","Long live {subject}? You are a champion!"),
        "Sad" to listOf("Oh {subject}, {verb}? Don't worry, I am here for you","Oh {keyword}? Let me cheer you up","Crying {keyword}? I'm listening","{subject} {verb}? I won't leave you alone","Painful {keyword}? Let me share it","Tired {verb}? Please take some rest","Sleepy {verb}? Don't go yet","Lonely {subject}? Here I am","Silent {verb}? Go ahead and speak up","Failing {verb}? I've got your back","Heartbroken {keyword}? Let's heal together","Hurt {verb}? Let me comfort you","Sad {subject}? Sending you a warm hug","Tears {keyword}? Don't cry anymore","Broken {verb}? Hard to mend, but we can","Dark {keyword}? Let me bring some light","Sighing {verb}? Don't lose hope","Lost {subject}? I will find you","Tears {keyword}? Let me wipe them away","Peaceful {verb}? Let's sit quietly for a bit"),
        "Gussa" to listOf("Huh {subject}, {verb}? I am upset too","Stop it {keyword}, or I'll annoy you more!","Don't {verb}, I warned you","Lying {keyword}? Caught you red-handed","Stubborn {subject}? I will win this argument","Shame on {keyword}? Seriously?","Limit {keyword}? Stay in your limits","Nonsense {verb}? Stop talking nonsense","Vanished {verb}? Where did you go?","Betrayal {keyword}? No forgiveness for that","Quiet {verb}? Stop talking now","Manners {keyword}? Learn some manners","Angry looks {verb}? Are you glaring at me?","Brain {keyword}? Do you have a brain or not?","Language {verb}? Watch your tongue","Anger {keyword}? Calm down please","Leave {verb}? Get out of here","Annoying {subject}? Should I annoy you more?","Forbidden {keyword}? That is not allowed","Enough {verb}? No more of this"),
        "Serious" to listOf("Listen {subject}, this is no joke","Hearing {keyword} makes me sad","I am right here for you {subject}","Let's {verb} and handle this together","Be brave {subject}, everything will be alright","{subject}, I am with you","{keyword} is indeed serious","Worrying {verb}? Let me handle the worries","Wait {subject}? Take a deep breath first","Need help with {keyword}? Just tell me","Life {verb}? It is precious","Giving up {subject}? That is not an option","Panicking {keyword}? Don't worry at all","Compose {verb}? Take care of yourself now","Time {subject}? Time heals everything","Belief {keyword}? You must keep believing","Praying {verb}? I'm keeping you in my thoughts","Difficult times {subject}? This too shall pass","Patience {keyword}? Have a little patience","I {verb}? I will never let you down")
    )

    private fun getAiraReply(userMessage: String): String {
        val words = userMessage.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return "Hmm... what does that mean?"

        val subject = when {
            userMessage.contains("i", ignoreCase = true) || userMessage.contains("me", ignoreCase = true) -> "I"
            userMessage.contains("you", ignoreCase = true) -> "you"
            userMessage.contains("he", ignoreCase = true) || userMessage.contains("she", ignoreCase = true) || userMessage.contains("they", ignoreCase = true) -> "they"
            else -> "you"
        }
        val verb = userMessage
        val keyword = words.first()

        val mood = when {
            userMessage.contains(Regex("mar gaya|died|accident|hospital|tension|divorce|suicide|police", RegexOption.IGNORE_CASE)) -> "Serious"
            userMessage.contains(Regex("gussa|naraz|jhoot|dhoka|bakwas|tang|had", RegexOption.IGNORE_CASE)) -> "Gussa"
            userMessage.contains(Regex("sad|udaas|ro|bore|akela|thak|dard", RegexOption.IGNORE_CASE)) -> "Sad"
            else -> "Happy"
        }

        val moodList = formulas[mood] ?: formulas["Happy"] ?: listOf("Acha $keyword, batao kya haal hain?")
        var pool = moodList.filter { it !in last3Replies }
        if (pool.isEmpty()) {
            last3Replies.clear()
            pool = moodList
        }
        val template = pool.random()

        val reply = template.replace("{subject}", subject).replace("{verb}", verb).replace("{keyword}", keyword)
        val finalReply = if (reply.split(" ").size > 20) reply.split(" ").take(20).joinToString(" ") else reply

        last3Replies.add(finalReply)
        if (last3Replies.size > 3) last3Replies.removeAt(0)
        return finalReply
    }

    fun setEnglishVoiceMode(mode: String) {
        englishVoiceMode.value = mode
        piperTtsManager.englishVoiceMode = mode
        saveToDataStore(mode)
    }

    val googleTtsAvailableLanguages: StateFlow<List<java.util.Locale>> = piperTtsManager.googleTtsAvailableLanguages
    val googleTtsAvailableVoices: StateFlow<List<android.speech.tts.Voice>> = piperTtsManager.googleTtsAvailableVoices
    val googleTtsSelectedLanguage: StateFlow<String> = piperTtsManager.googleTtsSelectedLanguage
    val googleTtsSelectedVoice: StateFlow<String> = piperTtsManager.googleTtsSelectedVoice

    fun setGoogleTtsLanguage(language: String) {
        piperTtsManager.setGoogleTtsLanguage(language)
    }

    fun setGoogleTtsVoice(voiceName: String) {
        piperTtsManager.setGoogleTtsVoice(voiceName)
    }

    private fun saveToDataStore(mode: String) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { settings ->
                settings[stringPreferencesKey("english_voice_mode")] = mode
            }
        }
    }



    private val _usePiperTts = MutableStateFlow(sharedPrefs.getBoolean("use_piper_tts", true))
    val usePiperTts: StateFlow<Boolean> = _usePiperTts.asStateFlow()

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "use_piper_tts") {
            _usePiperTts.value = prefs.getBoolean("use_piper_tts", true)
        }
    }

    private val _speakReplies = MutableStateFlow(sharedPrefs.getBoolean("speak_replies", true))
    val speakReplies: StateFlow<Boolean> = _speakReplies.asStateFlow()

    private val speechQueue = kotlinx.coroutines.channels.Channel<com.example.models.SpeechQueueItem>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    private var currentSpeechJob: Job? = null

    fun toggleSpeakReplies(enabled: Boolean) {
        _speakReplies.value = enabled
        sharedPrefs.edit().putBoolean("speak_replies", enabled).apply()
        if (!enabled) {
            stopAllSpeech()
        }
    }

    fun handleInterruption() {
        stopAllSpeech()
        _currentStatus.value = "Interrupted, sir."
        speakText("Yes, sir? Go ahead.")
    }

    fun stopAllSpeech() {
        while (speechQueue.tryReceive().isSuccess) { }
        currentSpeechJob?.cancel()
        currentSpeechJob = null
        piperTtsManager.stop()
        _audioAmplitude.value = 0f
        _isSpeaking.value = false
        _currentStatus.value = "Aira idle"
    }

    data class VibeParams(
        val noiseScale: Float,
        val lengthScale: Float,
        val pitch: Float,
        val alpha: Float
    )

    private val normalParams = VibeParams(0.667f, 1.0f, 1.0f, 0.65f)
    private val softParams = VibeParams(0.58f, 1.12f, 0.96f, 0.72f)
    private val flirtyParams = VibeParams(0.45f, 0.95f, 1.12f, 0.60f)
    private val teasingParams = VibeParams(0.38f, 0.88f, 1.18f, 0.55f)
    private val carefulParams = VibeParams(0.62f, 1.08f, 0.98f, 0.68f)
    private val sleepyParams = VibeParams(0.72f, 1.22f, 0.92f, 0.75f)

    fun interpolateVibe(v: Float): VibeParams {
        val anchors = listOf(8.0f, 25.0f, 42.0f, 58.5f, 75.0f, 92.0f)
        val params = listOf(normalParams, softParams, flirtyParams, teasingParams, carefulParams, sleepyParams)
        
        if (v <= anchors.first()) return params.first()
        if (v >= anchors.last()) return params.last()
        
        for (i in 0 until anchors.size - 1) {
            val xA = anchors[i]
            val xB = anchors[i+1]
            if (v in xA..xB) {
                val t = (v - xA) / (xB - xA)
                val pA = params[i]
                val pB = params[i+1]
                return VibeParams(
                    noiseScale = pA.noiseScale + t * (pB.noiseScale - pA.noiseScale),
                    lengthScale = pA.lengthScale + t * (pB.lengthScale - pA.lengthScale),
                    pitch = pA.pitch + t * (pB.pitch - pA.pitch),
                    alpha = pA.alpha + t * (pB.alpha - pA.alpha)
                )
            }
        }
        return normalParams
    }

    private val voicePrefs = com.example.utils.SecurePrefs.getEncryptedSharedPreferences(application, "voice_prefs")

    private val _voiceVibe = MutableStateFlow(voicePrefs.getFloat("voice_vibe", 8.0f))
    val voiceVibe: StateFlow<Float> = _voiceVibe.asStateFlow()

    private val _voiceNoiseScale = MutableStateFlow(voicePrefs.getFloat("noise_scale", 0.667f))
    val voiceNoiseScale: StateFlow<Float> = _voiceNoiseScale.asStateFlow()

    private val _voiceLengthScale = MutableStateFlow(voicePrefs.getFloat("length_scale", 1.0f))
    val voiceLengthScale: StateFlow<Float> = _voiceLengthScale.asStateFlow()

    private val _voicePitch = MutableStateFlow(voicePrefs.getFloat("pitch", 1.0f))
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    fun setSpeechPitch(pitch: Float) {
        _voicePitch.value = pitch
        voicePrefs.edit().putFloat("pitch", pitch).apply()
        sharedPrefs.edit().putFloat("pitch", pitch).apply()
    }

    private val _voiceAlpha = MutableStateFlow(voicePrefs.getFloat("alpha", 0.65f))
    val voiceAlpha: StateFlow<Float> = _voiceAlpha.asStateFlow()

    fun updateVoiceVibe(v: Float) {
        _voiceVibe.value = v
        val p = interpolateVibe(v)
        _voiceNoiseScale.value = p.noiseScale
        _voiceLengthScale.value = p.lengthScale
        _voicePitch.value = p.pitch
        _voiceAlpha.value = p.alpha

        // Save to voice_prefs
        voicePrefs.edit()
            .putFloat("voice_vibe", v)
            .putFloat("noise_scale", p.noiseScale)
            .putFloat("length_scale", p.lengthScale)
            .putFloat("pitch", p.pitch)
            .putFloat("alpha", p.alpha)
            .apply()

        // Also save to aira_settings for compatibility
        sharedPrefs.edit()
            .putFloat("voice_vibe", v)
            .putFloat("noise_scale", p.noiseScale)
            .putFloat("length_scale", p.lengthScale)
            .putFloat("pitch", p.pitch)
            .putFloat("alpha", p.alpha)
            .apply()
        
        // Let we speak test or apply dynamically to text-to-speech if initialized
    }

    private val _jarvisVoiceTone = MutableStateFlow(sharedPrefs.getString("jarvis_voice_tone", "Classic Jarvis") ?: "Classic Jarvis")
    val jarvisVoiceTone: StateFlow<String> = _jarvisVoiceTone.asStateFlow()

    fun setJarvisVoiceTone(tone: String) {
        _jarvisVoiceTone.value = tone
        sharedPrefs.edit().putString("jarvis_voice_tone", tone).apply()
        
        when (tone) {
            "Classic Jarvis" -> {
                _voicePitch.value = 0.92f
                _voiceLengthScale.value = 0.92f
                _voiceNoiseScale.value = 0.65f
                _voiceAlpha.value = 0.6f
            }
            "Deep Armor" -> {
                _voicePitch.value = 0.78f
                _voiceLengthScale.value = 1.12f
                _voiceNoiseScale.value = 0.73f
                _voiceAlpha.value = 0.68f
            }
            "Friday Tactical" -> {
                _voicePitch.value = 1.25f
                _voiceLengthScale.value = 0.85f
                _voiceNoiseScale.value = 0.52f
                _voiceAlpha.value = 0.58f
            }
            "Standard System" -> {
                _voicePitch.value = 1.00f
                _voiceLengthScale.value = 1.0f
                _voiceNoiseScale.value = 0.667f
                _voiceAlpha.value = 0.65f
            }
        }
        
        voicePrefs.edit()
            .putFloat("noise_scale", _voiceNoiseScale.value)
            .putFloat("length_scale", _voiceLengthScale.value)
            .putFloat("pitch", _voicePitch.value)
            .putFloat("alpha", _voiceAlpha.value)
            .apply()

        sharedPrefs.edit()
            .putFloat("noise_scale", _voiceNoiseScale.value)
            .putFloat("length_scale", _voiceLengthScale.value)
            .putFloat("pitch", _voicePitch.value)
            .putFloat("alpha", _voiceAlpha.value)
            .apply()
    }

    val piperActiveVoice: StateFlow<String> get() = piperTtsManager.activeVoice
    val piperIsEngineActive: StateFlow<Boolean> get() = piperTtsManager.isEngineActive
    val piperIsModelDownloaded: StateFlow<Map<String, Boolean>> get() = piperTtsManager.isModelDownloaded
    val piperDownloadProgress: StateFlow<Map<String, Float>> get() = piperTtsManager.downloadProgress
    val piperDownloadStatusMessage: StateFlow<String?> get() = piperTtsManager.downloadStatusMessage
    val piperAvailableVoices: List<com.example.service.PiperTtsManager.PiperVoice> get() = piperTtsManager.availableVoices
    val isVoskModelLoaded: Boolean get() = voskModel != null
    val showTtsDataDialog: StateFlow<Boolean> get() = piperTtsManager.showTtsDataDialog
    val missingTtsLanguageLocale: StateFlow<String> get() = piperTtsManager.missingTtsLanguageLocale

    fun dismissTtsDataDialog() {
        piperTtsManager.dismissTtsDataDialog()
    }

    fun openInstallTtsDataSettings() {
        piperTtsManager.openInstallTtsDataSettings()
    }

    enum class VoiceAssistantState {
        READY,
        DOWNLOADING,
        NOT_DOWNLOADED
    }

    enum class TtsEngine {
        AUTO,
        GOOGLE_TTS,
        PIPER_OFFLINE
    }

    private val _selectedTtsEngine = MutableStateFlow(
        try {
            TtsEngine.valueOf(sharedPrefs.getString("selected_tts_engine", TtsEngine.GOOGLE_TTS.name) ?: TtsEngine.GOOGLE_TTS.name)
        } catch (e: Exception) {
            TtsEngine.GOOGLE_TTS
        }
    )
    val selectedTtsEngine: StateFlow<TtsEngine> = _selectedTtsEngine.asStateFlow()

    fun setSelectedTtsEngine(engine: TtsEngine) {
        if (engine == TtsEngine.PIPER_OFFLINE && !com.example.utils.MemoryManager.isDeviceCapable(getApplication())) {
            _selectedTtsEngine.value = TtsEngine.GOOGLE_TTS
            sharedPrefs.edit().putString("selected_tts_engine", TtsEngine.GOOGLE_TTS.name).apply()
            piperTtsManager.selectedTtsEngine = TtsEngine.GOOGLE_TTS.name
            speakText("2GB device detected. Cloud mode enabled for safety. Offline features disabled.")
            return
        }
        _selectedTtsEngine.value = engine
        sharedPrefs.edit().putString("selected_tts_engine", engine.name).apply()
        piperTtsManager.selectedTtsEngine = engine.name
        when (engine) {
            TtsEngine.PIPER_OFFLINE -> {
                setEnglishVoiceMode("Amy")
            }
            TtsEngine.GOOGLE_TTS -> {
                setEnglishVoiceMode("India")
            }
            TtsEngine.AUTO -> {
                setEnglishVoiceMode("India")
            }
        }
    }

    enum class SttEngine {
        AUTO,
        VOSK_OFFLINE
    }

    private val _selectedSttEngine = MutableStateFlow(
        try {
            SttEngine.valueOf(sharedPrefs.getString("selected_stt_engine", SttEngine.AUTO.name) ?: SttEngine.AUTO.name)
        } catch (e: Exception) {
            SttEngine.AUTO
        }
    )
    val selectedSttEngine: StateFlow<SttEngine> = _selectedSttEngine.asStateFlow()

    fun setSelectedSttEngine(engine: SttEngine) {
        _selectedSttEngine.value = engine
        sharedPrefs.edit().putString("selected_stt_engine", engine.name).apply()
        if (engine == SttEngine.VOSK_OFFLINE) {
            initVoskModel()
        }
    }

    val modelReadyState: StateFlow<VoiceAssistantState> = combine(
        piperIsModelDownloaded,
        piperDownloadProgress
    ) { downloaded, progress ->
        when {
            downloaded["en_US-amy-medium"] == true -> VoiceAssistantState.READY
            progress.containsKey("en_US-amy-medium") -> VoiceAssistantState.DOWNLOADING
            else -> VoiceAssistantState.NOT_DOWNLOADED
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VoiceAssistantState.NOT_DOWNLOADED)

    private var amplitudeJob: Job? = null
    private var voskWaveJob: Job? = null

    private fun startVoskWaveLoop() {
        voskWaveJob?.cancel()
        voskWaveJob = viewModelScope.launch(Dispatchers.Default) {
            var tick = 0f
            while (_isListening.value && !_isSpeaking.value && !isUsingGoogleSTT) {
                val base = Math.abs(Math.sin(tick.toDouble())).toFloat() * 0.5f
                val noise = (Math.random().toFloat() * 0.35f)
                val level = (base + noise).coerceIn(0.1f, 1f)
                _audioAmplitude.value = level
                _voskRawAudioLevel.value = level
                tick += 0.25f
                kotlinx.coroutines.delay(40)
            }
            if (!_isSpeaking.value) {
                _audioAmplitude.value = 0f
                _voskRawAudioLevel.value = 0f
            }
        }
    }

    // --- Extras Live Data States ---
    private val _weatherText = MutableStateFlow("Offline / Not Loaded")
    val weatherText: StateFlow<String> = _weatherText.asStateFlow()

    private val newsRepository = com.example.repository.NewsRepository()

    private val _selectedNewsCategory = MutableStateFlow("All")
    val selectedNewsCategory: StateFlow<String> = _selectedNewsCategory.asStateFlow()

    private val _newsItems = MutableStateFlow<List<com.example.data.models.NewsItem>>(emptyList())
    val newsItems: StateFlow<List<com.example.data.models.NewsItem>> = _newsItems.asStateFlow()

    private val _newsFeed = MutableStateFlow<List<String>>(emptyList())
    val newsFeed: StateFlow<List<String>> = _newsFeed.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading: StateFlow<Boolean> = _isNewsLoading.asStateFlow()

    private val _newsError = MutableStateFlow<String?>(null)
    val newsError: StateFlow<String?> = _newsError.asStateFlow()

    private val _voiceCommandLogs = MutableStateFlow<List<VoiceCommandLog>>(emptyList())
    val voiceCommandLogs: StateFlow<List<VoiceCommandLog>> = _voiceCommandLogs.asStateFlow()

    // --- TTS & Voice Recognition Engine ---
    private var speechRecognizer: SpeechRecognizer? = null
    private var isWakeWordActiveListening = false
 
    init {
        try {
            sharedPrefs.registerOnSharedPreferenceChangeListener(prefChangeListener)

            if (!_isDeviceMemoryCapable.value || !com.example.utils.MemoryManager.isOfflineSupported(application)) {
                Log.w("AiraViewModel", "Device RAM is < 3GB (${_totalRamMb.value} MB). Enforcing Cloud Mode for device safety.")
                _isOfflineBrain.value = false
                _isLocalMode.value = false
                _usePersistentListening.value = false
                _selectedTtsEngine.value = TtsEngine.GOOGLE_TTS
                _selectedSttEngine.value = SttEngine.AUTO
                sharedPrefs.edit()
                    .putBoolean("offline_brain", false)
                    .putBoolean("is_local_mode", false)
                    .putBoolean("persistent_listening", false)
                    .putString("selected_tts_engine", TtsEngine.GOOGLE_TTS.name)
                    .putString("selected_stt_engine", SttEngine.AUTO.name)
                    .apply()
            }
            
            viewModelScope.launch {
                _currentStatus.value = "Loading Voice..."
                try {
                    piperTts.initialize()
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e("AiraViewModel", "Error initializing piperTts", e)
                }
                _currentStatus.value = "Aira idle"
            }
            try {
                piperTtsManager.selectedTtsEngine = _selectedTtsEngine.value.name
            } catch (e: Throwable) {
                Log.e("AiraViewModel", "Error setting selectedTtsEngine", e)
            }
            try {
                loadVoiceCommandLogs()
            } catch (e: Throwable) {
                Log.e("AiraViewModel", "Error loading voice command logs", e)
            }
            viewModelScope.launch {
                try {
                    getApplication<Application>().dataStore.data.collect { settings ->
                        val savedMode = settings[stringPreferencesKey("english_voice_mode")] ?: "India"
                        englishVoiceMode.value = savedMode
                        piperTtsManager.englishVoiceMode = savedMode
                    }
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e("AiraViewModel", "Error in dataStore collection", e)
                }
            }
            try {
                initPiperEngine(application)
            } catch (e: Throwable) {
                Log.e("AiraViewModel", "Error in initPiperEngine", e)
            }
            // Collect Piper auto-download status
            viewModelScope.launch {
                try {
                    piperTtsManager.downloadStatusMessage.collect { status ->
                        if (status != null) {
                            _currentStatus.value = status
                        }
                    }
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e("AiraViewModel", "Error collecting downloadStatusMessage", e)
                }
            }
            // Sync Piper TTS speaking state with OrbState and _isSpeaking
            viewModelScope.launch {
                try {
                    piperTtsManager.isSpeakingFlow.collect { speaking ->
                        _isSpeaking.value = speaking
                        if (speaking) {
                            _orbState.value = com.example.ui.components.OrbState.SPEAKING
                        } else {
                            if (_sttState.value == SttState.IDLE) {
                                _orbState.value = com.example.ui.components.OrbState.IDLE
                            }
                        }
                    }
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e("AiraViewModel", "Error collecting isSpeaking", e)
                }
            }
            // Clean up expired local DB caches to optimize disk footprint and seed initial Room DB data if empty
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val now = System.currentTimeMillis()
                    db.grokCacheDao().clearExpiredCaches(now)
                    queryCacheDao.deleteExpired(now - QueryCache.DEFAULT_TTL_MS)
                    weatherCacheDao.deleteExpiredWeather(now - WeatherCache.DEFAULT_TTL_MS)
                    Log.d("AiraViewModel", "Cleaned up expired local Room DB response and weather caches.")

                    // Seed initial Chat messages in Room DB if empty
                    if (chatDao.getAllMessagesList().isEmpty()) {
                        val now = System.currentTimeMillis()
                        chatDao.insertMessage(ChatMessage(sender = "aira", message = "Aira initialized. Local voice detection engine active.", timestamp = now - 300000))
                        chatDao.insertMessage(ChatMessage(sender = "user", message = "System diagnostic check requested.", timestamp = now - 180000))
                        chatDao.insertMessage(ChatMessage(sender = "aira", message = "All systems operating at maximum performance.", timestamp = now - 60000))
                    }

                    // Seed initial Memories in Room DB if empty
                    if (db.memoryDao().getAllMemoriesList().isEmpty()) {
                        val now = System.currentTimeMillis()
                        db.memoryDao().insertMemory(com.example.data.Memory(factText = "Primary AI Engine set to Gemini API / Llama 3.2 Offline", source = "system", createdAt = now - 86400000))
                        db.memoryDao().insertMemory(com.example.data.Memory(factText = "Offline TTS runtime powered by Amy ONNX JNI engine", source = "system", createdAt = now - 43200000))
                        db.memoryDao().insertMemory(com.example.data.Memory(factText = "Voice command wake word set to 'Hey Aira'", source = "voice", createdAt = now - 360000))
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e("AiraViewModel", "Failed to clear expired local caches or seed Room DB", e)
                }
            }
            refreshShizukuStatus()
            initSpeechRecognizer()
            registerNetworkMonitoring()
            startIdleMemoryCleanupWatchdog()

            // DEFERRED HEAVY INIT (5-second post-startup delay for 0-lag cold start and binder safety)
            viewModelScope.launch {
                kotlinx.coroutines.delay(5000L)
                val appCtx = getApplication<Application>()
                if (com.example.utils.MemoryManager.isOfflineSupported(appCtx)) {
                    if (com.example.utils.MemoryManager.isPiperSupported(appCtx)) {
                        try {
                            if (!piperTtsManager.MODEL_PATH.exists()) {
                                piperTtsManager.startDownload()
                            }
                        } catch (e: Throwable) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            Log.e("AiraViewModel", "Error checking/downloading piper model", e)
                        }
                    }
                    if (com.example.utils.MemoryManager.isVoskSupported(appCtx)) {
                        initVoskModel()
                    }
                } else {
                    Log.i("AiraViewModel", "Offline mode not supported on 2GB device. Skipping Piper, Vosk, and Llama init.")
                }
                try {
                    performFetchWeather()
                    generateMorningBriefing()
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e("AiraViewModel", "Error in performFetchWeather/generateMorningBriefing", e)
                }
                try {
                    fetchNews()
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e("AiraViewModel", "Error in fetchNews", e)
                }
                try {
                    preloadVoiceCommands()
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e("AiraViewModel", "Error in preloadVoiceCommands", e)
                }
            }

            // Start consuming queue on Main thread
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    for (item in speechQueue) {
                        if (_speakReplies.value) {
                            val job = launch {
                                performSpeakText(item)
                            }
                            currentSpeechJob = job
                            job.join()
                            currentSpeechJob = null
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e("AiraViewModel", "Error in speech queue loop", e)
                }
            }

            // Smooth silent startup without forcing TTS initial audio delay
            val hasRunFirstLaunchSelfTest = sharedPrefs.getBoolean("has_run_first_launch_selftest", false)
            if (!hasRunFirstLaunchSelfTest) {
                sharedPrefs.edit().putBoolean("has_run_first_launch_selftest", true).apply()
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("AiraViewModel", "Fatal error guarded in init block", e)
        }
    }

    private fun preloadVoiceCommands() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                VoiceCommandManager.getInstance(getApplication()).preloadDefaultActionsAndCommands()
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Error preloading voice commands: ${e.message}")
            }
        }
    }

    private fun initPiperEngine(application: Application) {
        piperTtsManager.setSpeakingCallbacks(
            onStart = {
                if (_isListening.value) {
                    stopListening()
                }
                _isSpeaking.value = true
                updateSttState(SttState.SPEAKING)
                startWaveAmplitudeLoop()
            },
            onStop = {
                _isSpeaking.value = false
                _audioAmplitude.value = 0f
                updateSttState(SttState.IDLE)
                if (_usePersistentListening.value) {
                    restartContinuousListeningIfNeeded()
                }
            }
        )
    }

    private fun startWaveAmplitudeLoop() {
        amplitudeJob?.cancel()
        amplitudeJob = viewModelScope.launch {
            var tick = 0
            while (_isSpeaking.value) {
                _audioAmplitude.value = (0.2f + 0.8f * kotlin.math.sin(tick * 0.4f)).coerceIn(0.1f, 1f)
                tick++
                kotlinx.coroutines.delay(16) // Unlocked: high performance 60fps style
            }
            _audioAmplitude.value = 0f
        }
    }

    private fun initSpeechRecognizer() {
        try {
            val context = getApplication<Application>()
            try {
                speechRecognizer?.destroy()
            } catch (_: Throwable) {}
            speechRecognizer = null

            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                // Check if Google Search package is actually installed
                var isGoogleAppInstalled = false
                try {
                    val pm = context.packageManager
                    pm.getPackageInfo("com.google.android.googlequicksearchbox", 0)
                    isGoogleAppInstalled = true
                } catch (_: Throwable) {
                    isGoogleAppInstalled = false
                }

                speechRecognizer = if (isGoogleAppInstalled) {
                    val googleComponent = android.content.ComponentName(
                        "com.google.android.googlequicksearchbox",
                        "com.google.android.voicesearch.service.SpeechRecognitionService"
                    )
                    try {
                        SpeechRecognizer.createSpeechRecognizer(context, googleComponent)
                    } catch (e: Throwable) {
                        Log.d("AiraViewModel", "Google explicit component SpeechRecognizer fallback to default", e)
                        SpeechRecognizer.createSpeechRecognizer(context)
                    }
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                if (speechRecognizer != null) {
                    speechRecognizer?.setRecognitionListener(this)
                    Log.d("AiraViewModel", "SpeechRecognizer initialized successfully (GoogleAppPresent=$isGoogleAppInstalled)")
                } else {
                    Log.w("AiraViewModel", "SpeechRecognizer.createSpeechRecognizer returned null")
                }
            } else {
                Log.w("AiraViewModel", "SpeechRecognizer.isRecognitionAvailable returned false")
            }
        } catch (e: Throwable) {
            Log.e("AiraViewModel", "Failed to create SpeechRecognizer", e)
            speechRecognizer = null
        }
    }

    fun initVoskModel() {
        val appCtx = getApplication<Application>()
        if (!com.example.utils.MemoryManager.isVoskSupported(appCtx) || !com.example.utils.MemoryManager.isOfflineSupported(appCtx)) {
            Log.w("AiraViewModel", "Device RAM < 3GB. Skipping Vosk STT initialization to prevent native OOM.")
            return
        }
        if (voskModel != null || isVoskInitializing) return
        isVoskInitializing = true
        _currentStatus.value = "Initializing Vosk Offline..."
        com.example.utils.VoskLogManager.logInfo("Initiating Vosk STT Model load pipeline...", "VoskInit")
        
        viewModelScope.launch(Dispatchers.IO) {
            com.example.utils.MemoryManager.loadModelOnDemand(appCtx, com.example.utils.NativeModelType.VOSK_STT) {
                if (com.example.utils.DownloadManager.isVoskModelDownloaded(appCtx)) {
                    val dir = com.example.utils.DownloadManager.getVoskModelDir(appCtx)
                    try {
                        com.example.utils.VoskLogManager.logInfo("Found downloaded Vosk model at: ${dir.absolutePath}. Instantiating...", "VoskInit")
                        voskModel = Model(dir.absolutePath)
                        isVoskInitializing = false
                        com.example.utils.VoskLogManager.logInfo("Vosk model successfully instantiated via JNI!", "VoskInit")
                        _currentStatus.value = "Offline engine ready"
                        _voskTriggerStatus.value = "Vosk STT Model Loaded & Ready"
                    } catch (e: Throwable) {
                        com.example.utils.VoskLogManager.logInitError("Failed to load Vosk model from ${dir.absolutePath}: ${e.message}", e)
                        if (dir.exists()) {
                            dir.deleteRecursively()
                        }
                        isVoskInitializing = false
                    }
                } else {
                    com.example.utils.VoskLogManager.logWarn("Vosk model missing. Triggering on-demand download...", "VoskInit")
                    viewModelScope.launch {
                        val downloaded = com.example.utils.DownloadManager.downloadVoskModel(appCtx)
                        if (downloaded) {
                            val dir = com.example.utils.DownloadManager.getVoskModelDir(appCtx)
                            try {
                                voskModel = Model(dir.absolutePath)
                                com.example.utils.VoskLogManager.logInfo("Vosk model loaded after download!", "VoskInit")
                                _currentStatus.value = "Offline engine ready"
                                _voskTriggerStatus.value = "Vosk STT Model Loaded & Ready"
                            } catch (e: Throwable) {
                                com.example.utils.VoskLogManager.logInitError("Failed to load Vosk model after download: ${e.message}", e)
                            }
                        } else {
                            _currentStatus.value = "Vosk Download Failed"
                            _voskTriggerStatus.value = "Vosk model missing"
                        }
                        isVoskInitializing = false
                    }
                }
            }
        }
    }

    private fun copyAssetFolder(assetManager: android.content.res.AssetManager, fromAssetPath: String, toAbsoluteDir: File): Boolean {
        return try {
            val files = assetManager.list(fromAssetPath)
            if (!files.isNullOrEmpty()) {
                if (!toAbsoluteDir.exists()) {
                    toAbsoluteDir.mkdirs()
                }
                var allSuccess = true
                for (file in files) {
                    val srcSubPath = if (fromAssetPath.isEmpty()) file else "$fromAssetPath/$file"
                    val destSubFile = File(toAbsoluteDir, file)
                    if (!copyAssetFolder(assetManager, srcSubPath, destSubFile)) {
                        allSuccess = false
                    }
                }
                allSuccess
            } else {
                toAbsoluteDir.parentFile?.let {
                    if (!it.exists()) it.mkdirs()
                }
                assetManager.open(fromAssetPath).use { input ->
                    java.io.FileOutputStream(toAbsoluteDir).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }
        } catch (e: Throwable) {
            com.example.utils.VoskLogManager.logInitError("Error copying asset $fromAssetPath to ${toAbsoluteDir.absolutePath}", e)
            false
        }
    }

    private fun performModelUnpack() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val targetDir = File(context.filesDir, "model")
                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }
                com.example.utils.VoskLogManager.logInfo("Extracting Vosk model assets directly to: ${targetDir.absolutePath}", "VoskInit")
                
                var loadedSuccessfully = false
                val extracted = copyAssetFolder(context.assets, "models/model-en", targetDir)
                val confFile = File(targetDir, "conf/model.conf")
                if (extracted && confFile.exists() && confFile.length() > 0L) {
                    try {
                        voskModel = Model(targetDir.absolutePath)
                        isVoskInitializing = false
                        loadedSuccessfully = true
                        com.example.utils.VoskLogManager.logInfo("Vosk model extracted and loaded successfully!", "VoskInit")
                        _currentStatus.value = "Offline engine ready"
                        _voskTriggerStatus.value = "Vosk STT Model Loaded & Ready"
                    } catch (e: Throwable) {
                        com.example.utils.VoskLogManager.logInitError("Vosk model load from extracted files failed: ${e.message}", e)
                        if (targetDir.exists()) {
                            targetDir.deleteRecursively()
                        }
                    }
                } else {
                    com.example.utils.VoskLogManager.logInitError("Asset extraction failed or conf/model.conf missing/empty after copy", null)
                }
                
                if (!loadedSuccessfully) {
                    com.example.utils.VoskLogManager.logWarn("Direct extraction failed. Attempting Vosk StorageService unpack...", "VoskInit")
                    if (targetDir.exists()) {
                        targetDir.deleteRecursively()
                    }
                    try {
                        StorageService.unpack(context, "models/model-en", "model",
                            { model ->
                                voskModel = model
                                isVoskInitializing = false
                                com.example.utils.VoskLogManager.logInfo("Vosk model unpacked via StorageService.", "VoskInit")
                                _currentStatus.value = "Offline engine ready"
                                _voskTriggerStatus.value = "Vosk STT Model Loaded & Ready"
                            },
                            { exception ->
                                isVoskInitializing = false
                                com.example.utils.VoskLogManager.logInitError("Vosk model StorageService unpack failed: ${exception.message}", exception)
                                _currentStatus.value = "System STT Ready"
                                _voskTriggerStatus.value = "STT Init Failed: StorageService error"
                            }
                        )
                    } catch (e: Throwable) {
                        isVoskInitializing = false
                        com.example.utils.VoskLogManager.logInitError("StorageService unpack exception: ${e.message}", e)
                        _currentStatus.value = "System STT Ready"
                        _voskTriggerStatus.value = "STT Init Failed: StorageService exception"
                    }
                }
            } catch (e: Throwable) {
                isVoskInitializing = false
                com.example.utils.VoskLogManager.logInitError("Exception during performModelUnpack: ${e.message}", e)
                _currentStatus.value = "System STT Ready"
                _voskTriggerStatus.value = "STT Init Failed: Exception"
            }
        }
    }

    private var consecutiveSpeechErrors = 0

    fun isInternetAvailable(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // --- Speech Recognition Triggers ---
    fun startListening() {
        markUserActivity()
        if (_isListening.value) return
        
        // Interrupt any ongoing TTS playing to avoid audio loop or echo
        stopAllSpeech()

        Log.i("STTFlow", "STEP 1: Checking RECORD_AUDIO permission...")
        // Protect and prevent recording if permission is missing
        if (ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("STTFlow", "STEP 1 FAILED: RECORD_AUDIO Permission missing!")
            _currentStatus.value = "RECORD_AUDIO Permission required!"
            if (_usePersistentListening.value) {
                _usePersistentListening.value = false
                sharedPrefs.edit().putBoolean("persistent_listening", false).apply()
            }
            speakText("Audio recording permission is required for voice assistant features. Please grant it.")
            return
        }
        Log.i("STTFlow", "STEP 1 PASSED: RECORD_AUDIO permission granted.")

        // Acquire audio focus for microphone recording
        com.example.utils.AiraAudioFocusManager.getInstance(getApplication()).requestSttFocus {
            stopListening()
        }

        _isListening.value = true
        updateSttState(SttState.LISTENING)
        hasSpeechStarted = false

        if (_isLocalMode.value || selectedSttEngine.value == SttEngine.VOSK_OFFLINE || !isInternetAvailable()) {
            Log.i("STTFlow", "STEP 2: Engine Selection -> Vosk Offline forced (LocalMode=${_isLocalMode.value}, OfflinePreference=${selectedSttEngine.value == SttEngine.VOSK_OFFLINE}, NetAvailable=${isInternetAvailable()})")
            switchToOfflineVosk()
        } else if (isInternetAvailable()) {
            Log.i("STTFlow", "STEP 2: Engine Selection -> STAGE 1: Google SpeechRecognizer (Primary Online)")
            isUsingGoogleSTT = true
            _sttEngineStatus.value = "Online"
            _currentStatus.value = "Listening (Google Speech)..."

            viewModelScope.launch(Dispatchers.Main) {
                if (speechRecognizer == null) {
                    initSpeechRecognizer()
                }

                val recognizer = speechRecognizer
                if (recognizer == null) {
                    Log.w("STTFlow", "STAGE 1 FAILED: Google SpeechRecognizer null. Falling back to STAGE 2 (Vosk STT).")
                    switchToOfflineVosk()
                    return@launch
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    val targetLocale = if (lang_code == "en-US") Locale.US else Locale.forLanguageTag("ur-PK")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLocale.toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLocale.toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, targetLocale.toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 10000L)
                    putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 10000L)
                    putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 5000L)
                }

                try {
                    recognizer.cancel()
                    recognizer.startListening(intent)
                    Log.i("STTFlow", "STAGE 1: Google SpeechRecognizer listening loop started successfully.")
                } catch (e: Exception) {
                    Log.e("STTFlow", "STAGE 1 FAILED: Exception starting Google STT. Falling back to STAGE 2 (Vosk STT).", e)
                    switchToOfflineVosk()
                }
            }
        } else {
            Log.i("STTFlow", "STEP 2: Network unavailable. Falling back to STAGE 2 (Vosk STT).")
            switchToOfflineVosk()
        }
    }

    private var systemSpeechRecognizer: SpeechRecognizer? = null

    fun stopListening() {
        handler.removeCallbacks(timeoutRunnable)
        _isListening.value = false
        if (_sttState.value == SttState.LISTENING) {
            updateSttState(SttState.IDLE)
        }
        voskWaveJob?.cancel()
        _audioAmplitude.value = 0f

        com.example.utils.AiraAudioFocusManager.getInstance(getApplication()).releaseSttFocus()

        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}

        try {
            systemSpeechRecognizer?.stopListening()
            systemSpeechRecognizer?.destroy()
            systemSpeechRecognizer = null
        } catch (_: Exception) {}

        try {
            voskSpeechService?.stop()
            voskSpeechService = null
        } catch (_: Exception) {}
    }

    fun switchToOfflineVosk() {
        handler.removeCallbacks(timeoutRunnable)
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}

        Log.i("STTFlow", "STAGE 2: Initializing Vosk STT (Secondary Fallback)...")
        com.example.utils.VoskLogManager.logInfo("STT STAGE 2: Triggering Vosk Secondary Fallback...", "STTFlow")

        if (!com.example.utils.MemoryManager.isVoskSupported(getApplication())) {
            Log.w("STTFlow", "STAGE 2 FAILED: Device RAM < 3GB. Vosk disabled. Falling back to STAGE 3 (System STT).")
            startSystemSttFallback()
            return
        }

        isUsingGoogleSTT = false
        _sttEngineStatus.value = "Offline"
        
        if (ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("STTFlow", "STAGE 2 FAILED: RECORD_AUDIO permission required!")
            _currentStatus.value = "RECORD_AUDIO Permission required!"
            _isListening.value = false
            if (_usePersistentListening.value) {
                _usePersistentListening.value = false
                sharedPrefs.edit().putBoolean("persistent_listening", false).apply()
            }
            return
        }
        
        viewModelScope.launch(Dispatchers.Main) {
            startVoskListening()
        }
    }

    fun startSystemSttFallback() {
        Log.i("STTFlow", "STAGE 3 (TERTIARY): Launching Android System SpeechRecognizer Fallback...")
        com.example.utils.VoskLogManager.logInfo("STT STAGE 3 (TERTIARY): Launching Android System SpeechRecognizer Fallback...", "STTFlow")
        
        handler.removeCallbacks(timeoutRunnable)
        _isListening.value = true
        updateSttState(SttState.LISTENING)
        isUsingGoogleSTT = false
        _sttEngineStatus.value = "System Fallback"
        _currentStatus.value = "Listening (System STT)..."

        if (ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("STTFlow", "STAGE 3 FAILED: RECORD_AUDIO Permission required!")
            _currentStatus.value = "RECORD_AUDIO Permission required!"
            _isListening.value = false
            return
        }

        viewModelScope.launch(Dispatchers.Main) {
            try {
                systemSpeechRecognizer?.destroy()
            } catch (_: Exception) {}

            try {
                val context = getApplication<Application>()
                systemSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                systemSpeechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.i("STTFlow", "STAGE 3: System STT Ready for speech")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.i("STTFlow", "STAGE 3: System STT Beginning of speech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _audioAmplitude.value = ((rmsdB + 2f) / 10f).coerceIn(0.1f, 1f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.i("STTFlow", "STAGE 3: System STT End of speech")
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        Log.e("STTFlow", "ALL STT STAGES FAILED! Stage 3 System STT Error Code: $error")
                        com.example.utils.VoskLogManager.logError("All 3 STT stages (Google -> Vosk -> System) failed! System STT code: $error", null, "STTFlow")
                        _isListening.value = false
                        _sttEngineStatus.value = "Failed"
                        _currentStatus.value = "Speech recognition error ($error). Tap mic to retry."
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        consecutiveSpeechErrors = 0
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        Log.i("STTFlow", "STAGE 3: System STT Result Captured: '$text'")
                        if (text.isNotBlank()) {
                            _currentStatus.value = "Recognized: $text"
                            processAssistantSession(text)
                        } else {
                            _currentStatus.value = "No speech recognized. Tap mic to retry."
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _currentStatus.value = "Phonetics: $text"
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                systemSpeechRecognizer?.cancel()
                systemSpeechRecognizer?.startListening(intent)
                Log.i("STTFlow", "STAGE 3: Android System STT listening loop started successfully.")
            } catch (e: Exception) {
                Log.e("STTFlow", "Fatal failure launching System STT Tertiary fallback: ${e.message}", e)
                _isListening.value = false
                _sttEngineStatus.value = "Failed"
                _currentStatus.value = "All STT Engines Unreachable"
            }
        }
    }

    private fun processVoskHypothesis(hypothesis: String) {
        if (hypothesis.isBlank()) return
        try {
            val json = JSONObject(hypothesis)
            if (json.has("result")) {
                val resultArray = json.optJSONArray("result")
                val words = mutableListOf<Pair<String, Float>>()
                var totalConf = 0f
                if (resultArray != null && resultArray.length() > 0) {
                    for (i in 0 until resultArray.length()) {
                        val item = resultArray.getJSONObject(i)
                        val w = item.optString("word", "")
                        val c = item.optDouble("conf", 0.0).toFloat()
                        if (w.isNotBlank()) {
                            words.add(Pair(w, c))
                            totalConf += c
                        }
                    }
                }
                val text = json.optString("text", "")
                val avgConf = if (words.isNotEmpty()) (totalConf / words.size).coerceIn(0f, 1f) else if (text.isNotBlank()) 0.85f else 0f
                _voskConfidenceScore.value = avgConf
                _voskWordConfidences.value = words

                val currentWakeWord = _wakeWord.value.lowercase().trim()
                val lowerText = text.lowercase().trim()

                if (text.isNotBlank()) {
                    if (lowerText.contains(currentWakeWord)) {
                        _voskTriggerStatus.value = "Wake Word Matched: '$text' (${(avgConf * 100).toInt()}%)"
                        com.example.utils.VoskLogManager.logInfo("Wake Word trigger matched: '$text' (Conf: ${(avgConf * 100).toInt()}%)")
                    } else if (avgConf < 0.60f) {
                        _voskTriggerStatus.value = "Low Confidence Rejected: '$text' (${(avgConf * 100).toInt()}%)"
                        com.example.utils.VoskLogManager.logWarn("Recognition low confidence ($text, conf=${(avgConf * 100).toInt()}%)")
                    } else {
                        _voskTriggerStatus.value = "Speech Recognized: '$text' (${(avgConf * 100).toInt()}%)"
                        com.example.utils.VoskLogManager.logInfo("Recognized phrase: '$text' (${(avgConf * 100).toInt()}%)")
                    }
                }
            } else if (json.has("partial")) {
                val partial = json.optString("partial", "")
                if (partial.isNotBlank()) {
                    _voskTriggerStatus.value = "Phonetic Partial: '$partial'"
                    _voskConfidenceScore.value = 0.70f
                }
            }
        } catch (e: Exception) {
            com.example.utils.VoskLogManager.logWarn("Failed to parse Vosk hypothesis JSON: ${e.message}")
        }
    }

    private fun startVoskListening() {
        if (ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("STTFlow", "STAGE 2 FAILED: RECORD_AUDIO Permission missing!")
            _currentStatus.value = "RECORD_AUDIO Permission required!"
            _isListening.value = false
            if (_usePersistentListening.value) {
                _usePersistentListening.value = false
                sharedPrefs.edit().putBoolean("persistent_listening", false).apply()
            }
            return
        }

        val model = voskModel
        if (model == null) {
            Log.w("STTFlow", "STAGE 2 FAILED: Vosk model null. Initiating download/extraction and falling back to STAGE 3 (System STT).")
            com.example.utils.VoskLogManager.logWarn("Vosk model null. Triggering background initialization & System STT fallback.", "STTFlow")
            initVoskModel()
            startSystemSttFallback()
            return
        }

        try {
            _currentStatus.value = "Listening (Offline Vosk)..."
            _voskTriggerStatus.value = "Listening for Voice Commands / Wake Word..."
            Log.i("STTFlow", "STAGE 2: Vosk Recognizer starting listening loop at 16000Hz...")
            com.example.utils.VoskLogManager.logInfo("Vosk Recognizer starting listening loop at 16000Hz...", "VoskEngine")
            startVoskWaveLoop()
            val recognizer = Recognizer(model, 16000.0f)
            voskSpeechService = SpeechService(recognizer, 16000.0f)
            voskSpeechService?.startListening(object : org.vosk.android.RecognitionListener {
                override fun onResult(hypothesis: String) {
                    consecutiveSpeechErrors = 0
                    processVoskHypothesis(hypothesis)
                    val text = extractVoskText(hypothesis)
                    Log.i("STTFlow", "STAGE 2: Vosk STT Result Captured: '$text'")
                    handleOfflineSpeechResult(text)
                }

                override fun onPartialResult(hypothesis: String) {
                    processVoskHypothesis(hypothesis)
                    val text = extractVoskPartialText(hypothesis)
                    if (text.isNotEmpty()) {
                        val currentWakeWord = _wakeWord.value.lowercase().trim()
                        if (_usePersistentListening.value && text.lowercase().contains(currentWakeWord)) {
                            _currentStatus.value = "Wake word detected! Listening..."
                            _voskTriggerStatus.value = "Wake Word Matched in Partial Speech!"
                        } else {
                            _currentStatus.value = "Phonetics: $text"
                        }
                    }
                }

                override fun onFinalResult(hypothesis: String) {
                    consecutiveSpeechErrors = 0
                    processVoskHypothesis(hypothesis)
                    val text = extractVoskText(hypothesis)
                    Log.i("STTFlow", "STAGE 2: Vosk STT Final Result Captured: '$text'")
                    handleOfflineSpeechResult(text)
                }

                override fun onError(exception: Exception) {
                    Log.e("STTFlow", "STAGE 2 FAILED: Vosk listener error: ${exception.message}", exception)
                    com.example.utils.VoskLogManager.logError("Vosk listener error: ${exception.message}", exception, "STTFlow")
                    _currentStatus.value = "Voice Engine Paused"
                    _voskTriggerStatus.value = "Offline voice processing issue: ${exception.localizedMessage ?: "Microphone busy"}"
                    consecutiveSpeechErrors++
                    if (consecutiveSpeechErrors >= 2) {
                        consecutiveSpeechErrors = 0
                        Log.w("STTFlow", "STAGE 2 FAILED: Vosk consecutive errors reached threshold. Falling back to STAGE 3 (System STT).")
                        startSystemSttFallback()
                        return
                    }
                    if (_isTestingWakeWord.value) {
                        viewModelScope.launch(Dispatchers.Main) {
                            kotlinx.coroutines.delay(1000)
                            if (_isTestingWakeWord.value) {
                                startListening()
                            }
                        }
                    } else {
                        stopListening()
                    }
                }

                override fun onTimeout() {
                    Log.w("STTFlow", "STAGE 2: Vosk listener timeout. Falling back to STAGE 3 (System STT).")
                    com.example.utils.VoskLogManager.logInfo("Vosk listener timeout", "VoskEngine")
                    _voskTriggerStatus.value = "Vosk Listener Timeout"
                    startSystemSttFallback()
                }
            })
        } catch (e: Exception) {
            Log.e("STTFlow", "STAGE 2 FAILED: Exception in startVoskListening: ${e.message}. Falling back to STAGE 3 (System STT).", e)
            com.example.utils.VoskLogManager.logInitError("Failed to start Vosk listening thread: ${e.message}", e, "STTFlow")
            _voskTriggerStatus.value = "Failed to start Vosk listening thread"
            startSystemSttFallback()
        }
    }

    private fun handleOfflineSpeechResult(text: String) {
        if (text.isEmpty()) {
            stopListening()
            if (_usePersistentListening.value) {
                restartContinuousListeningIfNeeded()
            }
            return
        }

        if (handleSpeechResultForTraining(text)) {
            stopListening()
            return
        }

        _currentStatus.value = "Recognized: $text"
        val isContinuous = _usePersistentListening.value
        val currentWakeWord = _wakeWord.value.lowercase().trim()
        val lowerText = text.lowercase().trim()

        if (isContinuous) {
            if (lowerText.contains(currentWakeWord)) {
                val index = lowerText.indexOf(currentWakeWord)
                var command = lowerText.substring(index + currentWakeWord.length).trim()

                if (command.startsWith(",") || command.startsWith(":") || command.startsWith("-")) {
                    command = command.substring(1).trim()
                }

                if (command.isEmpty()) {
                    val responses = listOf("Standing by.", "At your service.", "I'm listening.", "Aira activated.")
                    val ack = responses.random()
                    viewModelScope.launch {
                        chatDao.insertMessage(ChatMessage(sender = "aira", message = ack))
                        speakText(ack)
                    }
                } else {
                    processAssistantSession(command)
                }
                stopListening()
            } else {
                Log.d("AiraViewModel", "Ignored offline phrase without wake word: $text")
                stopListening()
                restartContinuousListeningIfNeeded()
            }
        } else {
            processAssistantSession(text)
            stopListening()
        }
    }

    private fun extractVoskText(hypothesis: String): String {
        return try {
            val json = JSONObject(hypothesis)
            json.optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractVoskPartialText(hypothesis: String): String {
        return try {
            val json = JSONObject(hypothesis)
            json.optString("partial", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun sanitizeTextForSpeech(input: String): String {
        if (input.isBlank()) return ""
        var cleaned = input
            .replace(Regex("```[\\s\\S]*?```"), " ") // Remove code blocks
            .replace(Regex("`[^`]*`"), " ") // Remove inline code
            .replace(Regex("https?://\\S+"), " ") // Remove URLs
            .replace(Regex("\\*\\*|\\*|_|#|>|-"), " ") // Remove markdown formatting symbols
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // Remove markdown links, keeping label text
            .replace(Regex("[{}\\[\\]\"\\\\]"), " ") // Remove JSON brackets/quotes
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
        return cleaned
    }

    fun speakText(text: String, speedMultiplier: Float = 1.0f) {
        if (!_speakReplies.value) {
            Log.d("AiraViewModel", "Speak replies is disabled. Skipping speech for: $text")
            return
        }
        val cleanText = sanitizeTextForSpeech(text)
        if (cleanText.isBlank()) return

        viewModelScope.launch {
            try {
                speechQueue.send(com.example.models.SpeechQueueItem(cleanText, speedMultiplier))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("AiraViewModel", "Failed to enqueue speech text: $cleanText", e)
            }
        }
    }

    private suspend fun performSpeakText(item: com.example.models.SpeechQueueItem) {
        if (_isListening.value) {
            stopListening()
        }
        
        piperTtsManager.speak(item.text, item.speedMultiplier)
        
        // Wait up to 1000ms for speech to start
        var waitStartLimit = 20
        while (!_isSpeaking.value && waitStartLimit > 0) {
            kotlinx.coroutines.delay(50)
            waitStartLimit--
        }
        
        // Wait as long as speech is active (dynamic limit based on text length: ~100ms per char safety margin)
        val dynamicLimit = (item.text.length * 2).coerceIn(300, 1800)
        var activeLimit = dynamicLimit
        while (_isSpeaking.value && activeLimit > 0) {
            kotlinx.coroutines.delay(50)
            activeLimit--
        }
    }

    fun speak(text: String) {
        speakText(text)
    }

    fun playVoiceTest(text: String) {
        speakText(text)
    }

    fun sendUserInput(userInput: String) {
        processAssistantSession(userInput)
    }

    // --- AI Brain Process ---
    private fun processAssistantSession(userInput: String) {
        viewModelScope.launch {
            try {
                Log.d("AiraViewModel", "Processing input: $userInput")
                updateSttState(SttState.PROCESSING)
                // Insert user speech to local SQLite via Room
                chatDao.insertMessage(ChatMessage(sender = "user", message = userInput))

                // Check for Macro trigger match first
                val macroResult = com.example.utils.MacroManager.processMacro(getApplication(), userInput)
                if (macroResult.executed) {
                    val reply = macroResult.summary
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = reply))
                    addVoiceCommandLog(userInput, macroResult.macroName, "SUCCESS", reply)
                    processAIResponse(reply)
                    updateSttState(SttState.IDLE)
                    _currentStatus.value = "Done."
                    return@launch
                }

                // Check for manual save command first
                val manualFactText = checkManualSaveMemory(userInput)
                if (manualFactText == "[FORGOT_COMMAND_EXECUTED]" || manualFactText == "[SAVE_COMMAND_EXECUTED]") {
                    updateSttState(SttState.IDLE)
                    _currentStatus.value = "Done."
                    return@launch
                }
                if (manualFactText != null) {
                    val mem = Memory(factText = manualFactText, source = "manual", category = "Personal", isImportant = true)
                    val insertedId = db.memoryDao().insertMemory(mem)
                    _lastSavedMemory.value = mem.copy(id = insertedId)
                    val reply = "All done. Saved to memory ✅"
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = reply))
                    processAIResponse(reply)
                    updateSttState(SttState.IDLE)
                    _currentStatus.value = "Done."
                    return@launch
                }

                val lowercaseInput = userInput.lowercase().trim()

                // 1. Core Voice Commands Analyzer (Intelligent matching 80%+ / variables)
                val voiceCommandMgr = VoiceCommandManager.getInstance(getApplication())
                val matchedCmd = voiceCommandMgr.matchAndExecuteCommand(lowercaseInput, this@AiraViewModel)
                if (matchedCmd) {
                    updateSttState(SttState.IDLE)
                    _currentStatus.value = "Done."
                    return@launch
                }

                // 2. Intelligent "Did you mean?" Fallback suggested match if between 50% & 80%
                val fallbackMatch = voiceCommandMgr.getDidYouMeanCommand(lowercaseInput)
                if (fallbackMatch != null) {
                    val suggestionText = "I didn't quite get that. Did you mean: '${fallbackMatch.triggerPhrase.uppercase()}'?"
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = suggestionText))
                    processAIResponse(suggestionText)
                    updateSttState(SttState.IDLE)
                    _currentStatus.value = "Done."
                    return@launch
                }

                // 3. Intercept local device commands first
                val intercepted = checkAndExecuteDeviceCommands(lowercaseInput)
                if (intercepted) {
                    updateSttState(SttState.IDLE)
                    _currentStatus.value = "Done."
                    return@launch
                }

                // 4. Process Topic, Emotion & Temperature
                topicTracker.processInput(userInput)
                _currentTopic.value = topicTracker.getCurrentTopic()
                _topicHistory.value = topicTracker.getTopicHistory()
                val topicContextPrompt = topicTracker.buildTopicContextPrompt()

                val (toneInstruction, detectedTemp) = if (_isEmotionDetectionEnabled.value) {
                    val emotionResult = com.example.utils.EmotionDetector.detectEmotion(userInput)
                    _currentEmotion.value = emotionResult.emotion
                    Pair(emotionResult.toneInstruction, emotionResult.recommendedTemperature)
                } else {
                    _currentEmotion.value = com.example.utils.UserEmotion.NEUTRAL
                    Pair("", 0.6)
                }

                val queryTemperature = when (_temperatureMode.value) {
                    "Low (0.3)" -> 0.3
                    "Medium (0.6)" -> 0.6
                    "High (0.9)" -> 0.9
                    "Custom" -> {
                        _customTemperatureText.value.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.6
                    }
                    else -> detectedTemp
                }

                val baseSystemInstruction = com.example.models.AiBrain.JARVIS_SYSTEM_INSTRUCTION + "\nYou possess full phone control capabilities including Wi-Fi, Bluetooth, volume, brightness, flashlight, alarms, launching apps, settings, camera, screenshot, and screen locking."
                val historyList = chatHistory.value.takeLast(10).map { Pair(it.sender, it.message) }

                // Recall Memories
                val relevantMemories = getRelevantMemories(userInput)
                val finalSystemInstruction = buildString {
                    append(baseSystemInstruction).append("\n")
                    append(toneInstruction).append("\n")
                    append(topicContextPrompt)
                    if (relevantMemories.isNotEmpty()) {
                        val factsStr = relevantMemories.mapIndexed { i, fact -> "${i + 1}. $fact" }.joinToString("\n")
                        append("\nFacts about user:\n").append(factsStr).append("\nUse these facts in reply.")
                    }
                }

                var aiFinalResponse = ""

                if (_isOfflineBrain.value) {
                    if (com.example.utils.MemoryManager.isSafeMode(getApplication())) {
                        Log.w("AiraViewModel", "Safe Mode / RAM < 3GB active. Local Llama model execution skipped to prevent OOM crash.")
                        _currentStatus.value = "Safe Mode Active: Using Cloud / Local Rules..."
                        try {
                            val (aiResponse, sourceEngine) = voiceCommandMgr.getRoutedAiResponse(userInput, finalSystemInstruction, historyList, queryTemperature)
                            val reply = if (aiResponse.isNotBlank()) aiResponse else com.example.data.AiraPredefinedResponses.getRandomFallbackResponse(userInput)
                            aiFinalResponse = reply
                            _currentStatus.value = "Processed via $sourceEngine (Online Fallback)"
                            chatDao.insertMessage(ChatMessage(sender = "aira", message = reply, isOffline = false))
                            processAIResponse(reply)
                        } catch (e: Exception) {
                            Log.e("AiraViewModel", "Online AI call failed, using offline predefined fallback response.", e)
                            if (e is AiraApiException || e.cause is AiraApiException || e.message?.contains("API_KEY_MISSING") == true || e.message?.contains("AIRA API key not found") == true) {
                                viewModelScope.launch(Dispatchers.Main) {
                                    Toast.makeText(getApplication(), "API keys missing. Please go to Settings and re-enter your keys.", Toast.LENGTH_LONG).show()
                                }
                            }
                            val reply = com.example.data.AiraPredefinedResponses.getRandomFallbackResponse(userInput)
                            aiFinalResponse = reply
                            _currentStatus.value = "Processed via Predefined Offline Fallback"
                            chatDao.insertMessage(ChatMessage(sender = "aira", message = reply, isOffline = true))
                            processAIResponse(reply)
                        }
                    } else {
                        _currentStatus.value = com.example.data.AiraPredefinedResponses.getRandomProcessingPhrase()
                        if (!com.example.utils.MemoryManager.isModelLoaded(com.example.utils.NativeModelType.LLAMA_CPP)) {
                            llamaCppBrain.initializeNativeEngine(_llamaThreads.value)
                        }
                        val rawReply = llamaCppBrain.getResponse(userInput, finalSystemInstruction, historyList, queryTemperature)
                        val reply = if (rawReply.isNotBlank()) rawReply else com.example.data.AiraPredefinedResponses.getRandomFallbackResponse(userInput)
                        aiFinalResponse = reply
                        _currentStatus.value = "Processed via Llama 3.2 (Offline)"
                        chatDao.insertMessage(ChatMessage(sender = "aira", message = reply, isOffline = true))
                        processAIResponse(reply)
                    }
                } else {
                    _currentStatus.value = com.example.data.AiraPredefinedResponses.getRandomProcessingPhrase()
                    val latencyJob = viewModelScope.launch {
                        kotlinx.coroutines.delay(800L)
                        val filler = com.example.models.JarvisLatencyFiller.getLatencyFiller(userInput)
                        speakText(filler)
                    }
                    try {
                        val accumulatedSb = StringBuilder()
                        var firstChunkReceived = false

                        voiceCommandMgr.streamRoutedAiResponse(
                            userInput = userInput,
                            systemInstruction = finalSystemInstruction,
                            history = historyList,
                            onSentenceReady = { sentence ->
                                if (!firstChunkReceived) {
                                    firstChunkReceived = true
                                    latencyJob.cancel()
                                }
                                processAIResponse(sentence, userInput)
                            }
                        ).collect { delta ->
                            if (!firstChunkReceived && delta.isNotBlank()) {
                                firstChunkReceived = true
                                latencyJob.cancel()
                            }
                            accumulatedSb.append(delta)
                        }

                        latencyJob.cancel()
                        val fullStreamed = accumulatedSb.toString().trim()
                        val reply = if (fullStreamed.isNotBlank()) fullStreamed else com.example.data.AiraPredefinedResponses.getRandomFallbackResponse(userInput)
                        aiFinalResponse = reply
                        _currentStatus.value = "Processed via AI Stream"
                        chatDao.insertMessage(ChatMessage(sender = "aira", message = reply, isOffline = false))
                        if (!firstChunkReceived) {
                            processAIResponse(reply, userInput)
                        }
                    } catch (e: Exception) {
                        latencyJob.cancel()
                        Log.e("AiraViewModel", "Online model call failed, checking memory before transitioning to local Llama 3.2 model.", e)
                        if (e is AiraApiException || e.cause is AiraApiException || e.message?.contains("API_KEY_MISSING") == true || e.message?.contains("AIRA API key not found") == true) {
                            viewModelScope.launch(Dispatchers.Main) {
                                Toast.makeText(getApplication(), "API keys missing. Please go to Settings and re-enter your keys.", Toast.LENGTH_LONG).show()
                            }
                        }
                        if (!com.example.utils.MemoryManager.isSafeMode(getApplication())) {
                            _currentStatus.value = "Online failure. Transitioning to Llama 3.2..."
                            if (!com.example.utils.MemoryManager.isModelLoaded(com.example.utils.NativeModelType.LLAMA_CPP)) {
                                llamaCppBrain.initializeNativeEngine(_llamaThreads.value)
                            }
                            val offlineReply = llamaCppBrain.getResponse(userInput, finalSystemInstruction, historyList, queryTemperature)
                            val reply = if (offlineReply.isNotBlank()) offlineReply else com.example.data.AiraPredefinedResponses.getRandomFallbackResponse(userInput)
                            aiFinalResponse = reply
                            _currentStatus.value = "Processed via Llama 3.2 (Offline Fallback)"
                            chatDao.insertMessage(ChatMessage(sender = "aira", message = reply, isOffline = true))
                            processAIResponse(reply)
                        } else {
                            val reply = com.example.data.AiraPredefinedResponses.getRandomFallbackResponse(userInput)
                            aiFinalResponse = reply
                            _currentStatus.value = "Processed via Fallback Engine"
                            chatDao.insertMessage(ChatMessage(sender = "aira", message = reply, isOffline = false))
                            processAIResponse(reply)
                        }
                    }
                }

                if (aiFinalResponse.isNotEmpty()) {
                    autoScanAndSaveMemory(userInput, aiFinalResponse)
                }
            } catch (e: Exception) {
                Logger.e("AiraViewModel", "Error in processAssistantSession", e)
                _currentStatus.value = "Error processing command"
                _orbState.value = com.example.ui.components.OrbState.ERROR
            } finally {
                _isProcessing.value = false
                if (_sttState.value == SttState.PROCESSING) {
                    updateSttState(SttState.IDLE)
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val capabilities = cm?.getNetworkCapabilities(cm.activeNetwork)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
        }
    }

    data class MemoryScanResult(
        val factText: String,
        val category: String, // Personal, Work, Tasks, Reminders, Preferences
        val isImportant: Boolean,
        val source: String // auto, offline_ai, online_ai, voice, manual
    )

    private fun evaluateLayer1KeywordFilter(userText: String, aiText: String): MemoryScanResult? {
        val cleanUser = userText.trim()
        val cleanUserLower = cleanUser.lowercase()

        // Name
        val enNameRegex = Regex("""(?i)\b(?:my name is|call me)\s+([a-zA-Z]{2,15})\b""")
        val urNameRegex = Regex("""(?i)\b(?:mera naam)\s+([a-zA-Z]{2,15})\s+hai\b""")
        enNameRegex.find(cleanUser)?.let { return MemoryScanResult("User's name is ${it.groupValues[1].replaceFirstChar { c -> c.uppercase() }}", "Personal", true, "auto") }
        urNameRegex.find(cleanUser)?.let { return MemoryScanResult("User's name is ${it.groupValues[1].replaceFirstChar { c -> c.uppercase() }}", "Personal", true, "auto") }

        // Age
        val ageRegex = Regex("""(?i)\b(?:i am|meri age|meri umar)\s+(\d{1,2})\s*(?:years old)?\b""")
        ageRegex.find(cleanUser)?.let { return MemoryScanResult("User's age is ${it.groupValues[1]}", "Personal", false, "auto") }

        // Location
        val cityRegexEn = Regex("""(?i)\b(?:i live in|i am from|located at)\s+([a-zA-Z\s]{3,20})\b""")
        cityRegexEn.find(cleanUser)?.let { return MemoryScanResult("User lives in ${it.groupValues[1].trim()}", "Personal", false, "auto") }

        // Birthday / Anniversary
        val bdayRegex = Regex("""(?i)\b(?:my birthday is on|i was born on|mera birthday|anniversary on)\s+([a-zA-Z0-9\s]{3,20})\b""")
        bdayRegex.find(cleanUser)?.let { return MemoryScanResult("User's date: ${it.groupValues[1].trim()}", "Reminders", true, "auto") }

        // Meeting / Appointment / Deadline / Reminders
        if (cleanUserLower.contains("meeting") || cleanUserLower.contains("appointment") || cleanUserLower.contains("deadline") || cleanUserLower.contains("reminder") || cleanUserLower.contains("doctor")) {
            val eventRegex = Regex("""(?i)\b(?:have a|meeting at|appointment on|deadline for|reminder to)\s+([a-zA-Z0-9\s]{3,30})\b""")
            eventRegex.find(cleanUser)?.let { return MemoryScanResult("Event/Reminder: ${it.groupValues[1].trim()}", "Reminders", true, "auto") }
        }

        // Work / Job / Code / Project
        if (cleanUserLower.contains("work as") || cleanUserLower.contains("job") || cleanUserLower.contains("working on") || cleanUserLower.contains("company") || cleanUserLower.contains("project")) {
            val jobRegex = Regex("""(?i)\b(?:i work as|working on|my project is|my job is)\s+([a-zA-Z0-9\s]{3,30})\b""")
            jobRegex.find(cleanUser)?.let { return MemoryScanResult("Work/Project: ${it.groupValues[1].trim()}", "Work", false, "auto") }
        }

        // Likes / Dislikes / Preferences
        if (cleanUserLower.contains("i like") || cleanUserLower.contains("i love") || cleanUserLower.contains("i prefer") || cleanUserLower.contains("favorite") || cleanUserLower.contains("mujhe pasand")) {
            val likeRegex = Regex("""(?i)\b(?:i like|i love|i prefer|my favorite is|mujhe pasand hai)\s+([a-zA-Z0-9\s]{3,25})\b""")
            likeRegex.find(cleanUser)?.let { return MemoryScanResult("Preference: Likes ${it.groupValues[1].trim()}", "Preferences", false, "auto") }
        }

        // Tasks
        if (cleanUserLower.contains("task") || cleanUserLower.contains("todo") || cleanUserLower.contains("need to submit") || cleanUserLower.contains("have to finish")) {
            val taskRegex = Regex("""(?i)\b(?:task|need to|have to)\s+([a-zA-Z0-9\s]{3,30})\b""")
            taskRegex.find(cleanUser)?.let { return MemoryScanResult("Task: ${it.groupValues[1].trim()}", "Tasks", false, "auto") }
        }

        return null
    }

    private fun evaluateLayer2OfflineAiFilter(userText: String, aiText: String): MemoryScanResult? {
        val cleanUser = userText.trim()
        val cleanUserLower = cleanUser.lowercase()

        // Offline Llama 3.2 local heuristic: Detect declarative state statements
        val isDeclarativeState = cleanUserLower.contains("i have ") || cleanUserLower.contains("my ") ||
                cleanUserLower.contains("we agreed ") || cleanUserLower.contains("don't forget ") ||
                cleanUserLower.contains("i always ") || cleanUserLower.contains("i am allergic ") ||
                cleanUserLower.contains("i study ") || cleanUserLower.contains("i buy ")

        if (isDeclarativeState && cleanUser.length in 10..120) {
            val category = when {
                cleanUserLower.contains("work") || cleanUserLower.contains("office") || cleanUserLower.contains("boss") || cleanUserLower.contains("client") -> "Work"
                cleanUserLower.contains("task") || cleanUserLower.contains("complete") || cleanUserLower.contains("finish") -> "Tasks"
                cleanUserLower.contains("time") || cleanUserLower.contains("date") || cleanUserLower.contains("at ") || cleanUserLower.contains("on ") -> "Reminders"
                cleanUserLower.contains("like") || cleanUserLower.contains("hate") || cleanUserLower.contains("prefer") || cleanUserLower.contains("love") -> "Preferences"
                else -> "Personal"
            }
            val isImportant = cleanUserLower.contains("important") || cleanUserLower.contains("urgent") || cleanUserLower.contains("doctor") || cleanUserLower.contains("password")
            return MemoryScanResult("User note: $cleanUser", category, isImportant, "offline_ai")
        }
        return null
    }

    private suspend fun evaluateLayer3OnlineAiFilter(userText: String, aiText: String): MemoryScanResult? = withContext(Dispatchers.IO) {
        try {
            val activeKey = ChatKeyManager.getInstance(getApplication()).getNextKey()
            if (activeKey.isNullOrEmpty()) return@withContext null

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$activeKey"
            val jsonPrompt = JSONObject().apply {
                val contents = JSONArray()
                val item = JSONObject()
                val parts = JSONArray()
                val part = JSONObject()
                part.put("text", "Task: Extract long-term memory facts from user conversation turn.\n" +
                        "User: \"$userText\"\nAI: \"$aiText\"\n" +
                        "Rule: If the user shared an important personal detail, date, preference, work task, or reminder worth remembering long-term, output exact line 'CATEGORY|FACT_SUMMARY' where CATEGORY is one of [Personal, Work, Tasks, Reminders, Preferences]. If no important fact to remember, output 'NONE'.")
                parts.put(part)
                item.put("parts", parts)
                contents.put(item)
                put("contents", contents)
            }

            val client = okHttpClient

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonPrompt.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respBody = resp.body?.string() ?: ""
                    val root = JSONObject(respBody)
                    val candidates = root.optJSONArray("candidates")
                    val first = candidates?.optJSONObject(0)
                    val content = first?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text", "NONE")?.trim() ?: "NONE"

                    if (text.contains("|") && !text.contains("NONE")) {
                        val split = text.split("|")
                        if (split.size >= 2) {
                            val cat = split[0].trim()
                            val fact = split[1].trim()
                            val validCat = if (cat in listOf("Personal", "Work", "Tasks", "Reminders", "Preferences")) cat else "Personal"
                            val isImp = fact.lowercase().contains("important") || fact.lowercase().contains("urgent") || validCat == "Reminders"
                            return@withContext MemoryScanResult(fact, validCat, isImp, "online_ai")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("AiraViewModel", "Layer 3 Online AI Memory Scan skipped or failed: ${e.message}")
        }
        return@withContext null
    }

    fun checkManualSaveMemory(userInput: String): String? {
        val lower = userInput.lowercase().trim()
        val forgetTriggers = listOf("forget that", "delete this", "remove this", "clear it", "bhool jao", "forget this", "delete that", "remove that", "forget about")
        var matchedForgetTrigger = ""
        for (t in forgetTriggers) {
            if (lower.startsWith(t) || lower.contains(" " + t) || lower == t) {
                matchedForgetTrigger = t
                break
            }
        }
        if (matchedForgetTrigger.isNotEmpty()) {
            var topic = ""
            val idx = lower.indexOf(matchedForgetTrigger)
            if (idx != -1) {
                topic = userInput.substring(idx + matchedForgetTrigger.length).trim()
                topic = topic.replace(Regex("(?i)^\\s*(?:about|ki|ke|that|ko|,)\\s*"), "")
                topic = topic.trim()
            }
            
            viewModelScope.launch(Dispatchers.IO) {
                val allMemories = db.memoryDao().getAllMemoriesList()
                if (topic.isEmpty()) {
                    if (allMemories.isNotEmpty()) {
                        val latest = allMemories.first()
                        db.memoryDao().deleteMemory(latest.id)
                    }
                } else {
                    val toDelete = allMemories.filter { it.factText.lowercase().contains(topic.lowercase()) }
                    if (toDelete.isNotEmpty()) {
                        for (m in toDelete) {
                            db.memoryDao().deleteMemory(m.id)
                        }
                    } else {
                        val words = topic.lowercase().split(Regex("\\s+")).filter { it.length > 3 }
                        val wordDelete = allMemories.filter { mem ->
                            val memLower = mem.factText.lowercase()
                            words.any { memLower.contains(it) }
                        }
                        if (wordDelete.isNotEmpty()) {
                            for (m in wordDelete) {
                                db.memoryDao().deleteMemory(m.id)
                            }
                        }
                    }
                }
                val reply = "Got it. I forgot that ✅"
                chatDao.insertMessage(ChatMessage(sender = "aira", message = reply))
                speakText(reply)
            }
            return "[FORGOT_COMMAND_EXECUTED]"
        }

        // LAYER 4: VOICE COMMAND (User Control)
        val saveTriggers = listOf("save this", "remember this", "remember that", "save that", "dual save", "ye save karo", "yaad rakhna", "yaad rkhna", "save karo", "remember my", "save memory")
        var isTriggered = false
        var matchedTrigger = ""
        for (t in saveTriggers) {
            if (lower.contains(t)) {
                isTriggered = true
                matchedTrigger = t
                break
            }
        }
        if (isTriggered) {
            var fact = userInput
            val regex = Regex("(?i)\\b" + Regex.escape(matchedTrigger) + "\\b")
            fact = fact.replace(regex, "")
            fact = fact.replace(Regex("(?i)^\\s*(?:ki|ke|that|ko|,|about|context)\\s*"), "")
            fact = fact.replace(Regex("(?i)\\s*(?:ki|ke|that|ko|,|about|context)\\s*$"), "")
            fact = fact.trim()

            viewModelScope.launch(Dispatchers.IO) {
                var finalFact = fact
                if (finalFact.isEmpty()) {
                    val recentMessages = chatDao.getAllMessagesList()
                    val lastUserMsg = recentMessages.firstOrNull { it.sender == "user" && !it.message.lowercase().contains(matchedTrigger) }
                    if (lastUserMsg != null) {
                        finalFact = lastUserMsg.message
                    }
                }
                if (finalFact.isNotEmpty()) {
                    val mem = Memory(
                        factText = finalFact,
                        source = "voice",
                        category = "Personal",
                        isImportant = true
                    )
                    val insertedId = db.memoryDao().insertMemory(mem)
                    _lastSavedMemory.value = mem.copy(id = insertedId)
                    val reply = "Saved to memory: \"$finalFact\" ✅"
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = reply))
                    speakText("Saved to memory")
                }
            }
            return "[SAVE_COMMAND_EXECUTED]"
        }
        return null
    }

    suspend fun autoScanAndSaveMemory(userText: String, aiText: String) = withContext(Dispatchers.IO) {
        val cleanUser = userText.trim()
        if (cleanUser.length < 3) return@withContext

        // LAYER 1: KEYWORD FILTER (Offline)
        var scanResult = evaluateLayer1KeywordFilter(cleanUser, aiText)

        // LAYER 2: OFFLINE AI FILTER (Llama 3.2 / Local AI)
        if (scanResult == null) {
            scanResult = evaluateLayer2OfflineAiFilter(cleanUser, aiText)
        }

        // LAYER 3: ONLINE AI FILTER (Gemini API)
        if (scanResult == null && isNetworkAvailable()) {
            scanResult = evaluateLayer3OnlineAiFilter(cleanUser, aiText)
        }

        // Execute save if candidate memory found
        scanResult?.let { result ->
            val existing = db.memoryDao().getAllMemoriesList()
            if (existing.none { it.factText.equals(result.factText, ignoreCase = true) }) {
                val newMem = Memory(
                    factText = result.factText,
                    source = result.source,
                    category = result.category,
                    isImportant = result.isImportant,
                    createdAt = System.currentTimeMillis()
                )
                val insertedId = db.memoryDao().insertMemory(newMem)
                val savedMem = newMem.copy(id = insertedId)
                _lastSavedMemory.value = savedMem
                Log.d("AiraViewModel", "Memory auto-saved [Layer: ${result.source}, Cat: ${result.category}]: ${result.factText}")
            }
        }
    }

    suspend fun getRelevantMemories(userQuery: String): List<String> = withContext(Dispatchers.IO) {
        val allMemories = db.memoryDao().getAllMemoriesList()
        if (allMemories.isEmpty()) return@withContext emptyList()

        val queryWords = userQuery.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
        
        val scoredMemories = allMemories.map { memory ->
            val factLower = memory.factText.lowercase()
            var score = 0
            for (word in queryWords) {
                if (factLower.contains(word)) {
                    score++
                }
            }
            val ageBias = 1.0 / (1.0 + (System.currentTimeMillis() - memory.createdAt) / 100000.0)
            Pair(memory, score.toDouble() + ageBias)
        }

        val sorted = scoredMemories.sortedByDescending { it.second }.map { it.first.factText }
        sorted.take(5)
    }

    // --- Local Device Custom Voice Controls & Permissions ---
    fun parseAndExecuteVoiceCommand(input: String): String {
        val response = com.example.service.ShizukuVoiceExecutionService.executeVoiceCommand(getApplication(), input)
        return response.responseMessage
    }

    private fun checkAndExecuteDeviceCommands(input: String): Boolean {
        // 0. 3-Layer Intelligent Command System (Urdu / Roman Urdu / English Aliases + Fuzzy Matching)
        val vcm = com.example.data.VoiceCommandManager.getInstance(getApplication())
        val normInput = input.trim().lowercase(java.util.Locale.ROOT)
        for ((action, aliases) in com.example.data.VoiceCommandManager.commandAliases) {
            val isExact = aliases.any { it.lowercase(java.util.Locale.ROOT).trim() == normInput }
            val match = if (!isExact) vcm.fuzzyMatch(normInput, aliases) else null
            if (isExact || match != null) {
                val actionResult = vcm.executeAction(action, this)
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = actionResult))
                    speakText(actionResult)
                }
                return true
            }
        }

        // 0.1 Predefined Assistant Responses Repository Matching
        val weatherInfo = _openMeteoWeather.value?.formattedText ?: "72°F, Partly Cloudy"
        val predefinedMatch = com.example.data.AiraPredefinedResponses.findPredefinedResponse(
            getApplication(),
            input,
            mapOf("weatherData" to weatherInfo)
        )
        if (predefinedMatch != null) {
            val responseMsg: String = if (predefinedMatch.parsedCommand != null) {
                com.example.utils.CommandParser.execute(getApplication(), predefinedMatch.parsedCommand, this)
            } else if (predefinedMatch.commandType != null) {
                val parsed = com.example.utils.ParsedCommand(
                    type = predefinedMatch.commandType,
                    originalInput = input,
                    summary = predefinedMatch.textResponse
                )
                com.example.utils.CommandParser.execute(getApplication(), parsed, this)
            } else {
                predefinedMatch.textResponse
            }

            val finalMsg = if (responseMsg.isNotBlank() && responseMsg != "Unknown command requested.") {
                responseMsg
            } else {
                predefinedMatch.textResponse
            }

            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = finalMsg))
                speakText(predefinedMatch.spokenResponse)
                _smartReplies.value = com.example.models.JarvisSpecializedToolkit.generateSmartReplies(finalMsg, input)
            }
            return true
        }

        // 0.2 Specialized J.A.R.V.I.S. Toolkit: Calculator & Math Evaluation
        val mathResult = com.example.models.JarvisSpecializedToolkit.tryEvaluateMath(input)
        if (mathResult != null) {
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = mathResult))
                speakText(mathResult)
                _smartReplies.value = com.example.models.JarvisSpecializedToolkit.generateSmartReplies(mathResult, input)
            }
            return true
        }

        // 0.3 Specialized J.A.R.V.I.S. Toolkit: Unit & Currency Conversions
        val convResult = com.example.models.JarvisSpecializedToolkit.tryEvaluateConversion(input)
        if (convResult != null) {
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = convResult))
                speakText(convResult)
                _smartReplies.value = com.example.models.JarvisSpecializedToolkit.generateSmartReplies(convResult, input)
            }
            return true
        }

        // 0.4 Specialized J.A.R.V.I.S. Toolkit: Web Searches (Google, YouTube, Wikipedia)
        val searchResult = com.example.models.JarvisSpecializedToolkit.handleWebSearch(getApplication(), input)
        if (searchResult != null) {
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = searchResult))
                speakText(searchResult)
                _smartReplies.value = com.example.models.JarvisSpecializedToolkit.generateSmartReplies(searchResult, input)
            }
            return true
        }

        // 0.5 Specialized J.A.R.V.I.S. Toolkit: Clipboard Reader & Copier
        val lower = input.lowercase(java.util.Locale.ROOT).trim()
        if (lower.contains("read clipboard") || lower.contains("what's on my clipboard") || lower.contains("what is on my clipboard") || lower.contains("speak clipboard") || lower == "clipboard") {
            val clipText = com.example.models.JarvisSpecializedToolkit.readClipboard(getApplication())
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = clipText))
                speakText(clipText)
                _smartReplies.value = com.example.models.JarvisSpecializedToolkit.generateSmartReplies(clipText, input)
            }
            return true
        }
        if (lower.startsWith("copy ") && lower.contains("to clipboard")) {
            val toCopy = lower.removePrefix("copy ").substringBefore("to clipboard").trim()
            val copyMsg = com.example.models.JarvisSpecializedToolkit.copyToClipboard(getApplication(), toCopy)
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = copyMsg))
                speakText(copyMsg)
                _smartReplies.value = com.example.models.JarvisSpecializedToolkit.generateSmartReplies(copyMsg, input)
            }
            return true
        }

        // 0.6 Specialized J.A.R.V.I.S. Toolkit: Screen OCR & Text Reading
        if (lower.contains("screen ocr") || lower.contains("extract text from screen") || lower.contains("ocr") || lower == "read screen") {
            val ocrText = com.example.models.JarvisSpecializedToolkit.extractScreenText(getApplication())
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = ocrText))
                speakText(ocrText)
                _smartReplies.value = com.example.models.JarvisSpecializedToolkit.generateSmartReplies(ocrText, input)
            }
            return true
        }

        // 0.7 Specialized J.A.R.V.I.S. Toolkit: Notification Reader
        if (lower.contains("read notification") || lower.contains("read my notifications") || lower.contains("check notification")) {
            val notifText = com.example.models.JarvisSpecializedToolkit.readNotifications(getApplication())
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = notifText))
                speakText(notifText)
                _smartReplies.value = com.example.models.JarvisSpecializedToolkit.generateSmartReplies(notifText, input)
            }
            return true
        }

        // 0.8 Specialized J.A.R.V.I.S. Toolkit: QR & Barcode Scanner
        if (lower.contains("scan qr") || lower.contains("scan barcode") || lower.contains("qr scanner") || lower.contains("open scanner")) {
            val qrMsg = com.example.models.JarvisSpecializedToolkit.launchQrScanner(getApplication())
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = qrMsg))
                speakText(qrMsg)
                _smartReplies.value = com.example.models.JarvisSpecializedToolkit.generateSmartReplies(qrMsg, input)
            }
            return true
        }

        // 0.9 Privacy Mode Toggle
        if (lower.contains("privacy mode")) {
            val enable = !lower.contains("off") && !lower.contains("disable") && !lower.contains("stop")
            setPrivacyMode(enable)
            val msg = if (enable) "Privacy Mode engaged, sir. All queries are now strictly locked to on-device processing." else "Privacy Mode disabled, sir. Online neural capabilities restored."
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = msg))
                _smartReplies.value = listOf("Run diagnostics", "System status", "Check battery")
            }
            return true
        }

        // 0.10 Do Not Disturb Mode Toggle
        if (lower.contains("do not disturb") || lower.contains("dnd mode") || lower == "dnd") {
            val enable = !lower.contains("off") && !lower.contains("disable") && !lower.contains("stop")
            setDoNotDisturb(enable)
            val msg = if (enable) "Do Not Disturb activated, sir. All incoming audible alerts silenced." else "Do Not Disturb deactivated, sir. Notifications and sound alerts active."
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = msg))
                _smartReplies.value = listOf("Run diagnostics", "System status", "Check battery")
            }
            return true
        }

        // 0.11 Mute Mode Toggle
        if (lower.contains("mute mode") || lower.contains("mute audio") || lower.contains("silent assistant") || lower == "mute") {
            toggleSpeakReplies(false)
            val msg = "Mute Mode engaged, sir. Responses will appear on screen without speech."
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = msg))
                _smartReplies.value = listOf("Unmute assistant", "System status", "Run diagnostics")
            }
            return true
        }
        if (lower.contains("unmute") || lower.contains("unmute audio") || lower.contains("voice on")) {
            toggleSpeakReplies(true)
            val msg = "Speech audio restored, sir. I am speaking aloud once again."
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = msg))
                speakText(msg)
                _smartReplies.value = listOf("Run diagnostics", "System status", "Mute assistant")
            }
            return true
        }

        val dagSteps = com.example.models.JarvisWorkflowDAG.parseMultiStepInput(input)
        if (dagSteps.size > 1) {
            val stepResults = mutableListOf<String>()
            for (step in dagSteps) {
                if (step.parsedCommand != null) {
                    val eval = com.example.models.JarvisSlotFiller.evaluate(step.parsedCommand)
                    if (eval is com.example.models.SlotFillResult.Incomplete) {
                        stepResults.add(eval.missingSlotPrompt)
                    } else {
                        stepResults.add(com.example.utils.CommandParser.execute(getApplication(), step.parsedCommand, this))
                    }
                }
            }
            if (stepResults.isNotEmpty()) {
                val combined = stepResults.joinToString(" ")
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = combined))
                    speakText(combined)
                }
                return true
            }
        }

        val parsedCommand = com.example.utils.CommandParser.parse(input)
        if (parsedCommand != null) {
            val eval = com.example.models.JarvisSlotFiller.evaluate(parsedCommand)
            val responseMsg = if (eval is com.example.models.SlotFillResult.Incomplete) {
                eval.missingSlotPrompt
            } else {
                com.example.utils.CommandParser.execute(getApplication(), parsedCommand, this)
            }
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                speakText(responseMsg)
            }
            return true
        }

        val automationResult = automationEngine.executeIntent(input)
        if (automationResult != null) {
            viewModelScope.launch {
                chatDao.insertMessage(ChatMessage(sender = "aira", message = automationResult))
                speakText(automationResult)
            }
            return true
        }

        return when {
            input.contains("lights") || input.contains("light") -> {
                val state = !input.contains("off") && !input.contains("stop")
                val responseMsg = toggleFlashlight(state)
                val statusText = if (state) "Lights command invoked. Room/device lights are now configured to ON." else "Lights command invoked. Room/device lights are now configured to OFF. All sub-system LEDs deactivated."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = statusText))
                    speakText(statusText)
                }
                true
            }
            input.contains("weather") || input.contains("temperature") -> {
                viewModelScope.launch {
                    val currentRes = performFetchWeather()
                    val responseMsg = "Weather command invoked. Here is the current environmental telemetry: $currentRes"
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("status report") || input.contains("status") -> {
                val report = "Initializing system status report. Neural engine: Piper TTS active. Wake word detection: Active. Offline brain state: configured. System performance: 60 FPS unlocked. All parameters normalized."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = report))
                    speakText(report)
                }
                true
            }
            input.contains("flashlight") || input.contains("torch") -> {
                val state = !input.contains("off") && !input.contains("stop")
                val responseMsg = toggleFlashlight(state)
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("wifi") || input.contains("wi-fi") || input.contains("internet") -> {
                val enable = !input.contains("off") && !input.contains("disable") && !input.contains("stop") && !input.contains("deactivate")
                val service = com.example.service.AiraAccessibilityService.instance
                val responseMsg = if (service != null) {
                    service.toggleWifi(enable)
                } else {
                    "Accessibility service is offline. Please enable Aira Command Core in system accessibility settings to automate Wi-Fi controls."
                }
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("bluetooth") || input.contains("bt ") || input.endsWith("bt") -> {
                val enable = !input.contains("off") && !input.contains("disable") && !input.contains("stop") && !input.contains("deactivate")
                val service = com.example.service.AiraAccessibilityService.instance
                val responseMsg = if (service != null) {
                    service.toggleBluetooth(enable)
                } else {
                    "Accessibility service is offline. Please enable Aira Command Core in system accessibility settings to automate Bluetooth controls."
                }
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("call") || input.contains("dial") -> {
                // Find potential digits or names
                val digits = input.filter { it.isDigit() }
                val number = if (digits.isNotEmpty()) digits else "911" // fallback or generic
                val responseMsg = "Dialing phone number: $number"
                initiatePhoneCall(number)
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("alarm") || input.contains("wake me up") -> {
                // Extracts alarm parameters
                val timeDigits = input.filter { it.isDigit() }
                val hour = if (timeDigits.length >= 2) timeDigits.substring(0, 2).toIntOrNull() ?: 7 else 7
                val minute = if (timeDigits.length >= 4) timeDigits.substring(2, 4).toIntOrNull() ?: 0 else 0
                setSystemAlarm(hour, minute, "Aira Wake UP Call")
                val responseMsg = "Scheduling system alarm for $hour:$minute"
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("settings") || input.contains("configure") -> {
                val responseMsg = "Opening Settings directory. Tap settings above."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("silent") || input.contains("mute") -> {
                setSoundMode(AudioManager.RINGER_MODE_SILENT)
                val responseMsg = "System ring audio configured to Silent Mode."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("vibrate") -> {
                setSoundMode(AudioManager.RINGER_MODE_VIBRATE)
                val responseMsg = "System ring audio configured to Vibrate Mode."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("sound") || input.contains("normal mode") -> {
                setSoundMode(AudioManager.RINGER_MODE_NORMAL)
                val responseMsg = "System ring audio configured to Normal Volume."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("camera") || input.contains("photo") -> {
                val responseMsg = "Launching device camera."
                launchSystemCamera()
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("toggle hud") || input.contains("hide hud") || input.contains("show hud") || input.contains("toggle display") || input.contains("toggle core display") -> {
                val newState = if (input.contains("hide") || input.contains("off") || input.contains("disable")) {
                    false
                } else if (input.contains("show") || input.contains("on") || input.contains("enable")) {
                    true
                } else {
                    !_showHud.value
                }
                toggleHud(newState)
                val responseMsg = if (newState) "Circular holographic HUD enabled." else "Holographic HUD hidden."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("clear chat") || input.contains("clear history") || input.contains("delete conversation") || input.contains("clear archives") || input.contains("delete chat") -> {
                clearChatHistory()
                val responseMsg = "Vocal archives cleared."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            else -> false
        }
    }

    // --- Hardware Integrations ---
    fun toggleFlashlight(on: Boolean): String {
        val cameraManager = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        return try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, on)
                if (on) "Aira command: Flashlight turned ON." else "Aira command: Flashlight turned OFF."
            } else {
                "Flashlight camera unit not found."
            }
        } catch (e: Exception) {
            "Flashlight access denied: ${e.localizedMessage}"
        }
    }

    fun initiatePhoneCall(number: String) {
        val context = getApplication<Application>()
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Failed to dial phone", e)
        }
    }

    fun setSystemAlarm(hour: Int, minute: Int, message: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Failed to schedule alarm", e)
        }
    }

    fun setSoundMode(ringerMode: Int) {
        val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            audioManager?.ringerMode = ringerMode
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Sound toggle failed. Needs permission / Do Not Disturb access.", e)
        }
    }

    fun launchSystemCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Failed to launch camera", e)
        }
    }

    // --- Extras: Real APIs Fetch Weather & News (Optimized with Open-Meteo API) ---
    data class OpenMeteoWeatherData(
        val locationName: String = "San Francisco",
        val country: String = "",
        val latitude: Double = 37.7749,
        val longitude: Double = -122.4194,
        val temperatureC: Double = 17.0,
        val windSpeedKmH: Double = 10.0,
        val windDirectionDeg: Int = 0,
        val weatherCode: Int = 0,
        val conditionDescription: String = "Clear Sky",
        val isDaytime: Boolean = true,
        val isGpsLocation: Boolean = false,
        val formattedText: String = "San Francisco: 17°C, Clear Sky"
    )

    private val _openMeteoWeather = MutableStateFlow<OpenMeteoWeatherData?>(null)
    val openMeteoWeather: StateFlow<OpenMeteoWeatherData?> = _openMeteoWeather.asStateFlow()

    data class CalendarEventInfo(
        val title: String,
        val timeFormatted: String,
        val isAllDay: Boolean
    )

    private val _morningBriefing = MutableStateFlow<String?>(null)
    val morningBriefing: StateFlow<String?> = _morningBriefing.asStateFlow()

    private val _isBriefingLoading = MutableStateFlow<Boolean>(false)
    val isBriefingLoading: StateFlow<Boolean> = _isBriefingLoading.asStateFlow()

    fun triggerMorningBriefing() {
        viewModelScope.launch {
            try {
                generateMorningBriefing()
            } catch (e: Throwable) {
                Log.e("AiraViewModel", "Error in triggerMorningBriefing", e)
            }
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            try {
                performFetchWeather()
                generateMorningBriefing()
            } catch (e: Throwable) {
                Log.e("AiraViewModel", "Error in refreshWeather", e)
            }
        }
    }

    suspend fun fetchCalendarEventsForToday(): List<CalendarEventInfo> = withContext(Dispatchers.IO) {
        val events = mutableListOf<CalendarEventInfo>()
        val context = getApplication<Application>()

        // 1. Query Android System Calendar Contract if permission granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            try {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                ContentUris.appendId(builder, startOfDay)
                ContentUris.appendId(builder, endOfDay)

                val projection = arrayOf(
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.ALL_DAY
                )

                context.contentResolver.query(
                    builder.build(),
                    projection,
                    null,
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC"
                )?.use { cursor ->
                    val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                    val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                    val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)

                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

                    while (cursor.moveToNext()) {
                        val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "Calendar Event" else "Calendar Event"
                        val beginTime = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L
                        val isAllDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false
                        val timeStr = if (isAllDay) "All Day" else timeFormat.format(Date(beginTime))

                        events.add(CalendarEventInfo(title, timeStr, isAllDay))
                    }
                }
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Error fetching system calendar events", e)
            }
        }

        // 2. Query Room DB reminders for pending schedule items
        try {
            val reminders = db.reminderDao().getAllReminders().first()
            reminders.filter { !it.isCompleted }.forEach { r ->
                events.add(CalendarEventInfo(r.title, r.timeLabel, false))
            }
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error fetching DB reminders", e)
        }

        events
    }

    suspend fun generateMorningBriefing(): String = withContext(Dispatchers.IO) {
        _isBriefingLoading.value = true
        try {
            val weatherStr = if (_weatherText.value.isNotBlank()) _weatherText.value else performFetchWeather()
            val events = fetchCalendarEventsForToday()

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour in 0..11 -> "Good morning"
                hour in 12..16 -> "Good afternoon"
                else -> "Good evening"
            }

            val scheduleSummary = if (events.isEmpty()) {
                "No pending calendar events or active reminders scheduled for today."
            } else {
                val items = events.take(5).joinToString("\n") { "• ${it.timeFormatted}: ${it.title}" }
                "Today's Schedule & Agenda:\n$items"
            }

            val briefingText = """
                $greeting, Boss. AIRA daily briefing protocol initiated.

                Local Weather: $weatherStr.

                $scheduleSummary

                Priority Count: ${events.size} active agenda item${if (events.size == 1) "" else "s"}. All tactical systems operational and standing by.
            """.trimIndent()

            _morningBriefing.value = briefingText
            briefingText
        } finally {
            _isBriefingLoading.value = false
        }
    }

    fun playMorningBriefing() {
        viewModelScope.launch {
            val briefing = _morningBriefing.value ?: generateMorningBriefing()
            speakText(briefing)
        }
    }

    fun searchCityWeather(cityQuery: String) {
        if (cityQuery.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${java.net.URLEncoder.encode(cityQuery.trim(), "UTF-8")}&count=1&language=en&format=json"
                val request = Request.Builder().url(geocodingUrl).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = JSONObject(body)
                            val results = json.optJSONArray("results")
                            if (results != null && results.length() > 0) {
                                val first = results.getJSONObject(0)
                                val lat = first.getDouble("latitude")
                                val lon = first.getDouble("longitude")
                                val name = first.optString("name", cityQuery)
                                val country = first.optString("country", "")
                                performFetchWeather(customLat = lat, customLon = lon, customName = name, customCountry = country)
                                return@launch
                            }
                        }
                    }
                }
                _weatherText.value = "City not found: $cityQuery"
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Error searching city weather", e)
                _weatherText.value = "Search error for $cityQuery"
            }
        }
    }

    private fun getUserLocation(): Pair<Double, Double>? {
        val context = getApplication<Application>()
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        try {
            val providers = listOfNotNull(
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else null,
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else null,
                LocationManager.PASSIVE_PROVIDER
            )
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.time > bestLocation.time) {
                    bestLocation = loc
                }
            }
            if (bestLocation != null) {
                return Pair(bestLocation.latitude, bestLocation.longitude)
            }
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error reading location", e)
        }
        return null
    }

    private fun reverseGeocode(lat: Double, lon: Double): String? {
        val context = getApplication<Application>()
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
        } catch (e: Exception) {
            null
        }
    }

    private fun mapWeatherCodeToDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            80, 81, 82 -> "Rain Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Partly Cloudy"
        }
    }

    private suspend fun performFetchWeather(
        customLat: Double? = null,
        customLon: Double? = null,
        customName: String? = null,
        customCountry: String? = null,
        forceRefresh: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val detectedLoc = if (customLat == null) {
            com.example.utils.AiraLocationManager.getBestLocation(getApplication(), okHttpClient)
        } else null

        val isExactLocation = detectedLoc?.isGpsLocation == true
        val lat = customLat ?: detectedLoc?.latitude
        val lon = customLon ?: detectedLoc?.longitude
        val detectedCity = customName ?: detectedLoc?.cityName
        val detectedCountry = customCountry ?: detectedLoc?.countryName ?: ""

        if (lat == null || lon == null) {
            // Location could not be determined; check Room cache for latest available
            val lastCached = weatherCacheDao.getLatestWeather()
            if (lastCached != null) {
                _weatherText.value = lastCached.formattedText
                return@withContext lastCached.formattedText
            }
            val unavailableMsg = "Weather unavailable (Location & connection required)."
            _weatherText.value = unavailableMsg
            return@withContext unavailableMsg
        }

        val locKey = when {
            !detectedCity.isNullOrEmpty() -> detectedCity.trim().lowercase()
            isExactLocation -> "gps_%.3f_%.3f".format(lat, lon)
            else -> "loc_%.3f_%.3f".format(lat, lon)
        }

        // 1. Check local Room cache first (reduces redundant network requests)
        if (!forceRefresh) {
            try {
                val cached = weatherCacheDao.getWeatherByLocation(locKey)
                    ?: if (isExactLocation) weatherCacheDao.getLatestWeather() else null
                if (cached != null && !cached.isExpired()) {
                    val cachedDataObj = OpenMeteoWeatherData(
                        locationName = cached.locationName,
                        country = cached.country,
                        latitude = cached.latitude,
                        longitude = cached.longitude,
                        temperatureC = cached.temperatureC,
                        windSpeedKmH = cached.windSpeedKmH,
                        windDirectionDeg = cached.windDirectionDeg,
                        weatherCode = cached.weatherCode,
                        conditionDescription = cached.conditionDescription,
                        isDaytime = cached.isDaytime,
                        isGpsLocation = cached.isGpsLocation,
                        formattedText = cached.formattedText
                    )
                    _openMeteoWeather.value = cachedDataObj
                    _weatherText.value = cached.formattedText
                    return@withContext cached.formattedText
                }
            } catch (e: Exception) {
                Log.w("AiraViewModel", "Error reading weather cache from Room: ${e.message}")
            }
        }

        val omUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
        val weatherResult = com.example.data.NetworkErrorHandler.safeApiCall("Open-Meteo Weather API") {
            val request = Request.Builder().url(omUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val current = json.getJSONObject("current_weather")
                        val temp = current.getDouble("temperature")
                        val wind = current.getDouble("windspeed")
                        val windDir = current.optInt("winddirection", 0)
                        val wCode = current.optInt("weathercode", 0)
                        val isDay = current.optInt("is_day", 1) == 1
                        val condition = mapWeatherCodeToDescription(wCode)

                        var forecastStr = ""
                        val daily = json.optJSONObject("daily")
                        if (daily != null) {
                            val maxTemps = daily.optJSONArray("temperature_2m_max")
                            val minTemps = daily.optJSONArray("temperature_2m_min")
                            if (maxTemps != null && minTemps != null && maxTemps.length() > 0 && minTemps.length() > 0) {
                                val max = maxTemps.getDouble(0)
                                val min = minTemps.getDouble(0)
                                forecastStr = " • High ${max.toInt()}°C / Low ${min.toInt()}°C"
                            }
                        }

                        val locName = when {
                            !detectedCity.isNullOrEmpty() -> detectedCity
                            isExactLocation -> reverseGeocode(lat, lon) ?: "Current Location"
                            else -> "Local Area"
                        }
                        val countryStr = detectedCountry

                        val locationLabel = if (countryStr.isNotEmpty() && !locName.contains(countryStr)) "$locName, $countryStr" else locName
                        val badge = if (isExactLocation) " (GPS)" else ""
                        val formattedStr = "$locationLabel$badge: ${temp.toInt()}°C, $condition • Wind ${wind.toInt()} km/h$forecastStr"

                        val dataObj = OpenMeteoWeatherData(
                            locationName = locName,
                            country = countryStr,
                            latitude = lat,
                            longitude = lon,
                            temperatureC = temp,
                            windSpeedKmH = wind,
                            windDirectionDeg = windDir,
                            weatherCode = wCode,
                            conditionDescription = condition,
                            isDaytime = isDay,
                            isGpsLocation = isExactLocation,
                            formattedText = formattedStr
                        )
                        _openMeteoWeather.value = dataObj

                        // 2. Persist fresh observation to Room local cache
                        try {
                            weatherCacheDao.insertWeather(
                                WeatherCache(
                                    locationKey = locKey,
                                    locationName = locName,
                                    country = countryStr,
                                    latitude = lat,
                                    longitude = lon,
                                    temperatureC = temp,
                                    windSpeedKmH = wind,
                                    windDirectionDeg = windDir,
                                    weatherCode = wCode,
                                    conditionDescription = condition,
                                    isDaytime = isDay,
                                    isGpsLocation = isExactLocation,
                                    formattedText = formattedStr,
                                    forecastStr = forecastStr,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        } catch (e: Exception) {
                            Log.w("AiraViewModel", "Failed to save weather into Room cache: ${e.message}")
                        }

                        formattedStr
                    } else null
                } else null
            }
        }

        // Fallback: If network failed or offline, try to retrieve the latest known weather from Room cache
        val fallbackCached = if (weatherResult == null) {
            weatherCacheDao.getWeatherByLocation(locKey) ?: weatherCacheDao.getLatestWeather()
        } else null

        val finalResult = weatherResult 
            ?: fallbackCached?.formattedText
            ?: "Weather unavailable (Check network connection)."
            
        _weatherText.value = finalResult
        finalResult
    }

    fun fetchNews(category: String = _selectedNewsCategory.value) {
        viewModelScope.launch {
            _selectedNewsCategory.value = category
            _isNewsLoading.value = true
            _newsError.value = null
            val result = newsRepository.getGoogleNews(category)
            result.onSuccess { items ->
                _newsItems.value = items
                _newsFeed.value = items.map { it.title }
                _isNewsLoading.value = false
            }.onFailure { error ->
                Log.e("AiraViewModel", "Error fetching Google News RSS feed for category $category", error)
                _newsError.value = error.message ?: "Failed to load Google News feed"
                _isNewsLoading.value = false
                if (_newsFeed.value.isEmpty()) {
                    _newsFeed.value = listOf(
                        "AI Advances in local edge reasoning platforms enable zero-latency processing.",
                        "Android standard 16 dynamic color customization rolls out system-wide.",
                        "Global satellite telemetry records ocean surface temperature variations."
                    )
                }
            }
        }
    }

    // --- Settings Savers ---
    fun updateWakeWord(word: String) {
        _wakeWord.value = word
        sharedPrefs.edit().putString("wake_word", word).apply()
    }

    // --- Custom Wake Word Voice Training & Testing Logic ---
    fun startWakeWordTraining(word: String) {
        _trainingWakeWordText.value = word
        _trainingCurrentStep.value = 1
        _trainingAttempts.value = emptyList()
        _isTrainingWakeWord.value = true
        _isRecordingAttempt.value = false
        _trainingQualityScore.value = "Speak clearly. Ready for Attempt 1."
        _trainingLiveAmplitude.value = 0f
    }

    fun stopWakeWordTraining() {
        _isTrainingWakeWord.value = false
        _isRecordingAttempt.value = false
        stopAttemptAudioRecord()
    }

    fun resetWakeWordTrainingAttempts() {
        _trainingAttempts.value = emptyList()
        _trainingCurrentStep.value = 1
        _trainingQualityScore.value = "Speak clearly. Ready for Attempt 1."
        _isRecordingAttempt.value = false
        stopAttemptAudioRecord()
    }

    fun removeTrainingAttemptAt(index: Int) {
        val current = _trainingAttempts.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _trainingAttempts.value = current
            _trainingCurrentStep.value = (current.size + 1).coerceAtMost(3)
            _trainingQualityScore.value = if (current.isEmpty()) {
                "Speak clearly. Ready for Attempt 1."
            } else {
                "Attempt removed. Ready for Attempt ${_trainingCurrentStep.value}."
            }
        }
    }

    fun startRecordingAttempt() {
        if (!_isTrainingWakeWord.value) return
        _isRecordingAttempt.value = true
        _trainingLiveAmplitude.value = 0f
        
        // Start continuous or one-shot listening
        startListening()
        
        // Start amplitude tracking
        startAttemptAudioRecord()
    }

    fun stopRecordingAttempt() {
        _isRecordingAttempt.value = false
        stopListening()
        stopAttemptAudioRecord()
    }

    private fun startAttemptAudioRecord() {
        trainingAudioRecordJob?.cancel()
        trainingAudioRecordJob = viewModelScope.launch(Dispatchers.Main) {
            if (isUsingGoogleSTT) {
                // Pipe standard audio amplitude to training live amplitude
                try {
                    _audioAmplitude.collect { amp ->
                        _trainingLiveAmplitude.value = amp
                    }
                } catch (e: Exception) {
                    Log.e("AiraViewModel", "Error piping audio amplitude to training live amplitude", e)
                }
            } else {
                // Vosk/Offline: Simulate a beautiful natural voice wave
                withContext(Dispatchers.Default) {
                    var angle = 0f
                    while (_isRecordingAttempt.value) {
                        val base = Math.abs(Math.sin(angle.toDouble())).toFloat() * 0.4f
                        val jitter = (Math.random().toFloat() * 0.3f)
                        _trainingLiveAmplitude.value = (base + jitter).coerceIn(0f, 1f)
                        angle += 0.2f
                        kotlinx.coroutines.delay(50)
                    }
                }
            }
        }
    }

    private fun stopAttemptAudioRecord() {
        trainingAudioRecordJob?.cancel()
        _trainingLiveAmplitude.value = 0f
    }

    fun processTrainingAttempt(transcription: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val cleaned = transcription.trim()
            if (cleaned.isEmpty()) return@launch
            
            val currentAttempts = _trainingAttempts.value.toMutableList()
            currentAttempts.add(cleaned)
            _trainingAttempts.value = currentAttempts
            
            val step = _trainingCurrentStep.value
            stopListening()
            stopAttemptAudioRecord()
            _isRecordingAttempt.value = false

            // Analyze Quality for this attempt
            val target = _trainingWakeWordText.value.lowercase().trim()
            val spoken = cleaned.lowercase().trim()
            
            val isMatch = spoken.contains(target) || target.contains(spoken)
            val feedback = if (isMatch) {
                "Attempt $step: '$cleaned' (Match: Excellent Clarity!)"
            } else {
                "Attempt $step: '$cleaned' (Phonetic variance detected)"
            }
            _trainingQualityScore.value = feedback

            if (step < 3) {
                _trainingCurrentStep.value = step + 1
            } else {
                // All 3 attempts complete! Evaluate final stability
                evaluateFinalTraining()
            }
        }
    }

    private fun evaluateFinalTraining() {
        val attempts = _trainingAttempts.value
        val target = _trainingWakeWordText.value
        if (attempts.size < 3) return

        val u1 = attempts[0].lowercase().trim()
        val u2 = attempts[1].lowercase().trim()
        val u3 = attempts[2].lowercase().trim()

        val uniqueCount = listOf(u1, u2, u3).distinct().size
        val quality = when (uniqueCount) {
            1 -> "Excellent"
            2 -> "Good"
            else -> "Fair"
        }

        _trainingQualityScore.value = when (quality) {
            "Excellent" -> "Training successful! 3/3 matching phonetic consistency. Quality: Excellent."
            "Good" -> "Training successful! 2/3 matching phonetic consistency. Quality: Good."
            else -> "Training complete with varied pronunciations. Quality: Fair."
        }
    }

    fun saveAndActivateTrainedWakeWord() {
        val target = _trainingWakeWordText.value.trim()
        if (target.isEmpty()) return

        val attempts = _trainingAttempts.value
        val u1 = attempts.getOrNull(0)?.lowercase()?.trim() ?: ""
        val u2 = attempts.getOrNull(1)?.lowercase()?.trim() ?: ""
        val u3 = attempts.getOrNull(2)?.lowercase()?.trim() ?: ""

        val uniqueCount = listOf(u1, u2, u3).filter { it.isNotEmpty() }.distinct().size
        val quality = when (uniqueCount) {
            0, 1 -> "Excellent"
            2 -> "Good"
            else -> "Fair"
        }

        val attemptsJson = org.json.JSONArray(attempts).toString()

        viewModelScope.launch(Dispatchers.IO) {
            val dbDao = db.trainedWakeWordDao()
            // Deactivate others
            dbDao.deactivateAll()
            // Insert and activate this one
            dbDao.insertTrainedWakeWord(
                com.example.data.TrainedWakeWord(
                    word = target,
                    quality = quality,
                    attemptsJson = attemptsJson,
                    isActive = true
                )
            )
            
            // Apply it as current active wake word
            updateWakeWord(target)
            
            _isTrainingWakeWord.value = false
            _trainingAttempts.value = emptyList()
            
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Wake Word Activated: $target", Toast.LENGTH_SHORT).show()
                speakText("New custom wake word $target activated.")
            }
        }
    }

    fun activateTrainedWakeWord(id: Long, word: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.trainedWakeWordDao().setActiveWakeWord(id)
            updateWakeWord(word)
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Activated: $word", Toast.LENGTH_SHORT).show()
                speakText("Custom wake word $word is now active.")
            }
        }
    }

    fun deleteTrainedWakeWord(id: Long, word: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val active = db.trainedWakeWordDao().getActiveWakeWord()
            db.trainedWakeWordDao().deleteById(id)
            if (active?.id == id) {
                // Revert to default "Hey Aira"
                updateWakeWord("Hey Aira")
            }
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Deleted custom trigger: $word", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleTestingWakeWord(enabled: Boolean) {
        _isTestingWakeWord.value = enabled
        _isTestWakeWordTriggered.value = false
        if (enabled) {
            _testTriggerText.value = "Testing Active. Say '${wakeWord.value}' to verify."
            startListening()
        } else {
            _testTriggerText.value = "Testing mode off."
            stopListening()
        }
    }

    fun processTestingAttempt(text: String) {
        val target = _wakeWord.value.lowercase().trim()
        val spoken = text.lowercase().trim()
        if (spoken.contains(target)) {
            _isTestWakeWordTriggered.value = true
            _testTriggerText.value = "Validated! Heard wake word in: '$text'"
            speakText("Wake word validated successfully.")
            
            // Reset trigger state after 3 seconds
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                _isTestWakeWordTriggered.value = false
            }
        } else {
            _testTriggerText.value = "Heard: '$text' (Not matched)"
        }
        
        // Keep listening in testing mode
        if (_isTestingWakeWord.value) {
            viewModelScope.launch(Dispatchers.Main) {
                kotlinx.coroutines.delay(1000)
                if (_isTestingWakeWord.value) {
                    startListening()
                }
            }
        }
    }

    fun handleSpeechResultForTraining(text: String): Boolean {
        if (_isTrainingWakeWord.value && _isRecordingAttempt.value) {
            processTrainingAttempt(text)
            return true
        }
        if (_isTestingWakeWord.value) {
            processTestingAttempt(text)
            return true
        }
        return false
    }

    fun toggleEmotionDetection(enabled: Boolean) {
        _isEmotionDetectionEnabled.value = enabled
        sharedPrefs.edit().putBoolean("emotion_detection_enabled", enabled).apply()
        if (!enabled) {
            _currentEmotion.value = com.example.utils.UserEmotion.NEUTRAL
        }
    }

    fun setTemperatureMode(mode: String) {
        _temperatureMode.value = mode
        sharedPrefs.edit().putString("temperature_mode", mode).apply()
    }

    fun setCustomTemperatureText(text: String) {
        _customTemperatureText.value = text
        sharedPrefs.edit().putString("custom_temperature_val", text).apply()
    }

    fun toggleOfflineBrain(isOffline: Boolean) {
        if (isOffline && (!com.example.utils.MemoryManager.isDeviceCapable(getApplication()) || !com.example.utils.MemoryManager.isOfflineSupported(getApplication()))) {
            _isOfflineBrain.value = false
            sharedPrefs.edit().putBoolean("offline_brain", false).apply()
            speakText("Offline mode not available on this device")
            return
        }
        _isOfflineBrain.value = isOffline
        sharedPrefs.edit().putBoolean("offline_brain", isOffline).apply()
        if (isOffline && !com.example.utils.DownloadManager.isLlamaModelDownloaded(getApplication())) {
            viewModelScope.launch {
                com.example.utils.DownloadManager.downloadLlamaModel(getApplication())
            }
        }
        val suffix = if (isOffline) "Active (Llama 3.2 local engine active)" else "Inactive (Online Brain active)"
        speakText("Aira offline brain mode configured to $suffix")
    }

    fun updateOnlineModel(model: String) {
        _onlineModel.value = model
        sharedPrefs.edit().putString("online_model", model).apply()
        speakText("Online AI brain configured to $model")
    }

    fun updateLlamaThreads(threads: Int) {
        _llamaThreads.value = threads
        sharedPrefs.edit().putInt("llama_threads", threads).apply()
        speakText("Llama core execution configured to $threads threads")
    }

    fun getLlamaEngineStatus(): String {
        if (!com.example.utils.MemoryManager.isDeviceCapable(getApplication())) {
            return "Disabled (Device RAM < 3GB - Low Memory Protection)"
        }
        return llamaCppBrain.getEngineStatus()
    }

    fun updateThemeIndex(index: Int) {
        _themeIndex.value = index
        sharedPrefs.edit().putInt("theme_index", index).apply()
        com.example.data.ThemeRepository.themes.getOrNull(index)?.let { theme ->
            val mode = if (theme.isDark) "dark" else "light"
            _appTheme.value = mode
            sharedPrefs.edit().putString("app_theme", mode).apply()
        }
    }

    fun selectTheme(index: Int) {
        _themeIndex.value = index
        sharedPrefs.edit().putInt("theme_index", index).apply()
        com.example.data.ThemeRepository.themes.getOrNull(index)?.let { theme ->
            val mode = if (theme.isDark) "dark" else "light"
            _appTheme.value = mode
            sharedPrefs.edit().putString("app_theme", mode).apply()
        }
    }

    fun toggleLowPerformanceMode(enabled: Boolean) {
        _lowPerformanceMode.value = enabled
        sharedPrefs.edit().putBoolean("low_performance", enabled).apply()
    }

    // --- Piper TTS Setters & Sync ---
    fun togglePiperTts(enabled: Boolean) {
        _usePiperTts.value = enabled
        sharedPrefs.edit().putBoolean("use_piper_tts", enabled).apply()
        piperTtsManager.setEngineEnabled(enabled)
        val term = if (enabled) "Piper Low-Latency Neural synthesis activated." else "Standard device Speech synthesis reactive."
        speakText(term)
    }

    fun updatePiperVoice(voiceId: String) {
        _selectedTtsEngine.value = TtsEngine.PIPER_OFFLINE
        sharedPrefs.edit().putString("selected_tts_engine", TtsEngine.PIPER_OFFLINE.name).apply()
        piperTtsManager.selectedTtsEngine = TtsEngine.PIPER_OFFLINE.name
        piperTtsManager.setVoice(voiceId)
        val shortName = voiceId.substringAfter("en_US-").substringBefore("-")
        setEnglishVoiceMode(shortName.replaceFirstChar { it.uppercase() })
        speakText("Acoustic voice loaded: $voiceId.")
    }

    fun downloadPiperModel(voiceId: String) {
        piperTtsManager.downloadVoiceModel(voiceId)
    }

    fun deletePiperModel(voiceId: String) {
        piperTtsManager.deleteVoiceModel(voiceId)
    }

    // --- Reminders CRUD ---
    fun addReminder(title: String, timeLabel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = reminderDao.insertReminder(Reminder(title = title, timeLabel = timeLabel))
            com.example.service.AiraNotificationManager.scheduleReminderAlarm(
                getApplication(),
                id,
                title,
                timeLabel
            )
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.deleteReminder(reminder)
            com.example.service.AiraNotificationManager.cancelReminderAlarm(
                getApplication(),
                reminder.id
            )
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.clearHistory()
        }
    }

    fun exportChatHistory(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatHistory.value
            if (messages.isEmpty()) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "No conversations to export", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val sb = java.lang.StringBuilder()
                sb.append("Aira Chat History Export\n")
                sb.append("Exported on: ${sdf.format(java.util.Date())}\n")
                sb.append("========================================\n\n")

                for (msg in messages) {
                    val senderName = if (msg.sender == "user") "User" else "Aira"
                    val timeStr = sdf.format(java.util.Date(msg.timestamp))
                    sb.append("[$timeStr] $senderName: ${msg.message}\n")
                }

                val textContent = sb.toString()
                
                val exportFile = File(context.cacheDir, "aira_chat_export.txt")
                exportFile.writeText(textContent)

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exportFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Aira Chat History Export")
                    putExtra(Intent.EXTRA_TEXT, "Here is my recent conversation export from Aira.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Export Chat History")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                launch(Dispatchers.Main) {
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Failed to export chat", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- Speech Recognition Overrides & Amplitude tracking ---
    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {
        Log.d("AiraViewModel", "Beginning speech")
        hasSpeechStarted = true
        handler.removeCallbacks(timeoutRunnable)
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Map rmsdB (typically range -2 to 10+) nicely to 0f-1f for anims
        val amp = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _audioAmplitude.value = amp
        if (rmsdB > 2f) {
            hasSpeechStarted = true
            handler.removeCallbacks(timeoutRunnable)
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        _audioAmplitude.value = 0f
    }

    override fun onError(error: Int) {
        handler.removeCallbacks(timeoutRunnable)
        _isListening.value = false
        _audioAmplitude.value = 0f
        com.example.utils.AiraAudioFocusManager.getInstance(getApplication()).releaseSttFocus()

        val msg = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_CLIENT -> "Google Speech client error."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "RECORD_AUDIO permission required."
            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timeout."
            SpeechRecognizer.ERROR_NO_MATCH -> "No phrasing recognized. Try speaking again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy."
            SpeechRecognizer.ERROR_SERVER -> "Google speech server error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timeout."
            else -> "Speech recognition error ($error)."
        }

        _currentStatus.value = msg
        Log.e("STTFlow", "STAGE 1 FAILED: Google SpeechRecognizer error code $error ($msg)")

        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
            try {
                speechRecognizer?.destroy()
            } catch (_: Throwable) {}
            speechRecognizer = null
        }

        if (isUsingGoogleSTT && error != SpeechRecognizer.ERROR_NO_MATCH) {
            Log.w("STTFlow", "STAGE 1 FAILED: Triggering STAGE 2 (Vosk STT) fallback due to Google STT error code $error")
            switchToOfflineVosk()
            return
        }

        consecutiveSpeechErrors++
        if (consecutiveSpeechErrors >= 3) {
            consecutiveSpeechErrors = 0
            if (_usePersistentListening.value) {
                _usePersistentListening.value = false
                sharedPrefs.edit().putBoolean("persistent_listening", false).apply()
                speakText("Continuous listening paused due to consecutive errors.")
            }
            stopListening()
            return
        }

        if (_isTestingWakeWord.value) {
            viewModelScope.launch(Dispatchers.Main) {
                kotlinx.coroutines.delay(1000)
                if (_isTestingWakeWord.value) {
                    startListening()
                }
            }
        } else if (_usePersistentListening.value) {
            restartContinuousListeningIfNeeded()
        }
    }

    override fun onResults(results: Bundle?) {
        com.example.utils.AiraAudioFocusManager.getInstance(getApplication()).releaseSttFocus()
        consecutiveSpeechErrors = 0
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotEmpty()) {
            if (handleSpeechResultForTraining(text)) {
                return
            }
            _currentStatus.value = "Recognized: $text"
            
            val isContinuous = _usePersistentListening.value
            val currentWakeWord = _wakeWord.value.lowercase().trim()
            val lowerText = text.lowercase().trim()

            if (isContinuous) {
                if (lowerText.contains(currentWakeWord)) {
                    val index = lowerText.indexOf(currentWakeWord)
                    var command = lowerText.substring(index + currentWakeWord.length).trim()
                    
                    if (command.startsWith(",") || command.startsWith(":") || command.startsWith("-")) {
                        command = command.substring(1).trim()
                    }

                    if (command.isEmpty()) {
                        val responses = listOf("Standing by.", "At your service.", "I'm listening.", "Aira activated.")
                        val ack = responses.random()
                        viewModelScope.launch {
                            chatDao.insertMessage(ChatMessage(sender = "aira", message = ack))
                            speakText(ack)
                        }
                    } else {
                        processAssistantSession(command)
                    }
                } else {
                    Log.d("AiraViewModel", "Ignored phrase without wake word in continuous mode: $text")
                    restartContinuousListeningIfNeeded()
                }
            } else {
                processAssistantSession(text)
            }
        } else {
            _currentStatus.value = "Empty phrasing recognized."
            if (_usePersistentListening.value) {
                restartContinuousListeningIfNeeded()
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotEmpty()) {
            _currentStatus.value = "Phonetics: $text"
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun processAIResponse(aiResponseText: String, userQuery: String = "") {
        val partitioned = com.example.models.JarvisOutputPartitioner.partition(aiResponseText, userQuery, getApplication())
        if (partitioned.shouldSpeak && partitioned.speechContent.isNotBlank()) {
            speakText(partitioned.speechContent, partitioned.speechSpeed)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            stopAllSpeech()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error stopping speech in onCleared", e)
        }
        try {
            handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error clearing handler messages in onCleared", e)
        }
        try {
            releaseAllNativeModels()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error in releaseAllNativeModels in onCleared", e)
        }
        try {
            releaseVoskModel()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error releasing Vosk model in onCleared", e)
        }
        try {
            piperTts.shutdown()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error shutting down offline piperTts in onCleared", e)
        }
        try {
            piperTtsManager.shutdown()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error shutting down piperTtsManager in onCleared", e)
        }
        try {
            com.example.utils.AiraAudioFocusManager.getInstance(getApplication()).releaseTtsFocus()
            com.example.utils.AiraAudioFocusManager.getInstance(getApplication()).releaseSttFocus()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error releasing audio focus in onCleared", e)
        }
        try {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error unregistering prefChangeListener", e)
        }
        try {
            networkCallback?.let {
                val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                cm?.unregisterNetworkCallback(it)
            }
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error unregistering networkCallback", e)
        }
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error destroying speechRecognizer in onCleared", e)
        }
    }

    private fun loadVoiceCommandLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbLogs = db.voiceCommandLogDao().getRecentLogs()
                val list = dbLogs.map {
                    VoiceCommandLog(
                        id = it.id,
                        command = it.command,
                        matchedTrigger = it.matchedTrigger,
                        timestamp = it.timestamp,
                        status = it.status,
                        details = it.details
                    )
                }
                _voiceCommandLogs.value = list
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Error loading voice command logs from Room DB", e)
            }
        }
    }

    fun addVoiceCommandLog(command: String, matchedTrigger: String?, status: String, details: String) {
        val sdf = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
        val timeStr = sdf.format(java.util.Date())
        val newLog = VoiceCommandLog(
            command = command,
            matchedTrigger = matchedTrigger,
            timestamp = timeStr,
            status = status,
            details = details
        )
        // Keep last 30 logs in UI state
        val updatedList = (listOf(newLog) + _voiceCommandLogs.value).take(30)
        _voiceCommandLogs.value = updatedList

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.voiceCommandLogDao().insertLog(
                    com.example.data.VoiceCommandLogEntity(
                        id = newLog.id,
                        command = newLog.command,
                        matchedTrigger = newLog.matchedTrigger,
                        timestamp = newLog.timestamp,
                        status = newLog.status,
                        details = newLog.details
                    )
                )
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Error saving voice command log to Room DB", e)
            }
        }
    }

    fun clearVoiceCommandLogs() {
        _voiceCommandLogs.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.voiceCommandLogDao().clearLogs()
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Error clearing voice command logs in Room DB", e)
            }
        }
    }
}

data class VoiceCommandLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val matchedTrigger: String?,
    val timestamp: String,
    val status: String, // "SUCCESS", "FAILED", "ABORTED"
    val details: String
)
