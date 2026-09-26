package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.SecurePrefs
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurePrefsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSecurePrefsBasicReadWrite() {
        val prefs = SecurePrefs.getEncryptedSharedPreferences(context, "test_secure_prefs_unit")
        prefs.edit()
            .putString("auth_token", "sec_token_val_12345")
            .putInt("api_port", 8080)
            .putBoolean("is_encrypted", true)
            .apply()

        assertEquals("sec_token_val_12345", prefs.getString("auth_token", null))
        assertEquals(8080, prefs.getInt("api_port", 0))
        assertTrue(prefs.getBoolean("is_encrypted", false))
    }

    @Test
    fun testMissingKeyReturnsDefault() {
        val prefs = SecurePrefs.getEncryptedSharedPreferences(context, "test_missing_key_prefs")
        assertNull(prefs.getString("non_existent_key", null))
        assertEquals("default_val", prefs.getString("non_existent_key", "default_val"))
        assertEquals(999, prefs.getInt("non_existent_int", 999))
    }

    @Test
    fun testInMemoryFallbackSharedPreferences() {
        val inMemPrefs = SecurePrefs.InMemorySharedPreferences("fallback_test")
        inMemPrefs.edit()
            .putString("secret_key", "memory_only_credential_999")
            .putLong("session_expiry", 987654321L)
            .putFloat("accuracy", 0.99f)
            .putBoolean("active", true)
            .putStringSet("providers", setOf("gemini", "groq"))
            .apply()

        assertEquals("memory_only_credential_999", inMemPrefs.getString("secret_key", null))
        assertEquals(987654321L, inMemPrefs.getLong("session_expiry", 0L))
        assertEquals(0.99f, inMemPrefs.getFloat("accuracy", 0.0f), 0.001f)
        assertTrue(inMemPrefs.getBoolean("active", false))
        assertEquals(setOf("gemini", "groq"), inMemPrefs.getStringSet("providers", null))

        // Test removal
        inMemPrefs.edit().remove("secret_key").apply()
        assertNull(inMemPrefs.getString("secret_key", null))

        // Test clear
        inMemPrefs.edit().clear().apply()
        assertFalse(inMemPrefs.contains("session_expiry"))
        assertTrue(inMemPrefs.all.isEmpty())
    }

    @Test
    fun testLegacyMigrationMechanism() {
        val legacyName = "legacy_unencrypted_store"
        val unencrypted = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        unencrypted.edit()
            .putString("legacy_key", "legacy_value_to_migrate")
            .putInt("legacy_counter", 42)
            .apply()

        // Requesting encrypted shared prefs with same name triggers migration
        val securePrefs = SecurePrefs.getEncryptedSharedPreferences(context, legacyName)
        assertEquals("legacy_value_to_migrate", securePrefs.getString("legacy_key", null))
        assertEquals(42, securePrefs.getInt("legacy_counter", 0))

        // Legacy plaintext prefs must be wiped
        val reloadedUnencrypted = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        assertTrue(reloadedUnencrypted.all.isEmpty())
    }
}
