package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.AiraAutomationEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiraAutomationEngineTest {

    private lateinit var context: Context
    private lateinit var automationEngine: AiraAutomationEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        automationEngine = AiraAutomationEngine(context)
    }

    @Test
    fun testOpenWebsiteUrlFormatting() {
        // Should handle URL and launch without throwing exception
        val result = automationEngine.openWebsite("google.com")
        assertTrue(result)
    }

    @Test
    fun testVolumeAdjustments() {
        val upResult = automationEngine.volumeUp()
        val downResult = automationEngine.volumeDown()
        assertNotNull(upResult)
        assertNotNull(downResult)
    }

    @Test
    fun testBrightnessAdjustments() {
        val brightnessResult = automationEngine.setBrightness(128)
        assertNotNull(brightnessResult)
    }

    @Test
    fun testMediaVolume() {
        val mediaResult = automationEngine.setMediaVolume(50)
        assertTrue(mediaResult)
    }
}
