package com.example

import com.example.network.stream.SentenceChunker
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit test suite for SentenceChunker.
 * Tests streaming natural language token chunking, punctuation boundaries,
 * decimal protection, buffer accumulation, and flushing.
 */
class SentenceChunkerTest {

    private lateinit var chunker: SentenceChunker

    @Before
    fun setUp() {
        chunker = SentenceChunker()
        chunker.reset()
    }

    @Test
    fun testNormalSentenceBoundaries() {
        val sentences = chunker.append("Hello world this is Aira. ")
        assertEquals(1, sentences.size)
        assertEquals("Hello world this is Aira.", sentences[0])
    }

    @Test
    fun testExclamationAndQuestionMarks() {
        val sentences1 = chunker.append("How are you doing today? ")
        assertEquals(1, sentences1.size)
        assertEquals("How are you doing today?", sentences1[0])

        val sentences2 = chunker.append("System online and operational! ")
        assertEquals(1, sentences2.size)
        assertEquals("System online and operational!", sentences2[0])
    }

    @Test
    fun testNewlineBoundaries() {
        val sentences = chunker.append("First line of response text.\nSecond line follows right here.\n")
        assertTrue(sentences.isNotEmpty())
        assertTrue(sentences.any { it.contains("First line") })
    }

    @Test
    fun testStreamingPartialTokens() {
        // Feed tokens one by one
        val tokens = listOf("Good ", "morning, ", "sir. ", "All ", "systems ", "are ", "functioning ", "properly.")
        val collectedSentences = mutableListOf<String>()

        for (token in tokens) {
            val res = chunker.append(token)
            collectedSentences.addAll(res)
        }

        val flushed = chunker.flush()
        if (flushed != null) {
            collectedSentences.add(flushed)
        }

        assertEquals(2, collectedSentences.size)
        assertTrue(collectedSentences[0].startsWith("Good morning"))
        assertTrue(collectedSentences[1].contains("functioning properly"))
    }

    @Test
    fun testIncompleteSentenceBufferingAndFlush() {
        val res = chunker.append("This is an unfinished sentence without punctuation")
        assertTrue(res.isEmpty())

        val flushed = chunker.flush()
        assertNotNull(flushed)
        assertEquals("This is an unfinished sentence without punctuation", flushed)

        // After flush, buffer should be empty
        assertNull(chunker.flush())
    }

    @Test
    fun testEmptyInputAndWhitespace() {
        val res1 = chunker.append("")
        assertTrue(res1.isEmpty())

        val res2 = chunker.append("   \n   \t  ")
        assertTrue(res2.isEmpty())

        assertNull(chunker.flush())
    }

    @Test
    fun testDecimalNumbersProtection() {
        // "Version 3.14 is ready. " should not break on "3."
        val sentences = chunker.append("Version 3.14 is completely ready now. ")
        assertEquals(1, sentences.size)
        assertEquals("Version 3.14 is completely ready now.", sentences[0])
    }

    @Test
    fun testResetBehavior() {
        chunker.append("Some partial pending buffer text")
        chunker.reset()
        assertNull(chunker.flush())

        val fresh = chunker.append("Fresh new sentence is here now. ")
        assertEquals(1, fresh.size)
        assertEquals("Fresh new sentence is here now.", fresh[0])
    }

    @Test
    fun testUnicodeAndSpecialPunctuation() {
        val sentences = chunker.append("Temperature is 25°C — everything is running smoothly. ")
        assertTrue(sentences.isNotEmpty())
        assertTrue(sentences.any { it.contains("25°C") })
    }
}
