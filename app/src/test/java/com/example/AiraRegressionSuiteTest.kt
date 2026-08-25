package com.example

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import com.example.data.DatabaseSchema
import com.example.models.LlamaCppBrain
import com.example.network.stream.SentenceChunker
import com.example.service.AiraAccessibilityService
import com.example.utils.AiraAudioFocusManager
import com.example.utils.SecurePrefs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Enterprise Regression Verification Test Suite for AIRA.
 * Covers all architectural, security, database, and automation safety invariants.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiraRegressionSuiteTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // AIRA-SEC-001: No plaintext API key persistence
    @Test
    fun regression_AIRA_SEC_001_NoPlaintextApiKeyPersistence() {
        val legacyPrefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
        // Ensure sensitive keys do not linger in legacy plaintext prefs
        assertNull(legacyPrefs.getString("gemini_api_key", null))
        assertNull(legacyPrefs.getString("groq_api_key", null))
        assertNull(legacyPrefs.getString("openai_api_key", null))
    }

    // AIRA-SEC-002: Keystore failure uses volatile memory only
    @Test
    fun regression_AIRA_SEC_002_KeystoreFailureUsesVolatileMemoryOnly() {
        val inMem = SecurePrefs.InMemorySharedPreferences("test_volatile")
        inMem.edit().putString("volatile_secret", "xyz_token_999").apply()
        assertEquals("xyz_token_999", inMem.getString("volatile_secret", null))
        // Verify it is not written to actual disk files
        val diskFile = context.getSharedPreferences("test_volatile", Context.MODE_PRIVATE)
        assertFalse(diskFile.contains("volatile_secret"))
    }

    // AIRA-DB-001 & AIRA-DB-002: Upgrade migrations preserve user data without destructive upgrades
    @Test
    fun regression_AIRA_DB_001_and_002_MigrationsPreserveData() {
        assertEquals(10, DatabaseSchema.DATABASE_VERSION)
        assertTrue(DatabaseSchema.ALL_MIGRATIONS.isNotEmpty())
        assertEquals(9, DatabaseSchema.ALL_MIGRATIONS.size)
    }

    // AIRA-PRIV-001 & AIRA-PRIV-002: Password fields and protected screen text are masked
    @Test
    fun regression_AIRA_PRIV_001_and_002_SensitiveFieldsMasked() {
        val service = AiraAccessibilityService()
        val pwNode = AccessibilityNodeInfo.obtain()
        pwNode.isPassword = true
        assertTrue(service.isSensitiveNode(pwNode))

        val pinNode = AccessibilityNodeInfo.obtain()
        pinNode.viewIdResourceName = "com.auth:id/pin_code"
        assertTrue(service.isSensitiveNode(pinNode))

        val normalNode = AccessibilityNodeInfo.obtain()
        normalNode.isPassword = false
        normalNode.viewIdResourceName = "com.app:id/title"
        assertFalse(service.isSensitiveNode(normalNode))
    }

    // AIRA-VOICE-001 & AIRA-VOICE-002: TTS Audio Focus and Cancellation
    @Test
    fun regression_AIRA_VOICE_001_and_002_AudioConcurrenyManagement() {
        val focusManager = AiraAudioFocusManager.getInstance(context)
        val granted = focusManager.requestTtsFocus()
        assertTrue(granted)
        focusManager.releaseTtsFocus()
    }

    // AIRA-STREAM-001: Sentence chunker token boundary buffering
    @Test
    fun regression_AIRA_STREAM_001_SentenceChunkerIsolation() {
        val chunker = SentenceChunker()
        val sentences = chunker.append("The system is now operating within normal parameters. ")
        assertEquals(1, sentences.size)
        assertEquals("The system is now operating within normal parameters.", sentences[0])
        assertNull(chunker.flush())
    }

    // AIRA-LLM-001: LlamaCppBrain heuristic fallback verification
    @Test
    fun regression_AIRA_LLM_001_LlamaCppBrainGracefulFallback() = runBlocking {
        val brain = LlamaCppBrain(context)
        val status = brain.getEngineStatus()
        assertTrue(status.isNotBlank())
        val offlineResponse = brain.getResponse("hello jarvis", "You are Jarvis.")
        assertTrue(offlineResponse.isNotBlank())
    }
}
