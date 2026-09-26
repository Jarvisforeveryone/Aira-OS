package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.network.stream.AiStreamManager
import com.example.network.stream.AiStreamState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiStreamManagerTest {

    private lateinit var context: Context
    private lateinit var streamManager: AiStreamManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        streamManager = AiStreamManager.getInstance(context)
        streamManager.resetState()
    }

    @Test
    fun testInitialStreamState() {
        assertEquals(AiStreamState.Idle, streamManager.streamState.value)
        assertEquals("", streamManager.liveStreamingText.value)
        assertEquals("", streamManager.activeThoughts.value)
        assertFalse(streamManager.isStreamingActive.value)
    }

    @Test
    fun testResetState() {
        streamManager.resetState()
        assertEquals(AiStreamState.Idle, streamManager.streamState.value)
        assertEquals("", streamManager.liveStreamingText.value)
        assertEquals("", streamManager.activeThoughts.value)
        assertFalse(streamManager.isStreamingActive.value)
    }

    @Test
    fun testCancelCurrentStream() {
        streamManager.cancelCurrentStream()
        assertFalse(streamManager.isStreamingActive.value)
        assertEquals(AiStreamState.Idle, streamManager.streamState.value)
    }

    @Test
    fun testStreamManagerSingleton() {
        val instance1 = AiStreamManager.getInstance(context)
        val instance2 = AiStreamManager.getInstance(context)
        assertSame(instance1, instance2)
    }
}
