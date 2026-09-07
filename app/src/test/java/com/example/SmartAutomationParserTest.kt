package com.example

import com.example.utils.SmartAutomationParser
import com.example.utils.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAutomationParserTest {

    @Test
    fun testParseBedtimeSpeech() {
        val speech = "When I say Goodnight, dim screen to 10%, turn off wifi and mute sound"
        val parsed = SmartAutomationParser.parseNaturalLanguage(speech)

        assertEquals("goodnight", parsed.triggerPhrase)
        assertEquals(TriggerType.VOICE_PHRASE, parsed.triggerType)
        assertTrue(parsed.actions.isNotEmpty())
        assertTrue(parsed.actions.any { it.contains("dim screen") })
        assertTrue(parsed.actions.any { it.contains("wifi off") })
        assertTrue(parsed.actions.any { it.contains("silent") })
    }

    @Test
    fun testParseBatteryLowTrigger() {
        val speech = "When battery drops below 20%, turn off bluetooth and dim screen"
        val parsed = SmartAutomationParser.parseNaturalLanguage(speech)

        assertEquals(TriggerType.BATTERY_LEVEL, parsed.triggerType)
        assertEquals("battery low", parsed.triggerPhrase)
        assertTrue(parsed.actions.any { it.contains("bluetooth off") })
        assertTrue(parsed.actions.any { it.contains("dim screen") })
    }

    @Test
    fun testParseAppTrigger() {
        val speech = "When I open YouTube, unmute sound and set brightness to 80%"
        val parsed = SmartAutomationParser.parseNaturalLanguage(speech)

        assertEquals(TriggerType.APP_OPENED, parsed.triggerType)
        assertTrue(parsed.actions.any { it.contains("brighten screen") })
        assertTrue(parsed.actions.any { it.contains("normal") })
    }

    @Test
    fun testPredefinedTemplatesPresent() {
        val templates = SmartAutomationParser.PREDEFINED_TEMPLATES
        assertTrue(templates.size >= 8)
        assertTrue(templates.any { it.id == "template_bedtime" })
        assertTrue(templates.any { it.id == "template_morning" })
        assertTrue(templates.any { it.id == "template_cinema" })
        assertTrue(templates.any { it.id == "template_battery_saver" })
    }

    @Test
    fun testFriendlyLabelsGeneration() {
        val (emoji1, label1) = SmartAutomationParser.getFriendlyActionLabel("dim screen to 10%")
        assertEquals("🔆", emoji1)

        val (emoji2, label2) = SmartAutomationParser.getFriendlyActionLabel("set sound mode to silent")
        assertEquals("🔇", emoji2)

        val (emoji3, label3) = SmartAutomationParser.getFriendlyActionLabel("turn wifi off")
        assertEquals("📶", emoji3)
    }
}
