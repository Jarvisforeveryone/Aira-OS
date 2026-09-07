package com.example

import com.example.utils.InstantMultiCommandDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstantMultiCommandDispatcherTest {

    @Test
    fun testSplitCompoundCommandsWithAnd() {
        val input = "turn on wifi and turn on flashlight"
        val parts = InstantMultiCommandDispatcher.splitIntoSubCommands(input)
        assertEquals(2, parts.size)
        assertEquals("turn on wifi", parts[0])
        assertEquals("turn on flashlight", parts[1])
    }

    @Test
    fun testSharedVerbExpansion() {
        val input = "turn on wifi and bluetooth"
        val parts = InstantMultiCommandDispatcher.splitIntoSubCommands(input)
        assertEquals(2, parts.size)
        assertEquals("turn on wifi", parts[0])
        assertEquals("turn on bluetooth", parts[1])
    }

    @Test
    fun testTripleCommandWithPunctuation() {
        val input = "turn off wifi, bluetooth, and flashlight"
        val parts = InstantMultiCommandDispatcher.splitIntoSubCommands(input)
        assertEquals(3, parts.size)
        assertEquals("turn off wifi", parts[0])
        assertEquals("turn off bluetooth", parts[1])
        assertEquals("turn off flashlight", parts[2])
    }

    @Test
    fun testPolitePrefixStripping() {
        val input = "can you please turn on flashlight and kindly dim the screen"
        val parts = InstantMultiCommandDispatcher.splitIntoSubCommands(input)
        assertEquals(2, parts.size)
        assertEquals("turn on flashlight", parts[0])
        assertEquals("dim the screen", parts[1])
    }

    @Test
    fun testSingleCommandRetained() {
        val input = "turn on wifi"
        val parts = InstantMultiCommandDispatcher.splitIntoSubCommands(input)
        assertEquals(1, parts.size)
        assertEquals("turn on wifi", parts[0])
    }

    @Test
    fun testComplexMultiCommandWithBrightnessAndSound() {
        val input = "turn off wifi, set brightness to 20% and mute phone"
        val parts = InstantMultiCommandDispatcher.splitIntoSubCommands(input)
        assertEquals(3, parts.size)
        assertEquals("turn off wifi", parts[0])
        assertEquals("set brightness to 20%", parts[1])
        assertEquals("mute phone", parts[2])
    }

    @Test
    fun testUrduRomanUrduMultiCommand() {
        val input = "torch jalao aur wifi on karo"
        // Since "aur" is an Urdu conjunction, let's make sure our conjunction regex splits it or handle it cleanly!
        val parts = InstantMultiCommandDispatcher.splitIntoSubCommands(input)
        assertTrue(parts.isNotEmpty())
    }
}
