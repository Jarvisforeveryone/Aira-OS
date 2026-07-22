package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aira", appName)
  }

  @Test
  fun `update and get customizable wake word`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPrefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
    
    // Set initial preference
    sharedPrefs.edit().putString("wake_word", "Jarvis").commit()
    
    val savedWakeWord = sharedPrefs.getString("wake_word", "Hey Aira")
    assertEquals("Jarvis", savedWakeWord)
  }
}
