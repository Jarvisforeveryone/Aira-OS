package com.example

import com.example.data.ChatMessage
import com.example.data.Memory
import org.junit.Assert.*
import org.junit.Test

/**
 * Production local unit tests for Aira application logic and models.
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testMemoryModel() {
        val memory = Memory(
            id = 1L,
            factText = "User lives in San Francisco",
            source = "voice",
            createdAt = 1000L
        )
        assertEquals(1L, memory.id)
        assertEquals("User lives in San Francisco", memory.factText)
        assertEquals("voice", memory.source)
        assertEquals(1000L, memory.createdAt)
    }

    @Test
    fun testChatMessageModel() {
        val chatMessage = ChatMessage(
            id = 101L,
            sender = "user",
            message = "Hello Aira",
            timestamp = 1000L,
            isOffline = false
        )
        assertEquals(101L, chatMessage.id)
        assertEquals("user", chatMessage.sender)
        assertEquals("Hello Aira", chatMessage.message)
        assertEquals(1000L, chatMessage.timestamp)
        assertFalse(chatMessage.isOffline)
    }
}
