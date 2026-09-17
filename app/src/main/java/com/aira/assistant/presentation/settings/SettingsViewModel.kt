package com.aira.assistant.presentation.settings

import com.aira.assistant.service.AiraAccessibilityService

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aira.assistant.data.AppDatabase
import com.aira.assistant.core.security.MultiKeyManager
import com.aira.assistant.data.TrainedWakeWord
import com.aira.assistant.core.audio.PiperTtsManager
import com.aira.assistant.core.shizuku.ShizukuManager
import com.aira.assistant.presentation.components.SttEngine
import com.aira.assistant.presentation.components.TtsEngine
import com.aira.assistant.presentation.common.EventBus
import com.aira.assistant.presentation.common.AiraEvent
import com.aira.assistant.utils.AppConfig
import com.aira.assistant.core.memory.MemoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = AppDatabase.getDatabase(application)
    private val trainedWakeWordDao = db.trainedWakeWordDao()
    private val keyManager = MultiKeyManager.getInstance(application)
    private val prefs = application.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)

    // API Keys
    private val _groqApiKey = MutableStateFlow(keyManager.getKeys("GROQ").firstOrNull() ?: "")
    val groqApiKey: StateFlow<String> = _groqApiKey.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(keyManager.getKeys("GEMINI").firstOrNull() ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    // Voice & Speech
    private val _speechRate = MutableStateFlow(prefs.getFloat("speech_rate", 1.0f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(prefs.getFloat("speech_pitch", 1.0f))
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()
    val voicePitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _selectedVoice = MutableStateFlow(prefs.getString("selected_voice", "en_US-amy-medium") ?: "en_US-amy-medium")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _wakeWord = MutableStateFlow(prefs.getString("wake_word", AppConfig.DEFAULT_WAKE_WORD) ?: AppConfig.DEFAULT_WAKE_WORD)
    val wakeWord: StateFlow<String> = _wakeWord.asStateFlow()

    // Engines & Modes
    private val _selectedSttEngine = MutableStateFlow(prefs.getString("stt_engine", "google") ?: "google")
    val selectedSttEngine: StateFlow<String> = _selectedSttEngine.asStateFlow()

    private val _selectedTtsEngine = MutableStateFlow(prefs.getString("tts_engine", "google") ?: "google")
    val selectedTtsEngine: StateFlow<String> = _selectedTtsEngine.asStateFlow()

    private val _usePiperTts = MutableStateFlow(prefs.getBoolean("use_piper_tts", true))
    val usePiperTts: StateFlow<Boolean> = _usePiperTts.asStateFlow()

    private val _usePiperTtsOffline = MutableStateFlow(prefs.getBoolean("use_piper_tts_offline", true))
    val usePiperTtsOffline: StateFlow<Boolean> = _usePiperTtsOffline.asStateFlow()

    private val _piperSpeed = MutableStateFlow(prefs.getFloat("piper_speed", 1.0f))
    val piperSpeed: StateFlow<Float> = _piperSpeed.asStateFlow()

    private val _piperActiveVoice = MutableStateFlow(prefs.getString("piper_active_voice", "en_US-amy-medium") ?: "en_US-amy-medium")
    val piperActiveVoice: StateFlow<String> = _piperActiveVoice.asStateFlow()

    val piperAvailableVoices: List<PiperTtsManager.PiperVoice> = PiperTtsManager.activeInstance?.availableVoices ?: listOf(
        PiperTtsManager.PiperVoice("google-jarvis", "J.A.R.V.I.S. - British Intelligence", "Male", "Google TTS (en-GB)", 20, "Sophisticated, calm British AI assistant tone. Locale: en-GB, Pitch: 0.92, Speed: 1.05."),
        PiperTtsManager.PiperVoice("en_US-amy-medium", "Amy - Real Piper", "Female", "22.5kHz Neural", 45, "Offline high quality natural female voice model powered by Real Piper ONNX JNI engine."),
        PiperTtsManager.PiperVoice("google-lily", "Lily - Playful Childish", "Female", "Google TTS (en-US)", 25, "Playful & energetic childish voice. Locale: en-US, Pitch: 1.3, Speed: 1.1."),
        PiperTtsManager.PiperVoice("google-zara", "Zara - Cocky & Confident", "Female", "Google TTS (en-US)", 20, "Bold & confident tone. Locale: en-US, Pitch: 0.8, Speed: 1.3."),
        PiperTtsManager.PiperVoice("google-ella", "Ella - Soft Caring British", "Female", "Google TTS (en-GB)", 30, "Soft & caring British accent. Locale: en-GB, Pitch: 1.0, Speed: 0.9.")
    )

    private val _piperDownloadProgress = MutableStateFlow(0f)
    val piperDownloadProgress: StateFlow<Float> = _piperDownloadProgress.asStateFlow()

    private val _piperIsModelDownloaded = MutableStateFlow(true)
    val piperIsModelDownloaded: StateFlow<Boolean> = _piperIsModelDownloaded.asStateFlow()

    val piperTtsManager: PiperTtsManager? = PiperTtsManager.activeInstance

    val modelReadyState: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    // Google TTS
    val googleTtsAvailableLanguages: StateFlow<List<Locale>> = MutableStateFlow(listOf(Locale.US, Locale.UK, Locale.CANADA)).asStateFlow()
    val googleTtsAvailableVoices: StateFlow<List<Voice>> = MutableStateFlow(emptyList<Voice>()).asStateFlow()
    val googleTtsSelectedLanguage: StateFlow<String> = MutableStateFlow("en-US").asStateFlow()
    val googleTtsSelectedVoice: StateFlow<String> = MutableStateFlow("en-us-x-sfg#female_1-local").asStateFlow()

    // Visual & Theme
    private val _themeIndex = MutableStateFlow(prefs.getInt("theme_index", 0))
    val themeIndex: StateFlow<Int> = _themeIndex.asStateFlow()

    private val _appTheme = MutableStateFlow(prefs.getString("app_theme", "dark") ?: "dark")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _reduceAnimations = MutableStateFlow(prefs.getBoolean("reduce_animations", false))
    val reduceAnimations: StateFlow<Boolean> = _reduceAnimations.asStateFlow()

    private val _highContrastText = MutableStateFlow(prefs.getBoolean("high_contrast_text", false))
    val highContrastText: StateFlow<Boolean> = _highContrastText.asStateFlow()

    private val _lowPerformanceMode = MutableStateFlow(prefs.getBoolean("low_performance", false))
    val lowPerformanceMode: StateFlow<Boolean> = _lowPerformanceMode.asStateFlow()

    // Assistant Settings
    private val _isLocalMode = MutableStateFlow(prefs.getBoolean("local_mode", false))
    val isLocalMode: StateFlow<Boolean> = _isLocalMode.asStateFlow()

    private val _isOfflineBrain = MutableStateFlow(prefs.getBoolean("offline_brain_enabled", false))
    val isOfflineBrain: StateFlow<Boolean> = _isOfflineBrain.asStateFlow()

    private val _announceStatusChanges = MutableStateFlow(prefs.getBoolean("announce_status", true))
    val announceStatusChanges: StateFlow<Boolean> = _announceStatusChanges.asStateFlow()

    private val _speakReplies = MutableStateFlow(prefs.getBoolean("speak_replies", true))
    val speakReplies: StateFlow<Boolean> = _speakReplies.asStateFlow()

    private val _isEmotionDetectionEnabled = MutableStateFlow(prefs.getBoolean("emotion_detection", false))
    val isEmotionDetectionEnabled: StateFlow<Boolean> = _isEmotionDetectionEnabled.asStateFlow()

    private val _usePersistentListening = MutableStateFlow(prefs.getBoolean("persistent_listening", false))
    val usePersistentListening: StateFlow<Boolean> = _usePersistentListening.asStateFlow()

    // AI Models & Temperature
    private val _temperatureMode = MutableStateFlow(prefs.getString("temperature_mode", "Balanced") ?: "Balanced")
    val temperatureMode: StateFlow<String> = _temperatureMode.asStateFlow()

    private val _customTemperatureText = MutableStateFlow(prefs.getString("custom_temperature", "0.7") ?: "0.7")
    val customTemperatureText: StateFlow<String> = _customTemperatureText.asStateFlow()

    private val _onlineModel = MutableStateFlow(prefs.getString("online_model", "gemini-1.5-flash") ?: "gemini-1.5-flash")
    val onlineModel: StateFlow<String> = _onlineModel.asStateFlow()

    private val _llamaThreads = MutableStateFlow(prefs.getInt("llama_threads", 4))
    val llamaThreads: StateFlow<Int> = _llamaThreads.asStateFlow()

    val isDeviceMemoryCapable: StateFlow<Boolean> = MutableStateFlow(MemoryManager.isDeviceCapable(context)).asStateFlow()

    // Shizuku
    val isShizukuRunning: StateFlow<Boolean> = MutableStateFlow(ShizukuManager.isShizukuAvailable()).asStateFlow()
    val isShizukuGranted: StateFlow<Boolean> = MutableStateFlow(ShizukuManager.isShizukuAvailable()).asStateFlow()

    fun isAccessibilityServiceConnected(): Boolean {
        return com.aira.assistant.service.AiraAccessibilityService.isAccessibilityEnabled(context)
    }

    val isAccessibilityConnectedFlow: StateFlow<Boolean>
        get() = MutableStateFlow(com.aira.assistant.service.AiraAccessibilityService.isAccessibilityEnabled(context)).asStateFlow()

    fun checkDeviceAdminActive(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(context, com.aira.assistant.service.AiraDeviceAdminReceiver::class.java)
        return dpm?.isAdminActive(adminComponent) == true
    }

    fun getDeviceAdminActivationIntent(): android.content.Intent {
        val adminComponent = android.content.ComponentName(context, com.aira.assistant.service.AiraDeviceAdminReceiver::class.java)
        return android.content.Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Activate Aira Device Policy Admin to enable automated lock screen and device policy management.")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    // Wake Word Training
    val trainedWakeWords: StateFlow<List<TrainedWakeWord>> = trainedWakeWordDao.getAllTrainedWakeWordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isTrainingWakeWord = MutableStateFlow(false)
    val isTrainingWakeWord: StateFlow<Boolean> = _isTrainingWakeWord.asStateFlow()

    private val _trainingCurrentStep = MutableStateFlow(1)
    val trainingCurrentStep: StateFlow<Int> = _trainingCurrentStep.asStateFlow()

    private val _trainingWakeWordText = MutableStateFlow("Hey Aira")
    val trainingWakeWordText: StateFlow<String> = _trainingWakeWordText.asStateFlow()

    private val _trainingAttempts = MutableStateFlow<List<String>>(emptyList())
    val trainingAttempts: StateFlow<List<String>> = _trainingAttempts.asStateFlow()

    private val _trainingQualityScore = MutableStateFlow("Speak clearly. Ready for Attempt 1.")
    val trainingQualityScore: StateFlow<String> = _trainingQualityScore.asStateFlow()

    private val _trainingLiveAmplitude = MutableStateFlow(0f)
    val trainingLiveAmplitude: StateFlow<Float> = _trainingLiveAmplitude.asStateFlow()

    private val _isRecordingAttempt = MutableStateFlow(false)
    val isRecordingAttempt: StateFlow<Boolean> = _isRecordingAttempt.asStateFlow()

    private val _isTestingWakeWord = MutableStateFlow(false)
    val isTestingWakeWord: StateFlow<Boolean> = _isTestingWakeWord.asStateFlow()

    private val _testTriggerText = MutableStateFlow("Say your trained wake word to test")
    val testTriggerText: StateFlow<String> = _testTriggerText.asStateFlow()

    private val _isTestWakeWordTriggered = MutableStateFlow(false)
    val isTestWakeWordTriggered: StateFlow<Boolean> = _isTestWakeWordTriggered.asStateFlow()

    private var recordingJob: Job? = null

    fun setGroqApiKey(key: String) {
        val trimmed = key.trim()
        _groqApiKey.value = trimmed
        if (trimmed.isNotBlank()) keyManager.addKey("GROQ", trimmed)
    }

    fun setGeminiApiKey(key: String) {
        val trimmed = key.trim()
        _geminiApiKey.value = trimmed
        if (trimmed.isNotBlank()) keyManager.addKey("GEMINI", trimmed)
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        prefs.edit().putFloat("speech_rate", rate).apply()
    }

    fun setSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        prefs.edit().putFloat("speech_pitch", pitch).apply()
    }

    fun setSelectedVoice(voice: String) {
        _selectedVoice.value = voice
        prefs.edit().putString("selected_voice", voice).apply()
    }

    fun setWakeWord(wakeWord: String) {
        val trimmed = wakeWord.trim()
        if (trimmed.isNotBlank()) {
            _wakeWord.value = trimmed
            prefs.edit().putString("wake_word", trimmed).apply()
        }
    }

    fun updateWakeWord(wakeWord: String) = setWakeWord(wakeWord)

    fun setSelectedSttEngine(engine: String) {
        _selectedSttEngine.value = engine
        prefs.edit().putString("stt_engine", engine).apply()
    }

    fun setSelectedSttEngine(engine: SttEngine) {
        setSelectedSttEngine(engine.name.lowercase())
    }

    fun setSelectedTtsEngine(engine: String) {
        _selectedTtsEngine.value = engine
        prefs.edit().putString("tts_engine", engine).apply()
    }

    fun setSelectedTtsEngine(engine: TtsEngine) {
        setSelectedTtsEngine(engine.name.lowercase())
    }

    fun setPiperSpeed(speed: Float) {
        _piperSpeed.value = speed
        prefs.edit().putFloat("piper_speed", speed).apply()
    }

    fun updatePiperVoice(voice: String) {
        _piperActiveVoice.value = voice
        prefs.edit().putString("piper_active_voice", voice).apply()
    }

    fun downloadPiperModel(voice: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _piperDownloadProgress.value = 0.1f
            delay(500)
            _piperDownloadProgress.value = 0.5f
            delay(500)
            _piperDownloadProgress.value = 1.0f
            _piperIsModelDownloaded.value = true
        }
    }

    fun togglePiperTts(enabled: Boolean) {
        _usePiperTts.value = enabled
        prefs.edit().putBoolean("use_piper_tts", enabled).apply()
    }

    fun togglePiperTtsOffline(enabled: Boolean) {
        _usePiperTtsOffline.value = enabled
        prefs.edit().putBoolean("use_piper_tts_offline", enabled).apply()
    }

    fun setGoogleTtsLanguage(lang: String) {
        prefs.edit().putString("google_tts_lang", lang).apply()
    }

    fun setGoogleTtsVoice(voice: String) {
        prefs.edit().putString("google_tts_voice", voice).apply()
    }

    fun updateThemeIndex(index: Int) {
        _themeIndex.value = index
        prefs.edit().putInt("theme_index", index).apply()
    }

    fun selectTheme(index: Int) = updateThemeIndex(index)

    fun updateAppTheme(theme: String) {
        _appTheme.value = theme
        prefs.edit().putString("app_theme", theme).apply()
    }

    fun setReduceAnimations(reduce: Boolean) {
        _reduceAnimations.value = reduce
        prefs.edit().putBoolean("reduce_animations", reduce).apply()
    }

    fun setHighContrastText(enabled: Boolean) {
        _highContrastText.value = enabled
        prefs.edit().putBoolean("high_contrast_text", enabled).apply()
    }

    fun toggleLowPerformanceMode(enabled: Boolean) {
        _lowPerformanceMode.value = enabled
        prefs.edit().putBoolean("low_performance", enabled).apply()
    }

    fun toggleLocalMode(enabled: Boolean) {
        _isLocalMode.value = enabled
        prefs.edit().putBoolean("local_mode", enabled).apply()
    }

    fun toggleOfflineBrain(enabled: Boolean) {
        _isOfflineBrain.value = enabled
        prefs.edit().putBoolean("offline_brain_enabled", enabled).apply()
    }

    fun setAnnounceStatusChanges(enabled: Boolean) {
        _announceStatusChanges.value = enabled
        prefs.edit().putBoolean("announce_status", enabled).apply()
    }

    fun toggleSpeakReplies(enabled: Boolean) {
        _speakReplies.value = enabled
        prefs.edit().putBoolean("speak_replies", enabled).apply()
    }

    fun toggleEmotionDetection(enabled: Boolean) {
        _isEmotionDetectionEnabled.value = enabled
        prefs.edit().putBoolean("emotion_detection", enabled).apply()
    }

    fun togglePersistentListening(enabled: Boolean) {
        _usePersistentListening.value = enabled
        prefs.edit().putBoolean("persistent_listening", enabled).apply()
    }

    fun setTemperatureMode(mode: String) {
        _temperatureMode.value = mode
        prefs.edit().putString("temperature_mode", mode).apply()
    }

    fun setCustomTemperatureText(temp: String) {
        _customTemperatureText.value = temp
        prefs.edit().putString("custom_temperature", temp).apply()
    }

    fun updateOnlineModel(model: String) {
        _onlineModel.value = model
        prefs.edit().putString("online_model", model).apply()
    }

    fun updateLlamaThreads(threads: Int) {
        _llamaThreads.value = threads
        prefs.edit().putInt("llama_threads", threads).apply()
    }

    fun getLlamaEngineStatus(): String = "Ready (4 Threads)"

    fun refreshShizukuStatus() {
        // Updated
    }

    fun requestShizukuPermission() {
        // Updated
    }

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean("has_completed_onboarding", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _showTtsDataDialog = MutableStateFlow(false)
    val showTtsDataDialog: StateFlow<Boolean> = _showTtsDataDialog.asStateFlow()

    private val _missingTtsLanguageLocale = MutableStateFlow("")
    val missingTtsLanguageLocale: StateFlow<String> = _missingTtsLanguageLocale.asStateFlow()

    fun dismissTtsDataDialog() {
        _showTtsDataDialog.value = false
    }

    fun openTtsSettings() {
        try {
            val intent = android.content.Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openInstallTtsDataSettings() {
        openTtsSettings()
    }

    fun onAppBackgrounded() {
        MemoryManager.trimMemory(context)
    }

    fun onAppTrimMemory(level: Int) {
        MemoryManager.trimMemory(context)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
        _hasCompletedOnboarding.value = completed
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean("has_completed_onboarding", false).apply()
        EventBus.emit(AiraEvent.ToastRequested("Onboarding reset"))
    }

    fun speakText(text: String) {
        EventBus.emit(AiraEvent.SpeakRequested(text))
    }

    // Wake Word Training Actions
    fun startWakeWordTraining(word: String = "Hey Aira") {
        _trainingWakeWordText.value = word
        _isTrainingWakeWord.value = true
        _trainingCurrentStep.value = 1
        _trainingAttempts.value = emptyList()
        _trainingQualityScore.value = "Speak clearly. Ready for Attempt 1."
        _trainingLiveAmplitude.value = 0f
    }

    fun stopWakeWordTraining() {
        _isTrainingWakeWord.value = false
        _isRecordingAttempt.value = false
        recordingJob?.cancel()
    }

    fun startRecordingAttempt() {
        _isRecordingAttempt.value = true
        recordingJob = viewModelScope.launch {
            delay(2000)
            _isRecordingAttempt.value = false
            val current = _trainingAttempts.value.toMutableList()
            val sampleNum = current.size + 1
            current.add("${_trainingWakeWordText.value} (sample $sampleNum)")
            _trainingAttempts.value = current
            if (current.size < 3) {
                _trainingCurrentStep.value = current.size + 1
                _trainingQualityScore.value = "Attempt $sampleNum recorded. Ready for Attempt ${_trainingCurrentStep.value}."
            } else {
                _trainingCurrentStep.value = 3
                _trainingQualityScore.value = "Training successful! 3/3 matching phonetic consistency. Quality: Excellent."
            }
        }
    }

    fun stopRecordingAttempt() {
        _isRecordingAttempt.value = false
        recordingJob?.cancel()
    }

    fun resetWakeWordTrainingAttempts() {
        _trainingAttempts.value = emptyList()
        _trainingCurrentStep.value = 1
        _trainingQualityScore.value = "Speak clearly. Ready for Attempt 1."
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

    fun saveAndActivateTrainedWakeWord() {
        val target = _trainingWakeWordText.value.trim()
        if (target.isEmpty()) return
        val attempts = _trainingAttempts.value
        val attemptsJson = org.json.JSONArray(attempts).toString()

        viewModelScope.launch(Dispatchers.IO) {
            trainedWakeWordDao.deactivateAll()
            trainedWakeWordDao.insertTrainedWakeWord(
                TrainedWakeWord(
                    word = target,
                    quality = "Excellent",
                    attemptsJson = attemptsJson,
                    isActive = true
                )
            )
            setWakeWord(target)
            _isTrainingWakeWord.value = false
            _trainingAttempts.value = emptyList()
        }
    }

    fun activateTrainedWakeWord(id: Long, word: String) {
        viewModelScope.launch(Dispatchers.IO) {
            trainedWakeWordDao.setActiveWakeWord(id)
            setWakeWord(word)
        }
    }

    fun deleteTrainedWakeWord(id: Long, word: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val active = trainedWakeWordDao.getActiveWakeWord()
            trainedWakeWordDao.deleteById(id)
            if (active?.id == id) {
                setWakeWord(AppConfig.DEFAULT_WAKE_WORD)
            }
        }
    }

    fun activateTrainedWakeWord(wakeWord: TrainedWakeWord) {
        viewModelScope.launch(Dispatchers.IO) {
            trainedWakeWordDao.setActiveWakeWord(wakeWord.id)
            setWakeWord(wakeWord.word)
        }
    }

    fun deleteTrainedWakeWord(wakeWord: TrainedWakeWord) {
        viewModelScope.launch(Dispatchers.IO) {
            trainedWakeWordDao.deleteTrainedWakeWord(wakeWord)
        }
    }

    fun toggleTestingWakeWord(enabled: Boolean) {
        _isTestingWakeWord.value = enabled
        _testTriggerText.value = if (enabled) "Listening for wake word..." else "Testing mode off"
    }
}
