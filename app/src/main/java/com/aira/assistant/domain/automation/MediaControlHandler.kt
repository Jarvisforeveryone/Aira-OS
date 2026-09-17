package com.aira.assistant.domain.automation

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import com.aira.assistant.service.AiraAccessibilityService

/**
 * Handles media playback, camera operations, and gallery interactions.
 */
class MediaControlHandler(
    private val context: Context,
    private val a11yProvider: () -> AiraAccessibilityService?
) {
    private val tag = "MediaControlHandler"
    private val a11y: AiraAccessibilityService? get() = a11yProvider()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private fun sendMediaKeyEvent(keyCode: Int): Boolean {
        return try {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager?.dispatchMediaKeyEvent(downEvent)
            audioManager?.dispatchMediaKeyEvent(upEvent)
            true
        } catch (e: Exception) {
            Log.e(tag, "sendMediaKeyEvent failed for $keyCode", e)
            false
        }
    }

    fun playMusic(): Boolean {
        Log.d(tag, "playMusic")
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    fun pauseMusic(): Boolean {
        Log.d(tag, "pauseMusic")
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)
    }

    fun stopMusic(): Boolean {
        Log.d(tag, "stopMusic")
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP)
    }

    fun nextSong(): Boolean {
        Log.d(tag, "nextSong")
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previousSong(): Boolean {
        Log.d(tag, "previousSong")
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun openCamera(): Boolean {
        Log.d(tag, "openCamera")
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(tag, "openCamera failed", e)
            false
        }
    }

    fun takePhoto(): Boolean {
        Log.d(tag, "takePhoto")
        if (!openCamera()) return false
        Thread.sleep(1500)
        return sendMediaKeyEvent(KeyEvent.KEYCODE_CAMERA)
    }

    fun recordVideo(): Boolean {
        Log.d(tag, "recordVideo")
        return try {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(tag, "recordVideo failed", e)
            false
        }
    }

    fun openGallery(): Boolean {
        Log.d(tag, "openGallery")
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(tag, "openGallery failed", e)
            false
        }
    }
}
