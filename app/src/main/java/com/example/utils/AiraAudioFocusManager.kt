package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

class AiraAudioFocusManager(private val context: Context) {
    companion object {
        private const val TAG = "AiraAudioFocus"

        @Volatile
        private var instance: AiraAudioFocusManager? = null

        fun getInstance(context: Context): AiraAudioFocusManager {
            return instance ?: synchronized(this) {
                instance ?: AiraAudioFocusManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var ttsFocusRequest: AudioFocusRequest? = null
    private var sttFocusRequest: AudioFocusRequest? = null
    private var isTtsFocusHeld = false
    private var isSttFocusHeld = false

    fun requestTtsFocus(onFocusLost: (() -> Unit)? = null): Boolean {
        if (audioManager == null) return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        when (focusChange) {
                            AudioManager.AUDIOFOCUS_LOSS,
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                                Log.d(TAG, "TTS Audio Focus lost ($focusChange)")
                                onFocusLost?.invoke()
                            }
                        }
                    }
                    .build()

                ttsFocusRequest = focusRequest
                val result = audioManager.requestAudioFocus(focusRequest)
                isTtsFocusHeld = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                Log.d(TAG, "Requested TTS Audio Focus: granted = $isTtsFocusHeld")
                isTtsFocusHeld
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            onFocusLost?.invoke()
                        }
                    },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                isTtsFocusHeld = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                isTtsFocusHeld
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error requesting TTS audio focus", e)
            true
        }
    }

    fun releaseTtsFocus() {
        if (audioManager == null || !isTtsFocusHeld) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ttsFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            isTtsFocusHeld = false
            ttsFocusRequest = null
            Log.d(TAG, "Released TTS Audio Focus")
        } catch (e: Throwable) {
            Log.e(TAG, "Error releasing TTS audio focus", e)
        }
    }

    fun requestSttFocus(onFocusLost: (() -> Unit)? = null): Boolean {
        if (audioManager == null) return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            Log.d(TAG, "STT Audio Focus lost ($focusChange)")
                            onFocusLost?.invoke()
                        }
                    }
                    .build()

                sttFocusRequest = focusRequest
                val result = audioManager.requestAudioFocus(focusRequest)
                isSttFocusHeld = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                Log.d(TAG, "Requested STT Audio Focus: granted = $isSttFocusHeld")
                isSttFocusHeld
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            onFocusLost?.invoke()
                        }
                    },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
                isSttFocusHeld = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                isSttFocusHeld
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error requesting STT audio focus", e)
            true
        }
    }

    fun releaseSttFocus() {
        if (audioManager == null || !isSttFocusHeld) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sttFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            isSttFocusHeld = false
            sttFocusRequest = null
            Log.d(TAG, "Released STT Audio Focus")
        } catch (e: Throwable) {
            Log.e(TAG, "Error releasing STT audio focus", e)
        }
    }
}
