package com.aira.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.service.PiperTtsManager
import com.tencent.piperncnn.PiperNcnn
import kotlinx.coroutines.*

class PiperTtsEngine(private val context: Context) {
    init {
        try {
            com.example.util.NativeLibraryLoader.loadLibraries(context)
        } catch (e: Throwable) {
            Log.e("PiperTtsEngine", "Failed to load libraries in init", e)
        }
    }
    private var isInitialized = false
    private var piperNcnn: PiperNcnn? = null
    private var isModelLoaded = false

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var activePlayJob: Job? = null
    private var audioTrack: AudioTrack? = null

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isModelLoaded) return@withContext
        com.example.utils.MemoryManager.loadModelOnDemand(context, com.example.utils.NativeModelType.PIPER_TTS) {
            try {
                if (!com.example.util.NativeLibraryLoader.isLoaded()) {
                    Log.w("PiperTtsEngine", "Native libraries not loaded, skipping JNI model load")
                    return@loadModelOnDemand
                }
                if (piperNcnn == null) {
                    piperNcnn = PiperNcnn()
                }
                Log.d("PiperTtsEngine", "Initializing JNI Piper model from assets...")
                val modelPath = "piper/models/en_US-amy-medium.onnx"
                val configPath = "piper/models/en_US-amy-medium.onnx.json"
                
                val success = piperNcnn?.loadModel(context.assets, modelPath, configPath) == true
                if (success) {
                    isModelLoaded = true
                    isInitialized = true
                    Log.d("PiperTtsEngine", "Piper model loaded successfully via JNI!")
                } else {
                    Log.e("PiperTtsEngine", "Failed to load Piper model via JNI")
                }
            } catch (e: Throwable) {
                Log.e("PiperTtsEngine", "Throwable initializing Piper JNI engine", e)
            }
        }
    }

    fun speak(text: String) {
        Log.d("PiperTtsEngine", "Speak request: $text")
        stop()

        activePlayJob = engineScope.launch(Dispatchers.Default) {
            try {
                if (!com.example.util.NativeLibraryLoader.isLoaded()) {
                    Log.w("PiperTtsEngine", "Native libraries not loaded, cannot speak via JNI")
                    return@launch
                }
                if (!isModelLoaded) {
                    initialize()
                }
                if (!isModelLoaded || piperNcnn == null) {
                    Log.e("PiperTtsEngine", "Cannot speak, model not loaded or JNI engine unavailable")
                    return@launch
                }

                // Callbacks for starting speech (triggers UI waves/pulses)
                withContext(Dispatchers.Main) {
                    PiperTtsManager.activeInstance?.onStartSpeaking?.invoke()
                }

                Log.d("PiperTtsEngine", "Synthesizing text: $text")
                val startTime = System.currentTimeMillis()
                val floatData = piperNcnn?.synthesize(text, 1.0f)
                val duration = System.currentTimeMillis() - startTime
                
                if (floatData == null || floatData.isEmpty()) {
                    Log.e("PiperTtsEngine", "Synthesis returned null or empty audio data")
                    withContext(Dispatchers.Main) {
                        PiperTtsManager.activeInstance?.onStopSpeaking?.invoke()
                    }
                    return@launch
                }

                Log.d("PiperTtsEngine", "Synthesized ${floatData.size} samples in ${duration}ms")

                // Play PCM float data
                playAudio(floatData)

            } catch (e: Throwable) {
                Log.e("PiperTtsEngine", "Throwable during JNI speak", e)
            } finally {
                withContext(Dispatchers.Main) {
                    PiperTtsManager.activeInstance?.onStopSpeaking?.invoke()
                }
            }
        }
    }

    private suspend fun playAudio(floatData: FloatArray) {
        val sampleRate = 22050
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufferSize, floatData.size * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.play()

            // Write PCM data
            val written = track.write(floatData, 0, floatData.size, AudioTrack.WRITE_BLOCKING)
            Log.d("PiperTtsEngine", "Wrote $written of ${floatData.size} float samples to AudioTrack")

            // Wait for playback to finish based on clip length
            val playDurationMs = (floatData.size.toDouble() / sampleRate * 1000).toLong()
            delay(playDurationMs + 100)

        } catch (e: Exception) {
            Log.e("PiperTtsEngine", "AudioTrack play exception", e)
        } finally {
            cleanupAudioTrack()
        }
    }

    private fun cleanupAudioTrack() {
        try {
            audioTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    stop()
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e("PiperTtsEngine", "Error cleaning up AudioTrack", e)
        } finally {
            audioTrack = null
        }
    }

    fun stop() {
        Log.d("PiperTtsEngine", "Stop requested")
        try {
            activePlayJob?.cancel()
            activePlayJob = null
            cleanupAudioTrack()
            PiperTtsManager.activeInstance?.onStopSpeaking?.invoke()
        } catch (e: Exception) {
            Log.e("PiperTtsEngine", "Error in stop", e)
        }
    }

    fun release() {
        Log.d("PiperTtsEngine", "Release requested")
        com.example.utils.MemoryManager.releaseModel(com.example.utils.NativeModelType.PIPER_TTS) {
            stop()
            piperNcnn = null
            isModelLoaded = false
            isInitialized = false
        }
    }

    fun close() {
        release()
        shutdown()
    }

    fun shutdown() {
        Log.d("PiperTtsEngine", "Shutdown requested")
        release()
        try {
            engineScope.cancel()
        } catch (e: Exception) {
            Log.e("PiperTtsEngine", "Error canceling engineScope", e)
        }
    }
}
