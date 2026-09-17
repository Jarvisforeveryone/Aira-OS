package com.aira.assistant.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.aira.assistant.MainActivity
import com.aira.assistant.R
import com.aira.assistant.utils.DownloadManager
import com.aira.assistant.core.memory.MemoryManager
import kotlinx.coroutines.*
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener as VoskRecognitionListener
import org.vosk.android.SpeechService as VoskSpeechService

class ActiveListeningService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Primary Low-Power DSP STT Listener
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechIntent: Intent? = null

    // Fallback: Vosk Keyword Spotter
    private var voskSpeechService: VoskSpeechService? = null
    private var voskModel: Model? = null

    private var isListeningLoopRunning = false
    private var currentWakeWord = "hey aira"
    private var consecutiveErrorCount = 0

    private val restartRunnable = Runnable {
        if (isListeningLoopRunning) {
            startSpeechRecognizerListening()
        }
    }

    companion object {
        private const val TAG = "ActiveListeningService"
        const val CHANNEL_ID = "aira_active_listening_channel"
        const val NOTIFICATION_ID = 4001
        const val ACTION_START = "com.aira.assistant.ACTION_START_ACTIVE_LISTENING"
        const val ACTION_STOP = "com.aira.assistant.ACTION_STOP_ACTIVE_LISTENING"
        const val ACTION_WAKE_WORD_TRIGGERED = "com.aira.assistant.ACTION_WAKE_WORD_TRIGGERED"
        const val EXTRA_WAKE_WORD = "extra_wake_word"

        fun startService(context: Context, wakeWord: String = "Hey Aira") {
            val intent = Intent(context, ActiveListeningService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WAKE_WORD, wakeWord)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch service", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ActiveListeningService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop service", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            Log.d(TAG, "Stopping Active Listening Foreground Service")
            stopListening()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        currentWakeWord = intent?.getStringExtra(EXTRA_WAKE_WORD) ?: "Hey Aira"
        createNotificationChannel()
        val notification = createNotification(currentWakeWord)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted. Foreground service may not show notification.")
                // Still call startForeground, but wrap in try-catch
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "Active Listening Service started foreground with wake word: $currentWakeWord")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException on startForeground: ${e.message}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // On Android 14+, type cannot be dropped if mic is used; stop service to avoid fatal exception
                stopSelf()
                return START_NOT_STICKY
            }
            // Fallback: Try without type only on older Android versions prior to Android 14
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                Log.e(TAG, "Critical: Cannot start foreground", e2)
                stopSelf()
                return START_NOT_STICKY
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground with microphone type", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                stopSelf()
                return START_NOT_STICKY
            }
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                Log.e(TAG, "Critical failure launching foreground service", e2)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startListening()

        return START_NOT_STICKY
    }

    private fun startListening() {
        if (isListeningLoopRunning) return
        isListeningLoopRunning = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot start active listening: RECORD_AUDIO permission not granted.")
            stopSelf()
            return
        }

        mainHandler.post {
            initAndStartBuiltInSpeechRecognizer()
        }
    }

    /**
     * PRIMARY ENGINE: Built-in Android SpeechRecognizer (runs on low-power DSP hardware).
     */
    private fun initAndStartBuiltInSpeechRecognizer() {
        if (!isListeningLoopRunning) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "SpeechRecognizer is not available on this device. Falling back to Vosk KWS.")
            startFallbackListening()
            return
        }

        try {
            releaseSpeechRecognizer()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            if (speechRecognizer == null) {
                Log.w(TAG, "Failed to create SpeechRecognizer instance. Falling back.")
                startFallbackListening()
                return
            }

            speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    scheduleRestartSpeechRecognizer(100)
                }

                override fun onError(error: Int) {
                    consecutiveErrorCount++
                    val backoffDelay = (500L * (1L shl (consecutiveErrorCount - 1).coerceAtMost(5)))
                        .coerceAtMost(16000L)
                    Log.d(TAG, "SpeechRecognizer error: $error (streak=$consecutiveErrorCount). Restarting in ${backoffDelay}ms.")
                    scheduleRestartSpeechRecognizer(backoffDelay)
                }

                override fun onResults(results: Bundle?) {
                    consecutiveErrorCount = 0
                    checkSpeechResults(results)
                    scheduleRestartSpeechRecognizer(100)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    checkSpeechResults(partialResults)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            startSpeechRecognizerListening()
            Log.i(TAG, "Primary SpeechRecognizer active listening initialized successfully.")

        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing primary SpeechRecognizer: ${e.message}", e)
            startFallbackListening()
        }
    }

    private fun startSpeechRecognizerListening() {
        if (!isListeningLoopRunning || speechRecognizer == null || speechIntent == null) return
        try {
            speechRecognizer?.startListening(speechIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Error starting SpeechRecognizer listening: ${e.message}")
            scheduleRestartSpeechRecognizer(500)
        }
    }

    private fun scheduleRestartSpeechRecognizer(delayMs: Long) {
        if (!isListeningLoopRunning) return
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.postDelayed(restartRunnable, delayMs)
    }

    private fun checkSpeechResults(bundle: Bundle?) {
        if (bundle == null) return
        val matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (text in matches) {
            val lowercase = text.lowercase().trim()
            val target = currentWakeWord.lowercase().trim()
            if (lowercase.contains(target) || lowercase.contains("hey aira") || lowercase.contains("aira") || lowercase.contains("jarvis")) {
                Log.i(TAG, "Wake word detected via built-in SpeechRecognizer: '$text'")
                onWakeWordMatched(text)
                // Pause and restart recognizer
                mainHandler.post {
                    speechRecognizer?.stopListening()
                    scheduleRestartSpeechRecognizer(1000)
                }
                break
            }
        }
    }

    /**
     * FALLBACK:
     * If built-in SpeechRecognizer is unavailable, fallback to Vosk Keyword Spotting.
     * If Vosk is unavailable, display a user Toast notifying speech recognition is unavailable.
     */
    private fun startFallbackListening() {
        serviceScope.launch {
            try {
                val isVoskDownloaded = DownloadManager.isVoskModelDownloaded(applicationContext)
                val isVoskSupported = MemoryManager.isVoskSupported(applicationContext)

                if (isVoskDownloaded && isVoskSupported) {
                    val modelDir = DownloadManager.getVoskModelDir(applicationContext)
                    if (modelDir.exists()) {
                        Log.i(TAG, "Starting Vosk Keyword Spotting fallback recognizer...")
                        voskModel = Model(modelDir.absolutePath)
                        val recognizer = Recognizer(voskModel, 16000.0f)
                        voskSpeechService = VoskSpeechService(recognizer, 16000.0f)
                        voskSpeechService?.startListening(object : VoskRecognitionListener {
                            override fun onResult(hypothesis: String) { checkVoskHypothesis(hypothesis) }
                            override fun onPartialResult(hypothesis: String) { checkVoskHypothesis(hypothesis) }
                            override fun onFinalResult(hypothesis: String) { checkVoskHypothesis(hypothesis) }
                            override fun onError(exception: Exception) {
                                Log.w(TAG, "Vosk fallback listener error: ${exception.message}")
                            }
                            override fun onTimeout() {}
                        })
                        return@launch
                    }
                }

                // Fallback Toast if neither SpeechRecognizer nor Vosk are available
                mainHandler.post {
                    Toast.makeText(
                        applicationContext,
                        "AIRA: Speech recognition unavailable on this device.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in fallback active listening: ${e.message}", e)
            }
        }
    }

    private fun checkVoskHypothesis(hypothesis: String) {
        try {
            val json = JSONObject(hypothesis)
            val text = json.optString("text").ifBlank { json.optString("partial") }.lowercase().trim()
            val target = currentWakeWord.lowercase().trim()
            if (text.isNotEmpty() && (text.contains(target) || text.contains("aira") || text.contains("hey aira") || text.contains("jarvis"))) {
                Log.i(TAG, "Wake word matched in Vosk fallback: '$text'")
                onWakeWordMatched(text)
            }
        } catch (_: Exception) {}
    }

    private fun onWakeWordMatched(detectedPhrase: String) {
        try {
            // 1. Broadcast wake word event
            val broadcastIntent = Intent(ACTION_WAKE_WORD_TRIGGERED).apply {
                setPackage(packageName)
                putExtra("wake_word", detectedPhrase)
            }
            sendBroadcast(broadcastIntent)

            // 2. Launch or bring MainActivity to front
            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                action = "com.aira.assistant.ACTION_WAKE_WORD_DETECTED"
                putExtra(EXTRA_WAKE_WORD, detectedPhrase)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching wake word event", e)
        }
    }

    private fun releaseSpeechRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying SpeechRecognizer", e)
        }
    }

    private fun stopListening() {
        isListeningLoopRunning = false
        consecutiveErrorCount = 0
        mainHandler.removeCallbacks(restartRunnable)

        // Stop Primary SpeechRecognizer
        mainHandler.post {
            releaseSpeechRecognizer()
        }

        // Stop Vosk Fallback
        try {
            voskSpeechService?.stop()
            voskSpeechService?.shutdown()
            voskSpeechService = null
            voskModel?.close()
            voskModel = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Vosk service", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Active Listening Mode"
            val descriptionText = "Persistent low-power voice wake monitoring service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(wakeWord: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ActiveListeningService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AIRA Active Listening Mode")
            .setContentText("Low-power voice wake active • Say \"$wakeWord\"")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingOpenApp)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Active Listening",
                pendingStop
            )
            .setSubText("Hardware DSP Voice Wake")
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        serviceScope.cancel()
    }
}
