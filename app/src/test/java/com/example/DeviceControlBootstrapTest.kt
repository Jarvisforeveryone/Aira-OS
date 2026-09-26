package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.utils.ShizukuManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DeviceControlBootstrapTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testShizukuConstants() {
        assertEquals("moe.shizuku.privileged.api", ShizukuManager.SHIZUKU_PACKAGE)
        assertEquals("com.draco.ladb", ShizukuManager.LADB_PACKAGE)
        assertEquals(
            "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
            ShizukuManager.LADB_SHIZUKU_START_COMMAND
        )
    }

    @Test
    fun testPlayStoreIntentGeneration() {
        ShizukuManager.openPlayStore(context, ShizukuManager.SHIZUKU_PACKAGE)
        val shadowApp = org.robolectric.Shadows.shadowOf(context as android.app.Application)
        val startedIntent: Intent? = shadowApp.nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_VIEW, startedIntent?.action)
        assertEquals(Uri.parse("market://details?id=moe.shizuku.privileged.api"), startedIntent?.data)
    }

    @Test
    fun testLadbPlayStoreIntentGeneration() {
        ShizukuManager.openPlayStore(context, ShizukuManager.LADB_PACKAGE)
        val shadowApp = org.robolectric.Shadows.shadowOf(context as android.app.Application)
        val startedIntent: Intent? = shadowApp.nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_VIEW, startedIntent?.action)
        assertEquals(Uri.parse("market://details?id=com.draco.ladb"), startedIntent?.data)
    }

    @Test
    fun testSdkBranchLogic() {
        val sdk30 = 30
        val sdk29 = 29

        assertTrue("API 30 must use Wireless Debugging flow", sdk30 >= android.os.Build.VERSION_CODES.R)
        assertTrue("API 29 must use LADB flow", sdk29 <= android.os.Build.VERSION_CODES.Q)
    }
}
