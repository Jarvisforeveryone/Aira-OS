package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MultiKeyManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MultiKeyManagerTest {

    private lateinit var context: Context
    private lateinit var keyManager: MultiKeyManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        keyManager = MultiKeyManager.getInstance(context)
        // Clean up test provider keys
        val existing = keyManager.getKeys("TEST_PROVIDER")
        for (k in existing) {
            keyManager.removeKeyByValue("TEST_PROVIDER", k)
        }
    }

    @Test
    fun testKeyAddAndRetrieval() {
        val fakeKey1 = "AIzaSyFakeKeyTest123456789"
        val fakeKey2 = "AIzaSyFakeKeyTest987654321"

        assertTrue(keyManager.addKey("TEST_PROVIDER", fakeKey1))
        assertTrue(keyManager.addKey("TEST_PROVIDER", fakeKey2))

        val keys = keyManager.getKeys("TEST_PROVIDER")
        assertEquals(2, keys.size)
        assertTrue(keys.contains(fakeKey1))
        assertTrue(keys.contains(fakeKey2))
        assertEquals(2, keyManager.getKeyCount("TEST_PROVIDER"))
    }

    @Test
    fun testDuplicateKeyPrevention() {
        val fakeKey = "AIzaSyUniqueKeyTest0001"
        assertTrue(keyManager.addKey("TEST_PROVIDER", fakeKey))
        assertFalse(keyManager.addKey("TEST_PROVIDER", fakeKey))
        assertEquals(1, keyManager.getKeyCount("TEST_PROVIDER"))
    }

    @Test
    fun testKeyRemoval() {
        val fakeKey1 = "AIzaSyRemoveTest1"
        val fakeKey2 = "AIzaSyRemoveTest2"
        keyManager.addKey("TEST_PROVIDER", fakeKey1)
        keyManager.addKey("TEST_PROVIDER", fakeKey2)

        assertTrue(keyManager.removeKeyByValue("TEST_PROVIDER", fakeKey1))
        val keys = keyManager.getKeys("TEST_PROVIDER")
        assertEquals(1, keys.size)
        assertEquals(fakeKey2, keys[0])

        assertTrue(keyManager.removeKey("TEST_PROVIDER", 0))
        assertEquals(0, keyManager.getKeyCount("TEST_PROVIDER"))
    }

    @Test
    fun testRoundRobinSelection() {
        val keyA = "Key_A_11111111"
        val keyB = "Key_B_22222222"
        keyManager.addKey("TEST_PROVIDER", keyA)
        keyManager.addKey("TEST_PROVIDER", keyB)

        val first = keyManager.getNextKey("TEST_PROVIDER")
        val second = keyManager.getNextKey("TEST_PROVIDER")
        val third = keyManager.getNextKey("TEST_PROVIDER")

        assertNotNull(first)
        assertNotNull(second)
        assertNotNull(third)
        assertNotEquals(first, second)
        assertEquals(first, third)
    }

    @Test
    fun testCooldownHandling() {
        val keyA = "Key_Cooldown_A"
        val keyB = "Key_Cooldown_B"
        keyManager.addKey("TEST_PROVIDER", keyA)
        keyManager.addKey("TEST_PROVIDER", keyB)

        // Mark keyA in cooldown for 1 hour
        keyManager.markCooldown("TEST_PROVIDER", keyA, 3600000L)

        // Next key should be keyB since keyA is cooled down
        val nextKey = keyManager.getNextKey("TEST_PROVIDER")
        assertEquals(keyB, nextKey)

        // When both are cooled down
        keyManager.markCooldown("TEST_PROVIDER", keyB, 3600000L)
        val exhausted = keyManager.getNextKey("TEST_PROVIDER")
        assertNull(exhausted)
    }

    @Test
    fun testKeyStatusPersistence() {
        val key = "Key_Status_Test"
        keyManager.addKey("TEST_PROVIDER", key)
        keyManager.setKeyStatus("TEST_PROVIDER", key, isSuccess = true, message = "200 OK")

        val status = keyManager.getKeyStatus("TEST_PROVIDER", key)
        assertTrue(status.isSuccess)
        assertEquals("200 OK", status.message)
    }
}
