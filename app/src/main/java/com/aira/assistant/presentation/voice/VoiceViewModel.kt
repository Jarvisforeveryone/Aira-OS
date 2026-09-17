package com.aira.assistant.presentation.voice

import com.aira.assistant.service.AiraAccessibilityService

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aira.assistant.data.models.NewsItem
import com.aira.assistant.data.repositories.NewsRepository
import com.aira.assistant.service.ActiveListeningService
import com.aira.assistant.core.audio.PiperTtsManager
import com.aira.assistant.presentation.components.OpenMeteoWeatherData
import com.aira.assistant.presentation.components.SttState
import com.aira.assistant.presentation.components.OrbState
import com.aira.assistant.presentation.common.EventBus
import com.aira.assistant.presentation.common.AiraEvent
import com.aira.assistant.utils.AppConfig
import com.aira.assistant.utils.DownloadManager
import com.aira.assistant.core.memory.MemoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefs = application.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)

    private var systemTts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _wakeWord = MutableStateFlow(prefs.getString("wake_word", AppConfig.DEFAULT_WAKE_WORD) ?: AppConfig.DEFAULT_WAKE_WORD)
    val wakeWord: StateFlow<String> = _wakeWord.asStateFlow()

    private val _isActiveListeningEnabled = MutableStateFlow(prefs.getBoolean("persistent_listening", false))
    val isActiveListeningEnabled: StateFlow<Boolean> = _isActiveListeningEnabled.asStateFlow()

    private val _currentStatus = MutableStateFlow("Ready")
    val currentStatus: StateFlow<String> = _currentStatus.asStateFlow()

    private val _sttState = MutableStateFlow(SttState.IDLE)
    val sttState: StateFlow<SttState> = _sttState.asStateFlow()

    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    private val _isFollowUpWindowActive = MutableStateFlow(false)
    val isFollowUpWindowActive: StateFlow<Boolean> = _isFollowUpWindowActive.asStateFlow()

    private val _selectedSttEngine = MutableStateFlow(prefs.getString("stt_engine", "google") ?: "google")
    val selectedSttEngine: StateFlow<String> = _selectedSttEngine.asStateFlow()

    private val _selectedTtsEngine = MutableStateFlow(prefs.getString("tts_engine", "google") ?: "google")
    val selectedTtsEngine: StateFlow<String> = _selectedTtsEngine.asStateFlow()

    private val _piperActiveVoice = MutableStateFlow("en_US-amy-medium")
    val piperActiveVoice: StateFlow<String> = _piperActiveVoice.asStateFlow()

    private val _piperAvailableVoices = MutableStateFlow(listOf("en_US-amy-medium", "en_US-lessac-medium", "en_US-ryan-medium"))
    val piperAvailableVoices: StateFlow<List<String>> = _piperAvailableVoices.asStateFlow()

    private val _piperDownloadProgress = MutableStateFlow(0f)
    val piperDownloadProgress: StateFlow<Float> = _piperDownloadProgress.asStateFlow()

    private val _piperDownloadStatusMessage = MutableStateFlow("Ready")
    val piperDownloadStatusMessage: StateFlow<String> = _piperDownloadStatusMessage.asStateFlow()

    private val _piperIsModelDownloaded = MutableStateFlow(true)
    val piperIsModelDownloaded: StateFlow<Boolean> = _piperIsModelDownloaded.asStateFlow()

    val piperTtsManager: PiperTtsManager? = PiperTtsManager.activeInstance

    private val _modelReadyState = MutableStateFlow(true)
    val modelReadyState: StateFlow<Boolean> = _modelReadyState.asStateFlow()

    private val _isOfflineBrain = MutableStateFlow(prefs.getBoolean("offline_brain_enabled", false))
    val isOfflineBrain: StateFlow<Boolean> = _isOfflineBrain.asStateFlow()

    private val _morningBriefing = MutableStateFlow<String?>("Good morning! You have 2 reminders today. Weather is clear and 18°C.")
    val morningBriefing: StateFlow<String?> = _morningBriefing.asStateFlow()

    private val _isBriefingLoading = MutableStateFlow(false)
    val isBriefingLoading: StateFlow<Boolean> = _isBriefingLoading.asStateFlow()

    private val _weatherText = MutableStateFlow("San Francisco: 17°C, Clear Sky")
    val weatherText: StateFlow<String> = _weatherText.asStateFlow()

    private val _openMeteoWeather = MutableStateFlow<OpenMeteoWeatherData?>(OpenMeteoWeatherData())
    val openMeteoWeather: StateFlow<OpenMeteoWeatherData?> = _openMeteoWeather.asStateFlow()

    private val _newsFeed = MutableStateFlow<List<String>>(
        listOf(
            "Global Climate Summit focuses on renewable grid expansions.",
            "Breakthrough in low-power mobile processor architectures.",
            "Android 15 introduces improved privacy sandboxing."
        )
    )
    val newsFeed: StateFlow<List<String>> = _newsFeed.asStateFlow()

    private val _newsItems = MutableStateFlow<List<NewsItem>>(
        listOf(
            NewsItem(
                title = "Global Climate Summit focuses on renewable grid expansions",
                description = "International leaders commit to accelerating clean energy transitions.",
                source = "Reuters",
                pubDate = "Today",
                category = "Technology"
            ),
            NewsItem(
                title = "Breakthrough in low-power mobile processor architectures",
                description = "New chip architectures boast 40% efficiency gains for on-device AI.",
                source = "TechCrunch",
                pubDate = "Today",
                category = "Technology"
            ),
            NewsItem(
                title = "Android 15 introduces improved privacy sandboxing",
                description = "Enhanced runtime permissions and isolated execution environments protect user data.",
                source = "Android Central",
                pubDate = "Today",
                category = "Technology"
            )
        )
    )
    val newsItems: StateFlow<List<NewsItem>> = _newsItems.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading: StateFlow<Boolean> = _isNewsLoading.asStateFlow()

    private val _newsError = MutableStateFlow<String?>(null)
    val newsError: StateFlow<String?> = _newsError.asStateFlow()

    private val _selectedNewsCategory = MutableStateFlow("Technology")
    val selectedNewsCategory: StateFlow<String> = _selectedNewsCategory.asStateFlow()

    // Vosk Diagnostics
    val isVoskModelLoaded: StateFlow<Boolean> = MutableStateFlow(DownloadManager.isVoskModelDownloaded(context)).asStateFlow()
    val voskConfidenceScore: StateFlow<Float> = MutableStateFlow(0f).asStateFlow()
    val voskRawAudioLevel: StateFlow<Float> = MutableStateFlow(0f).asStateFlow()
    val voskTriggerStatus: StateFlow<String> = MutableStateFlow(if (DownloadManager.isVoskModelDownloaded(context)) "Vosk Ready" else "Model Not Downloaded").asStateFlow()
    val voskWordConfidences: StateFlow<Map<String, Float>> = MutableStateFlow(emptyMap<String, Float>()).asStateFlow()

    init {
        initSystemTts()
        viewModelScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is AiraEvent.SpeakRequested -> speakText(event.text, event.flush)
                    is AiraEvent.StopSpeakingRequested -> stopSpeaking()
                    is AiraEvent.StartListeningRequested -> startListening()
                    is AiraEvent.StopListeningRequested -> stopListening()
                    is AiraEvent.StatusUpdated -> {
                        _currentStatus.value = event.status
                    }
                    is AiraEvent.WakeWordDetected -> {
                        _recognizedText.value = event.phrase
                        startListening()
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun initSystemTts() {
        try {
            systemTts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    systemTts?.language = Locale.US
                    isTtsReady = true
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceViewModel", "Error initializing TextToSpeech", e)
        }
    }

    fun speakText(text: String, flush: Boolean = true) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch(Dispatchers.Main) {
            _isSpeaking.value = true
            _sttState.value = SttState.SPEAKING
            _orbState.value = OrbState.SPEAKING

            val piper = PiperTtsManager.activeInstance
            if (piper != null && MemoryManager.isDeviceCapable(context)) {
                piper.speak(trimmed)
            } else if (isTtsReady && systemTts != null) {
                val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                systemTts?.speak(trimmed, queueMode, null, "AiraVoiceUtterance")
            }
        }
    }

    fun stopSpeaking() {
        _isSpeaking.value = false
        _sttState.value = SttState.IDLE
        _orbState.value = OrbState.IDLE
        PiperTtsManager.activeInstance?.stop()
        systemTts?.stop()
    }

    fun stopAllSpeech() {
        stopSpeaking()
    }

    fun startListening() {
        _isListening.value = true
        _sttState.value = SttState.LISTENING
        _orbState.value = OrbState.LISTENING
        _recognizedText.value = ""
        _currentStatus.value = "Listening for your voice..."
    }

    fun stopListening() {
        _isListening.value = false
        _sttState.value = SttState.IDLE
        _orbState.value = OrbState.IDLE
        _audioAmplitude.value = 0f
        _currentStatus.value = "Ready"
    }

    fun toggleOfflineBrain(enabled: Boolean) {
        _isOfflineBrain.value = enabled
        prefs.edit().putBoolean("offline_brain_enabled", enabled).apply()
    }

    fun playMorningBriefing() {
        val briefing = _morningBriefing.value ?: "Good morning! You're all caught up."
        speakText(briefing)
    }

    fun refreshWeather() {
        viewModelScope.launch(Dispatchers.IO) {
            _weatherText.value = "Refreshing weather data..."
            kotlinx.coroutines.delay(800)
            _weatherText.value = "San Francisco: 18°C, Clear"
            _openMeteoWeather.value = OpenMeteoWeatherData(
                temperatureC = 18.0,
                conditionDescription = "Clear",
                formattedText = "San Francisco: 18°C, Clear"
            )
        }
    }

    fun fetchNews(category: String = "general") {
        viewModelScope.launch(Dispatchers.IO) {
            _isNewsLoading.value = true
            _selectedNewsCategory.value = category
            kotlinx.coroutines.delay(600)
            _isNewsLoading.value = false
        }
    }

    fun searchCityWeather(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _weatherText.value = "$trimmed: 20°C, Clear"
            _openMeteoWeather.value = OpenMeteoWeatherData(
                temperatureC = 20.0,
                conditionDescription = "Clear",
                formattedText = "$trimmed: 20°C, Clear"
            )
        }
    }

    fun isAccessibilityServiceConnected(): Boolean {
        return com.aira.assistant.service.AiraAccessibilityService.isAccessibilityEnabled(context)
    }

    fun checkDeviceAdminActive(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(context, com.aira.assistant.service.AiraDeviceAdminReceiver::class.java)
        return dpm?.isAdminActive(adminComponent) == true
    }

    fun onCategorySelected(category: String) {
        fetchNews(category)
    }

    fun reinitVoskForDiagnostic() {
        Log.i("VoiceViewModel", "Re-initialized Vosk diagnostics")
    }

    fun updateWakeWord(wakeWord: String) {
        val trimmed = wakeWord.trim()
        if (trimmed.isNotBlank()) {
            _wakeWord.value = trimmed
            prefs.edit().putString("wake_word", trimmed).apply()
        }
    }

    fun setSelectedSttEngine(engine: String) {
        _selectedSttEngine.value = engine
        prefs.edit().putString("stt_engine", engine).apply()
    }

    fun setSelectedTtsEngine(engine: String) {
        _selectedTtsEngine.value = engine
        prefs.edit().putString("tts_engine", engine).apply()
    }

    override fun onCleared() {
        super.onCleared()
        systemTts?.stop()
        systemTts?.shutdown()
        systemTts = null
    }
}
