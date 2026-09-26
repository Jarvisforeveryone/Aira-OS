package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.AiraAudioFocusManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceConcurrencyTest {

    private lateinit var context: Context
    private lateinit var audioFocusManager: AiraAudioFocusManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        audioFocusManager = AiraAudioFocusManager.getInstance(context)
    }

    @Test
    fun testAudioFocusSingleton() {
        val instance1 = AiraAudioFocusManager.getInstance(context)
        val instance2 = AiraAudioFocusManager.getInstance(context)
        assertSame(instance1, instance2)
    }

    @Test
    fun testTtsAudioFocusRequestAndRelease() {
        val requested = audioFocusManager.requestTtsFocus()
        assertTrue(requested)

        // Releasing TTS focus should run without error
        audioFocusManager.releaseTtsFocus()
    }

    @Test
    fun testSttAudioFocusRequestAndRelease() {
        val requested = audioFocusManager.requestSttFocus()
        assertTrue(requested)

        audioFocusManager.releaseSttFocus()
    }
}
