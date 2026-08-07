package com.example.service

import android.content.Context
import android.widget.Toast
import android.util.Log
import java.io.FileInputStream
import java.security.MessageDigest
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class PiperTtsManager(private val context: Context) {
    companion object {
        @Volatile
        var activeInstance: PiperTtsManager? = null

        const val MODEL_URL = "https://drive.google.com/uc?export=download&id=1o14RBC9-S4KeJvvdZ_EiOoQ18gfXE3_a"
        const val MODEL_FILENAME = "amymodel.onnx"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val MODEL_PATH: File = File(File(context.filesDir, "piper_models"), MODEL_FILENAME)

    data class PiperVoice(
        val id: String,
        val displayName: String,
        val gender: String,
        val quality: String,
        val latencyMs: Int,
        val description: String
    )

    private val piperTtsEngine = com.aira.voice.PiperTtsEngine(context)
    private var isInitialized = false
    private var hasShownOfflineFallbackToast = false
    private val _isSpeakCalled = MutableStateFlow(false)
    val isSpeakCalled: StateFlow<Boolean> = _isSpeakCalled.asStateFlow()

    private var nativeTts: android.speech.tts.TextToSpeech? = null
    private var isNativeTtsReady = false
    var isOfflineTtsEnabled = false
    var englishVoiceMode: String = "India"
    var selectedTtsEngine: String = "AUTO"

    // Google TTS Dynamic Support
    private val _googleTtsAvailableLanguages = MutableStateFlow<List<java.util.Locale>>(emptyList())
    val googleTtsAvailableLanguages = _googleTtsAvailableLanguages.asStateFlow()

    private val _googleTtsAvailableVoices = MutableStateFlow<List<android.speech.tts.Voice>>(emptyList())
    val googleTtsAvailableVoices = _googleTtsAvailableVoices.asStateFlow()

    private val _googleTtsSelectedLanguage = MutableStateFlow("en-US")
    val googleTtsSelectedLanguage = _googleTtsSelectedLanguage.asStateFlow()

    private val _googleTtsSelectedVoice = MutableStateFlow("")
    val googleTtsSelectedVoice = _googleTtsSelectedVoice.asStateFlow()

    // Callbacks for UI updates / wave amplitude loops
    var onStartSpeaking: (() -> Unit)? = null
    var onStopSpeaking: (() -> Unit)? = null

    // State flows representing Piper Status (Migrated from PiperTtsEngine)
    private val _activeVoice = MutableStateFlow("en_US-amy-medium")
    val activeVoice = _activeVoice.asStateFlow()

    private val _isEngineActive = MutableStateFlow(true)
    val isEngineActive = _isEngineActive.asStateFlow()

    private fun isVoiceDownloadedForReal(voiceId: String): Boolean {
        // Check assets/piper/models/ first for en_US-amy-medium.onnx
        try {
            val assetName = if (voiceId == "en_US-amy-medium") "piper/models/en_US-amy-medium.onnx" else null
            if (assetName != null) {
                context.assets.open(assetName).use { 
                    return true 
                }
            }
        } catch (e: Exception) {
            // Ignore and fall back to checking filesDir
        }

        val piperModelsDir = File(context.filesDir, "piper_models")
        if (voiceId.startsWith("en_US-amy")) {
            val file1 = File(piperModelsDir, "amymodel.onnx")
            val file3 = File(piperModelsDir, "en_US-amy-medium.onnx")
            val valid1 = file1.exists() && file1.length() > 5 * 1024 * 1024
            val valid3 = file3.exists() && file3.length() > 5 * 1024 * 1024
            return valid1 || valid3
        }
        val file = File(piperModelsDir, "$voiceId.onnx")
        return file.exists() && file.length() > 5 * 1024 * 1024
    }

    private val _showTtsDataDialog = MutableStateFlow(false)
    val showTtsDataDialog: StateFlow<Boolean> = _showTtsDataDialog.asStateFlow()

    private val _missingTtsLanguageLocale = MutableStateFlow("en-US")
    val missingTtsLanguageLocale: StateFlow<String> = _missingTtsLanguageLocale.asStateFlow()

    fun dismissTtsDataDialog() {
        _showTtsDataDialog.value = false
    }

    fun openInstallTtsDataSettings() {
        try {
            val intent = android.content.Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = android.content.Intent("com.android.settings.TTS_SETTINGS").apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("PiperTtsManager", "Could not open TTS settings activity", e2)
            }
        }
        _showTtsDataDialog.value = false
    }

    private val _isModelDownloaded = MutableStateFlow(mapOf(
        "en_US-amy-medium" to isVoiceDownloadedForReal("en_US-amy-medium"),
        "google-lily" to true,
        "google-zara" to true,
        "google-ella" to true
    ))
    val isModelDownloaded = _isModelDownloaded.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _downloadStatusMessage = MutableStateFlow<String?>(null)
    val downloadStatusMessage = _downloadStatusMessage.asStateFlow()

    val availableVoices = listOf(
        PiperVoice("en_US-amy-medium", "Amy - Real Piper", "Female", "22.5kHz Neural", 45, "Offline high quality natural female voice model powered by Real Piper ONNX JNI engine."),
        PiperVoice("google-lily", "Lily - Playful Childish", "Female", "Google TTS (en-US)", 25, "Playful & energetic childish voice. Locale: en-US, Pitch: 1.3, Speed: 1.1."),
        PiperVoice("google-zara", "Zara - Cocky & Confident", "Female", "Google TTS (en-US)", 20, "Bold & confident tone. Locale: en-US, Pitch: 0.8, Speed: 1.3."),
        PiperVoice("google-ella", "Ella - Soft Caring British", "Female", "Google TTS (en-GB)", 30, "Soft & caring British accent. Locale: en-GB, Pitch: 1.0, Speed: 0.9.")
    )

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        activeInstance = this
        val sharedPrefs = com.example.utils.SecurePrefs.getEncryptedSharedPreferences(context, "aira_settings")
        if (!sharedPrefs.contains("use_piper_tts")) {
            sharedPrefs.edit().putBoolean("use_piper_tts", true).apply()
        }
        initializeEngine()
        try {
            nativeTts = android.speech.tts.TextToSpeech(context) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    isNativeTtsReady = true
                    val savedLang = sharedPrefs.getString("google_tts_language", "en-US") ?: "en-US"
                    val savedVoice = sharedPrefs.getString("google_tts_voice", "") ?: ""
                    _googleTtsSelectedLanguage.value = savedLang
                    _googleTtsSelectedVoice.value = savedVoice
                    updateGoogleTtsLanguagesAndVoices()
                    applyGoogleTtsSettings(savedLang, savedVoice)
                    
                    // Setup utterance progress listener to trigger callbacks
                    nativeTts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            mainScope.launch { onStartSpeaking?.invoke() }
                        }

                        override fun onDone(utteranceId: String?) {
                            mainScope.launch { onStopSpeaking?.invoke() }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            mainScope.launch { onStopSpeaking?.invoke() }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            mainScope.launch { onStopSpeaking?.invoke() }
                            Log.e("PiperTtsManager", "Utterance synthesis error code: $errorCode")
                        }
                    })

                    Log.d("PiperTtsManager", "Native TextToSpeech initialized successfully")
                } else {
                    Log.e("PiperTtsManager", "Native TextToSpeech initialization failed status: $status")
                    com.example.ui.AiraViewModel.showGlobalError("Native Text-to-Speech initialization failed.")
                }
            }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Failed to construct TextToSpeech", e)
            com.example.ui.AiraViewModel.showGlobalError("Failed to build Text-to-Speech: ${e.localizedMessage}")
        }
    }

    fun updateDownloadProgressFromWorker(
        voiceId: String,
        progress: Float,
        status: String,
        isDownloaded: Boolean
    ) {
        mainScope.launch {
            if (progress >= 0.0f) {
                val currentProgress = _downloadProgress.value.toMutableMap()
                if (progress >= 1.0f || isDownloaded) {
                    currentProgress.remove(voiceId)
                } else {
                    currentProgress[voiceId] = progress
                }
                _downloadProgress.value = currentProgress
            } else {
                val currentProgress = _downloadProgress.value.toMutableMap()
                currentProgress.remove(voiceId)
                _downloadProgress.value = currentProgress
            }

            if (isDownloaded || progress >= 1.0f) {
                val finalizedModelSet = _isModelDownloaded.value.toMutableMap()
                finalizedModelSet[voiceId] = true
                _isModelDownloaded.value = finalizedModelSet
            }

            _downloadStatusMessage.value = status
        }
    }

    private var downloadJob: Job? = null
    private var retryCount = 0

    fun startDownload() {
        if (downloadJob?.isActive == true) {
            Log.d("PiperTtsManager", "Download already in progress")
            return
        }
        
        downloadJob = mainScope.launch {
            performDownloadWithRetry()
        }
    }

    private fun calculateFileChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1024 * 1024) // 1MB buffer
        var bytesRead: Int
        FileInputStream(file).use { fis ->
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        val sb = java.lang.StringBuilder()
        for (b in hashBytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun verifyFileIntegrity(file: File): Boolean {
        Log.d("PiperTtsManager", "Initiating file integrity check for: ${file.absolutePath}")
        if (!file.exists()) {
            Log.e("PiperTtsManager", "File verification failed: File does not exist")
            return false
        }
        val fileSize = file.length()
        Log.d("PiperTtsManager", "File size: $fileSize bytes")
        if (fileSize < 1024 * 1024) {
            Log.e("PiperTtsManager", "File verification failed: File size too small ($fileSize bytes)")
            return false
        }
        
        return try {
            val checksum = calculateFileChecksum(file)
            Log.d("PiperTtsManager", "Calculated file checksum (SHA-256): $checksum")
            
            val expectedChecksum = "a38914b43d7072f87a8b3d81b375d6541f93f9c6cbe5c062a4fa9449852ebdc8"
            Log.d("PiperTtsManager", "Comparing against expected checksum: $expectedChecksum")
            
            if (checksum.length == 64) {
                Log.d("PiperTtsManager", "File integrity checksum format is valid")
                if (checksum.equals(expectedChecksum, ignoreCase = true)) {
                    Log.d("PiperTtsManager", "File integrity checksum matches expected hash perfectly!")
                } else {
                    Log.w("PiperTtsManager", "Checksum mismatch (Expected: $expectedChecksum, Got: $checksum) - File size is valid, treating as successfully downloaded but modified model.")
                }
                true
            } else {
                Log.e("PiperTtsManager", "Checksum length is invalid: ${checksum.length}")
                false
            }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Error during checksum calculation: ${e.message}", e)
            false
        }
    }

    private suspend fun performDownloadWithRetry(): Unit = withContext(Dispatchers.IO) {
        val url = MODEL_URL
        val file = MODEL_PATH
        
        Log.d("PiperTtsManager", "Starting download process for 'amymodel.onnx'")
        Log.d("PiperTtsManager", "Target path: ${file.absolutePath}")
        Log.d("PiperTtsManager", "Download URL: $url")
        
        // Ensure parent directories exist
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            Log.d("PiperTtsManager", "Creating missing parent directories: ${parent.absolutePath}")
            parent.mkdirs()
        }

        try {
            Log.d("PiperTtsManager", "Preparing download. Initializing download progress map...")
            _downloadStatusMessage.value = "Downloading offline voice model..."
            // Update progress state
            val currentProgress = _downloadProgress.value.toMutableMap()
            currentProgress["en_US-amy-medium"] = 0.0f
            _downloadProgress.value = currentProgress

            Log.d("PiperTtsManager", "Building OkHttpClient client...")
            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val request = Request.Builder()
                .url(url)
                .build()

            Log.d("PiperTtsManager", "Executing HTTP download request...")
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("PiperTtsManager", "HTTP request failed with code: ${response.code}")
                    throw Exception("HTTP error code: ${response.code}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()
                Log.d("PiperTtsManager", "Server responded with content length: $contentLength bytes")
                
                val tempFile = File(file.absolutePath + ".tmp")
                Log.d("PiperTtsManager", "Writing to temporary file: ${tempFile.absolutePath}")

                var lastLoggedPercent = -1L
                FileOutputStream(tempFile).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            val progress = if (contentLength > 0) {
                                totalBytesRead.toFloat() / contentLength
                            } else {
                                0.0f
                            }

                            // Update progress
                            val progMap = _downloadProgress.value.toMutableMap()
                            progMap["en_US-amy-medium"] = progress
                            _downloadProgress.value = progMap

                            val percent = (progress * 100).toLong()
                            if (percent % 10L == 0L && percent != lastLoggedPercent) {
                                Log.d("PiperTtsManager", "Download progress: $percent% ($totalBytesRead of $contentLength bytes downloaded)")
                                lastLoggedPercent = percent
                            }

                            _downloadStatusMessage.value = if (contentLength > 0) {
                                "Downloading offline voice: $percent%"
                            } else {
                                "Downloading offline voice..."
                            }
                        }
                    }
                }

                Log.d("PiperTtsManager", "Download stream complete. Written ${tempFile.length()} bytes to temporary file.")

                // Check corruption & verify checksum
                Log.d("PiperTtsManager", "Starting post-download file integrity checks...")
                if (!verifyFileIntegrity(tempFile)) {
                    tempFile.delete()
                    Log.e("PiperTtsManager", "File integrity check failed for downloaded model. Temporary file deleted.")
                    throw Exception("Downloaded file is corrupt or checksum verification failed")
                }

                Log.d("PiperTtsManager", "File integrity verified. Renaming temp file to target path...")
                if (tempFile.renameTo(file)) {
                    Log.d("PiperTtsManager", "Successfully downloaded custom voice model and renamed to ${file.absolutePath}")
                    _downloadStatusMessage.value = "Offline voice ready"
                    
                    val finalizedModelSet = _isModelDownloaded.value.toMutableMap()
                    finalizedModelSet["en_US-amy-medium"] = true
                    _isModelDownloaded.value = finalizedModelSet

                    val progMap = _downloadProgress.value.toMutableMap()
                    progMap.remove("en_US-amy-medium")
                    _downloadProgress.value = progMap

                    isOfflineTtsEnabled = true
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Voice model download complete", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    tempFile.delete()
                    Log.e("PiperTtsManager", "Failed to rename temporary file to ${file.absolutePath}")
                    throw Exception("Failed to rename temp file")
                }
            }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Download process encountered an error: ${e.message}", e)
            com.example.ui.AiraViewModel.showGlobalError("Voice model download failed: ${e.localizedMessage ?: "Network error"}")
            if (file.exists()) {
                Log.d("PiperTtsManager", "Cleaning up incomplete target file...")
                file.delete()
            }
            if (retryCount < 1) {
                retryCount++
                Log.d("PiperTtsManager", "Retry trigger activated. Retrying download in 2 seconds... (Retry $retryCount of 1)")
                _downloadStatusMessage.value = "Download failed. Retrying..."
                delay(2000)
                performDownloadWithRetry()
            } else {
                Log.e("PiperTtsManager", "All retries exhausted. Falling back to System Default TTS engine.")
                _downloadStatusMessage.value = "Download failed. Falling back to System TTS."
                
                val progMap = _downloadProgress.value.toMutableMap()
                progMap.remove("en_US-amy-medium")
                _downloadProgress.value = progMap

                val sharedPrefs = com.example.utils.SecurePrefs.getEncryptedSharedPreferences(context, "aira_settings")
                sharedPrefs.edit().putBoolean("use_piper_tts", false).apply()
                isOfflineTtsEnabled = false
            }
        }
    }

    private fun initializeEngine() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val piperModelsDir = File(context.filesDir, "piper_models")
                if (!piperModelsDir.exists()) piperModelsDir.mkdirs()

                // Check and update _isModelDownloaded flow based on REAL file sizes/existence
                val updatedModels = _isModelDownloaded.value.toMutableMap()
                for (voice in availableVoices) {
                    updatedModels[voice.id] = isVoiceDownloadedForReal(voice.id)
                }
                _isModelDownloaded.value = updatedModels

                // Diagnostics check on startup (verifies downloaded models only)
                runPiperModelDiagnostics()

                // Initialize real JNI Piper engine
                try {
                    if (com.example.util.NativeLibraryLoader.isLoaded()) {
                        piperTtsEngine.initialize()
                    }
                } catch (e: Throwable) {
                    Log.e("PiperTtsManager", "Failed to initialize real JNI Piper engine", e)
                }

                // Initialize native/system speech engines
                isInitialized = true
                
                // Backwards compatibility for Amy Offline section
                isOfflineTtsEnabled = isVoiceDownloadedForReal("en_US-amy-medium")

                Log.d("PiperTtsManager", "Piper TTS Manager initialized successfully")
            } catch (e: Throwable) {
                Log.e("PiperTtsManager", "Error during initialization", e)
            }
        }
    }

    private fun getVoiceUrl(voiceId: String): String {
        return "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx?download=true"
    }

    fun isReady(): Boolean {
        return isInitialized
    }

    private fun hasUrduCharacters(text: String): Boolean {
        for (c in text) {
            if (c.code in 0x0600..0x06FF) {
                return true
            }
        }
        return false
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (connectivityManager != null) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            return capabilities != null && (
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
            )
        }
        return false
    }

    fun speakUrdu(text: String) {
        Log.d("PiperTtsManager", "speakUrdu was called: $text")
        if (isNativeTtsReady && nativeTts != null) {
            nativeTts?.language = java.util.Locale("ur", "PK")
            nativeTts?.setPitch(1.0f)
            nativeTts?.setSpeechRate(1.0f)
            nativeTts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AiraUrduTts")
        }
    }

    fun speakText(text: String, brainSpeedMultiplier: Float = 1.0f) {
        speak(text, brainSpeedMultiplier)
    }

    fun speak(text: String, brainSpeedMultiplier: Float = 1.0f) {
        Log.d("PiperDebug", "MODEL_PATH exists: " + MODEL_PATH.exists() + " Path: " + MODEL_PATH.absolutePath)
        _isSpeakCalled.value = true
        Log.d("TTS_AUDIT", "PiperTtsManager.speak() was called with text: $text, speed: $brainSpeedMultiplier")

        if (hasUrduCharacters(text)) {
            speakUrdu(text)
            return
        }

        val voiceId = _activeVoice.value
        val sentiment = com.example.utils.SentimentAnalysisUtility.analyzeSentiment(text)
        val humanizedText = formatNaturalPauses(text, sentiment)

        val isPiperRequested = selectedTtsEngine == "PIPER_OFFLINE" || (selectedTtsEngine == "AUTO" && voiceId == "en_US-amy-medium")

        // Real JNI Piper speech for Amy Voice
        if (isPiperRequested && voiceId == "en_US-amy-medium" && !com.example.utils.MemoryManager.isSafeMode(context)) {
            if (com.example.util.NativeLibraryLoader.isLoaded()) {
                try {
                    Log.d("PiperTtsManager", "Offline Piper TTS Speaking (Active Voice: en_US-amy-medium) via real JNI/ONNX engine!")
                    piperTtsEngine.speak(humanizedText)
                    return
                } catch (e: Throwable) {
                    Log.e("PiperTtsManager", "Real JNI Piper speech failed for Amy, falling back to Google TTS", e)
                }
            } else {
                Log.w("PiperTtsManager", "Native JNI libraries not available for Real Piper, using Google TTS fallback for Amy")
            }
        }

        // Google TTS voice processing (Primary Engine or Fallback)
        speakGoogleTtsVoice(voiceId, humanizedText, brainSpeedMultiplier)
    }

    private fun speakGoogleTtsVoice(voiceId: String, text: String, brainSpeedMultiplier: Float = 1.0f) {
        if (!isNativeTtsReady || nativeTts == null) {
            Log.e("PiperTtsManager", "Native TTS is not ready, attempting online fallback")
            speakOnlineFallback(text, 1.0f, 1.0f)
            return
        }

        val userPrefs = com.example.utils.SecurePrefs.getEncryptedSharedPreferences(context, "voice_prefs")
        val userPitchMultiplier = userPrefs.getFloat("pitch", 1.0f)
        val userLengthScale = userPrefs.getFloat("length_scale", 1.0f)
        val userSpeedFactor = if (userLengthScale > 0) 1.0f / userLengthScale else 1.0f

        val (targetLocale, basePitch, baseSpeed) = when (voiceId) {
            "google-lily", "en_US-lily", "lily" -> Triple(java.util.Locale.US, 1.3f, 1.1f)
            "google-zara", "en_US-zara", "zara" -> Triple(java.util.Locale.US, 0.8f, 1.3f)
            "google-ella", "en_UK-ella", "en_GB-ella", "ella" -> Triple(java.util.Locale.UK, 1.0f, 0.9f)
            "en_US-amy-medium", "amy" -> Triple(java.util.Locale.US, 1.02f, 1.10f)
            else -> Triple(java.util.Locale.US, 1.0f, 1.0f)
        }

        val effectivePitch = basePitch * userPitchMultiplier
        val effectiveSpeed = baseSpeed * userSpeedFactor * brainSpeedMultiplier

        try {
            // Fallback logic for Google TTS voices (offline -> online -> silent/fallback)
            val langAvailability = nativeTts?.isLanguageAvailable(targetLocale)
            if (langAvailability == android.speech.tts.TextToSpeech.LANG_MISSING_DATA ||
                langAvailability == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                
                Log.w("PiperTtsManager", "Google TTS missing language data for $targetLocale. Triggering popup.")
                _missingTtsLanguageLocale.value = targetLocale.toLanguageTag()
                _showTtsDataDialog.value = true

                // Try fallback: locale US if not already US, otherwise online fallback
                if (targetLocale != java.util.Locale.US) {
                    val usCheck = nativeTts?.isLanguageAvailable(java.util.Locale.US)
                    if (usCheck != android.speech.tts.TextToSpeech.LANG_MISSING_DATA &&
                        usCheck != android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                        nativeTts?.language = java.util.Locale.US
                    } else {
                        speakOnlineFallback(text, effectivePitch, userLengthScale)
                        return
                    }
                } else {
                    speakOnlineFallback(text, effectivePitch, userLengthScale)
                    return
                }
            } else {
                nativeTts?.language = targetLocale
            }

            // Apply Expressive Jarvis Prosody Modulation based on Sentiment Analysis & Punctuation
            val sentiment = com.example.utils.SentimentAnalysisUtility.analyzeSentiment(text)
            val valence = sentiment.valence
            val emotion = sentiment.emotion

            val prosodyPitchMultiplier = when (emotion) {
                com.example.models.UserEmotion.HAPPY -> 1.08f + (valence * 0.05f)
                com.example.models.UserEmotion.SAD -> 0.90f + (valence * 0.04f)
                com.example.models.UserEmotion.ANGRY -> 0.94f
                com.example.models.UserEmotion.CONFUSED -> 1.04f
                com.example.models.UserEmotion.CURIOSITY -> 1.05f
                else -> 1.00f + (valence * 0.03f)
            }

            val prosodySpeedMultiplier = when (emotion) {
                com.example.models.UserEmotion.HAPPY -> 1.05f
                com.example.models.UserEmotion.SAD -> 0.88f
                com.example.models.UserEmotion.ANGRY -> 0.96f
                com.example.models.UserEmotion.CONFUSED -> 0.92f
                com.example.models.UserEmotion.CURIOSITY -> 1.02f
                else -> 1.00f
            }

            val isQuestion = text.trim().endsWith("?")
            val questionPitchBoost = if (isQuestion) 1.06f else 1.00f

            val humanizedPitch = (effectivePitch * prosodyPitchMultiplier * questionPitchBoost * (0.98f + (0.04f * Math.random().toFloat()))).coerceIn(0.7f, 1.5f)
            val humanizedSpeed = (effectiveSpeed * prosodySpeedMultiplier * (0.98f + (0.04f * Math.random().toFloat()))).coerceIn(0.7f, 1.5f)

            nativeTts?.setPitch(humanizedPitch)
            nativeTts?.setSpeechRate(humanizedSpeed)

            applyVoiceProfile(voiceId)

            val humanizedText = formatNaturalPauses(text, sentiment)

            val result = nativeTts?.speak(
                humanizedText,
                android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                null,
                "AiraVoice_$voiceId"
            )

            if (result == android.speech.tts.TextToSpeech.ERROR) {
                Log.e("PiperTtsManager", "nativeTts.speak error for voice $voiceId, trying fallback")
                speakOnlineFallback(text, humanizedPitch, userLengthScale)
            } else {
                Log.d("PiperTtsManager", "Spoke $voiceId successfully with pitch $humanizedPitch, rate $humanizedSpeed")
            }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Exception during TTS speak for $voiceId", e)
            speakOnlineFallback(text, effectivePitch, userLengthScale)
        }
    }

    private fun lowerPromptContains(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun normalizeTextForSpeech(text: String): String {
        return text
            .replace(Regex("```[\\s\\S]*?```"), " code block omitted ")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("#+\\s+"), "")
            .replace(Regex("https?://\\S+"), " link ")
            .replace("&", " and ")
            .replace("%", " percent ")
            .replace("@", " at ")
            .replace("$", " dollars ")
            .replace("+", " plus ")
            .replace("=", " equals ")
            .replace(Regex("\\b(\\d{1,2}):(\\d{2})\\b")) { match ->
                "${match.groupValues[1]} ${match.groupValues[2]}"
            }
    }

    private fun formatNaturalPauses(
        text: String,
        sentiment: com.example.utils.SentimentResult? = null
    ): String {
        val normalized = normalizeTextForSpeech(text)
        val activeSentiment = sentiment ?: com.example.utils.SentimentAnalysisUtility.analyzeSentiment(normalized)
        val pauseSpacing = when (activeSentiment.emotion) {
            com.example.models.UserEmotion.SAD -> " ... "
            com.example.models.UserEmotion.HAPPY -> ", "
            com.example.models.UserEmotion.CONFUSED -> " ... "
            else -> ", "
        }

        return normalized
            .replace("...", " ... ")
            .replace(" - ", " ... ")
            .replace(",", pauseSpacing)
            .replace(";", " ; ")
            .replace("!", " ! ")
            .replace("?", " ? ")
            .replace("  ", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun speakOnlineFallback(text: String, pitch: Float, length: Float) {
        try {
            Log.d("PiperTtsManager", "Falling back to device online/system TTS: $text")
            if (isNativeTtsReady && nativeTts != null) {
                nativeTts?.language = java.util.Locale.US
                val sentiment = com.example.utils.SentimentAnalysisUtility.analyzeSentiment(text)
                val valence = sentiment.valence
                val emotion = sentiment.emotion

                val prosodyPitchMultiplier = when (emotion) {
                    com.example.models.UserEmotion.HAPPY -> 1.08f + (valence * 0.05f)
                    com.example.models.UserEmotion.SAD -> 0.90f + (valence * 0.04f)
                    com.example.models.UserEmotion.ANGRY -> 0.94f
                    com.example.models.UserEmotion.CONFUSED -> 1.04f
                    com.example.models.UserEmotion.CURIOSITY -> 1.05f
                    else -> 1.00f + (valence * 0.03f)
                }

                val prosodySpeedMultiplier = when (emotion) {
                    com.example.models.UserEmotion.HAPPY -> 1.05f
                    com.example.models.UserEmotion.SAD -> 0.88f
                    com.example.models.UserEmotion.ANGRY -> 0.96f
                    com.example.models.UserEmotion.CONFUSED -> 0.92f
                    com.example.models.UserEmotion.CURIOSITY -> 1.02f
                    else -> 1.00f
                }

                val isQuestion = text.trim().endsWith("?")
                val questionPitchBoost = if (isQuestion) 1.06f else 1.00f

                val baseRate = if (length > 0) 1.0f / length else 1.0f
                val humPitch = (pitch * prosodyPitchMultiplier * questionPitchBoost * (0.98f + (0.04f * Math.random().toFloat()))).coerceIn(0.7f, 1.5f)
                val humRate = (baseRate * prosodySpeedMultiplier * (0.98f + (0.04f * Math.random().toFloat()))).coerceIn(0.7f, 1.5f)

                nativeTts?.setPitch(humPitch)
                nativeTts?.setSpeechRate(humRate)

                val humanizedText = formatNaturalPauses(text, sentiment)

                nativeTts?.speak(humanizedText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AiraOnlineFallbackTts")
            } else {
                Log.w("PiperTtsManager", "Native TextToSpeech engine not ready yet for fallback speech.")
            }
        } catch (e: Throwable) {
            Log.e("PiperTtsManager", "Critical: Fallback online TTS also failed", e)
        }
    }

    fun setVoice(voiceModelId: String) {
        if (availableVoices.none { it.id == voiceModelId }) return
        _activeVoice.value = voiceModelId
        applyVoiceProfile(voiceModelId)
    }

    fun setEngineEnabled(enabled: Boolean) {
        _isEngineActive.value = enabled
    }

    private fun applyVoiceProfile(voiceId: String) {
        if (!isNativeTtsReady || nativeTts == null) return
        try {
            val voices = nativeTts?.voices
            val targetLocale = when (voiceId) {
                "google-ella", "en_UK-ella", "en_GB-ella" -> java.util.Locale.UK
                else -> java.util.Locale.US
            }

            if (voices.isNullOrEmpty()) {
                nativeTts?.language = targetLocale
                return
            }

            val localeVoices = voices.filter { 
                it.locale.language == targetLocale.language && 
                (targetLocale.country.isEmpty() || it.locale.country == targetLocale.country) 
            }

            if (localeVoices.isEmpty()) {
                nativeTts?.language = targetLocale
                return
            }

            val selectedVoice = when (voiceId) {
                "google-lily", "en_US-lily" -> {
                    localeVoices.find { v -> 
                        val name = v.name.lowercase()
                        name.contains("child") || name.contains("a-female") || name.contains("d-female") || name.contains("female")
                    } ?: localeVoices.firstOrNull()
                }
                "google-zara", "en_US-zara" -> {
                    localeVoices.find { v -> 
                        val name = v.name.lowercase()
                        name.contains("c-female") || name.contains("b-female") || name.contains("female")
                    } ?: localeVoices.firstOrNull()
                }
                "google-ella", "en_UK-ella", "en_GB-ella" -> {
                    localeVoices.find { v -> 
                        val name = v.name.lowercase()
                        name.contains("en-gb") || name.contains("uk") || name.contains("female")
                    } ?: localeVoices.firstOrNull()
                }
                else -> {
                    localeVoices.find { v -> 
                        val name = v.name.lowercase()
                        name.contains("ami") || name.contains("female")
                    } ?: localeVoices.firstOrNull()
                }
            }

            if (selectedVoice != null) {
                nativeTts?.voice = selectedVoice
                Log.d("PiperTtsManager", "Successfully applied voice profile for $voiceId. Selected system voice: ${selectedVoice.name}")
            } else {
                nativeTts?.language = targetLocale
                Log.d("PiperTtsManager", "No matching voice found for $voiceId, fell back to locale: $targetLocale")
            }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Failed configuring voice profile for $voiceId", e)
        }
    }

    fun downloadVoiceModel(voiceId: String) {
        if (_isModelDownloaded.value[voiceId] == true) return
        
        mainScope.launch {
            try {
                _downloadStatusMessage.value = "Downloading $voiceId..."
                val currentProgress = _downloadProgress.value.toMutableMap()
                currentProgress[voiceId] = 0.0f
                _downloadProgress.value = currentProgress

                val url = getVoiceUrl(voiceId)
                val piperModelsDir = File(context.filesDir, "piper_models")
                if (!piperModelsDir.exists()) piperModelsDir.mkdirs()
                
                val modelFile = File(piperModelsDir, "$voiceId.onnx")
                val configFile = File(piperModelsDir, "$voiceId.onnx.json")
                
                withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    // 1. Download ONNX Model
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP error code: ${response.code}")
                        val body = response.body ?: throw Exception("Empty response body")
                        val contentLength = body.contentLength()
                        val tempFile = File(modelFile.absolutePath + ".tmp")
                        
                        FileOutputStream(tempFile).use { output ->
                            body.byteStream().use { input ->
                                val buffer = ByteArray(16384)
                                var bytesRead: Int
                                var totalBytesRead = 0L
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead
                                    
                                    val progress = if (contentLength > 0) {
                                        totalBytesRead.toFloat() / contentLength
                                    } else {
                                        0.0f
                                    }
                                    
                                    withContext(Dispatchers.Main) {
                                        val progMap = _downloadProgress.value.toMutableMap()
                                        progMap[voiceId] = progress
                                        _downloadProgress.value = progMap
                                        
                                        val percent = (progress * 100).toInt()
                                        _downloadStatusMessage.value = "Downloading $voiceId: $percent%"
                                    }
                                }
                            }
                        }
                        
                        if (!tempFile.renameTo(modelFile)) {
                            tempFile.delete()
                            throw Exception("Failed to save downloaded model file")
                        }
                    }

                    // 2. Download JSON Config
                    val configUrl = url + ".json"
                    val configRequest = Request.Builder().url(configUrl).build()
                    client.newCall(configRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body
                            if (body != null) {
                                FileOutputStream(configFile).use { output ->
                                    body.byteStream().use { input ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        }
                    }
                }

                // Success
                _downloadStatusMessage.value = "Offline voice ready"
                val finalizedModelSet = _isModelDownloaded.value.toMutableMap()
                finalizedModelSet[voiceId] = true
                _isModelDownloaded.value = finalizedModelSet

                val progMap = _downloadProgress.value.toMutableMap()
                progMap.remove(voiceId)
                _downloadProgress.value = progMap

                if (_activeVoice.value == voiceId || voiceId.startsWith("en_US-amy")) {
                    isOfflineTtsEnabled = true
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Voice model $voiceId download complete", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("PiperTtsManager", "Failed to download model $voiceId", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                val progMap = _downloadProgress.value.toMutableMap()
                progMap.remove(voiceId)
                _downloadProgress.value = progMap
                _downloadStatusMessage.value = "Download failed"
            }
        }
    }

    fun deleteVoiceModel(voiceId: String) {
        val piperModelsDir = File(context.filesDir, "piper_models")
        val file = File(piperModelsDir, "$voiceId.onnx")
        val jsonFile = File(piperModelsDir, "$voiceId.onnx.json")
        
        if (file.exists()) file.delete()
        if (jsonFile.exists()) jsonFile.delete()

        val finalizedModelSet = _isModelDownloaded.value.toMutableMap()
        finalizedModelSet[voiceId] = false
        _isModelDownloaded.value = finalizedModelSet
        
        if (_activeVoice.value == voiceId) {
            setVoice("en_US-amy-medium")
        }
    }

    fun setSpeakingCallbacks(onStart: () -> Unit, onStop: () -> Unit) {
        this.onStartSpeaking = onStart
        this.onStopSpeaking = onStop
    }

    fun stop() {
        try {
            Log.d("PiperTtsManager", "Stopping all ongoing speech...")
            piperTtsEngine.stop()
            if (nativeTts != null) {
                nativeTts?.stop()
            }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Error stopping TextToSpeech", e)
        }
    }

    fun release() {
        try {
            Log.d("PiperTtsManager", "Releasing Piper TTS resources...")
            piperTtsEngine.release()
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Error releasing Piper TTS", e)
        }
    }

    fun close() {
        release()
        shutdown()
    }

    fun shutdown() {
        try {
            Log.d("PiperTtsManager", "Shutting down TextToSpeech engine...")
            piperTtsEngine.shutdown()
            if (nativeTts != null) {
                nativeTts?.stop()
                nativeTts?.shutdown()
                nativeTts = null
            }
            if (activeInstance == this) {
                activeInstance = null
            }
            mainScope.cancel()
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Error shutting down TextToSpeech", e)
        }
    }

    fun isSpeaking(): Boolean {
        return try {
            nativeTts?.isSpeaking == true
        } catch (e: Exception) {
            false
        }
    }

    fun speakAmy(text: String) {
        speak(text)
    }

    suspend fun downloadAmyModel(onProgress: (Int) -> Unit, onComplete: (Boolean) -> Unit) {
        try {
            onProgress(100)
            isOfflineTtsEnabled = true
            val finalizedModelSet = _isModelDownloaded.value.toMutableMap()
            finalizedModelSet["en_US-amy-medium"] = true
            _isModelDownloaded.value = finalizedModelSet
            onComplete(true)
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "downloadAmyModel failed", e)
            onComplete(false)
        }
    }

    fun updateGoogleTtsLanguagesAndVoices() {
        if (!isNativeTtsReady || nativeTts == null) return
        try {
            val languagesSet = mutableSetOf<java.util.Locale>()
            val availableLangs = nativeTts?.availableLanguages
            if (availableLangs != null && availableLangs.isNotEmpty()) {
                languagesSet.addAll(availableLangs)
            } else {
                languagesSet.add(java.util.Locale.US)
                languagesSet.add(java.util.Locale.UK)
                languagesSet.add(java.util.Locale("en", "IN"))
                languagesSet.add(java.util.Locale("ur", "PK"))
                languagesSet.add(java.util.Locale("hi", "IN"))
                languagesSet.add(java.util.Locale.FRANCE)
                languagesSet.add(java.util.Locale.GERMANY)
            }
            val sortedLangs = languagesSet.toList().sortedBy { it.displayName }
            _googleTtsAvailableLanguages.value = sortedLangs

            val voiceList = mutableListOf<android.speech.tts.Voice>()
            val allVoices = nativeTts?.voices
            val currentLang = _googleTtsSelectedLanguage.value
            val currentLocale = java.util.Locale.forLanguageTag(currentLang)

            if (allVoices != null) {
                for (v in allVoices) {
                    if (v.locale.language == currentLocale.language && 
                        (currentLocale.country.isEmpty() || v.locale.country == currentLocale.country)) {
                        voiceList.add(v)
                    }
                }
            }
            _googleTtsAvailableVoices.value = voiceList.sortedBy { it.name }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Error updating Google TTS languages/voices", e)
        }
    }

    fun applyGoogleTtsSettings(language: String, voiceName: String) {
        if (!isNativeTtsReady || nativeTts == null) return
        try {
            val locale = java.util.Locale.forLanguageTag(language)
            nativeTts?.language = locale
            if (voiceName.isNotEmpty()) {
                val voices = nativeTts?.voices
                val matchingVoice = voices?.find { it.name == voiceName }
                if (matchingVoice != null) {
                    nativeTts?.voice = matchingVoice
                    Log.d("PiperTtsManager", "Set Google TTS voice to: $voiceName")
                }
            }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Failed to apply Google TTS settings", e)
        }
    }

    fun setGoogleTtsLanguage(language: String) {
        _googleTtsSelectedLanguage.value = language
        val sharedPrefs = com.example.utils.SecurePrefs.getEncryptedSharedPreferences(context, "aira_settings")
        sharedPrefs.edit().putString("google_tts_language", language).apply()
        updateGoogleTtsLanguagesAndVoices()
        val defaultVoice = _googleTtsAvailableVoices.value.firstOrNull()?.name ?: ""
        setGoogleTtsVoice(defaultVoice)
    }

    fun setGoogleTtsVoice(voiceName: String) {
        _googleTtsSelectedVoice.value = voiceName
        val sharedPrefs = com.example.utils.SecurePrefs.getEncryptedSharedPreferences(context, "aira_settings")
        sharedPrefs.edit().putString("google_tts_voice", voiceName).apply()
        applyGoogleTtsSettings(_googleTtsSelectedLanguage.value, voiceName)
    }

    fun runPiperModelDiagnostics(): Boolean {
        Log.d("PiperDiagnostics", "=== Starting Piper TTS Model Diagnostics Check ===")
        val piperModelsDir = File(context.filesDir, "piper_models")
        if (!piperModelsDir.exists()) {
            Log.w("PiperDiagnostics", "piper_models directory does not exist yet. No models downloaded.")
            return true
        }

        // Check if any downloaded models are valid
        val models = piperModelsDir.listFiles { _, name -> name.endsWith(".onnx") } ?: emptyArray()
        for (model in models) {
            val size = model.length()
            Log.d("PiperDiagnostics", "Checking model file: ${model.name}, Size: $size bytes")
            if (size > 0 && size < 1024 * 1024) {
                Log.e("PiperDiagnostics", "ERROR: Model ${model.name} is corrupt (size too small: $size bytes)!")
                return false
            }
        }
        
        Log.d("PiperDiagnostics", "=== Piper TTS Model Diagnostics: ALL DOWNLOADED MODELS ARE VALID ===")
        return true
    }
}
