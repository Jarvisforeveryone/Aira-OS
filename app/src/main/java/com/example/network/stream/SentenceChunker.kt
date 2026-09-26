package com.example.network.stream

import java.util.regex.Pattern

/**
 * Splits streaming token deltas into complete natural sentences in real-time.
 * Enables zero-latency text-to-speech pipelining while tokens are still streaming.
 */
class SentenceChunker {
    private val buffer = StringBuilder()
    private var sentenceCounter = 0

    // Match punctuation followed by space or newline, or double newline
    private val boundaryPattern = Pattern.compile("(?<=[.!?\\n])\\s+")

    /**
     * Appends a new delta text chunk and returns any newly completed sentences.
     */
    @Synchronized
    fun append(delta: String): List<String> {
        buffer.append(delta)
        val text = buffer.toString()

        // Check if there are sentence terminators followed by whitespace or at end
        val sentences = mutableListOf<String>()
        var searchStart = 0

        for (i in 0 until text.length) {
            val char = text[i]
            if (char == '.' || char == '!' || char == '?' || char == '\n' || char == '—') {
                // Heuristic: check if this is not a decimal number (e.g., 3.14)
                if (char == '.' && i > 0 && i < text.length - 1 && text[i - 1].isDigit() && text[i + 1].isDigit()) {
                    continue
                }

                // Check if followed by whitespace, newline or end of buffer with sufficient length
                val hasTrailingWhitespace = (i + 1 < text.length && text[i + 1].isWhitespace())
                val isSentenceLongEnough = (i + 1 - searchStart) >= 12

                if (hasTrailingWhitespace && isSentenceLongEnough) {
                    val candidate = text.substring(searchStart, i + 1).trim()
                    if (candidate.isNotBlank()) {
                        sentences.add(candidate)
                        searchStart = i + 1
                    }
                }
            }
        }

        if (searchStart > 0) {
            val remaining = text.substring(searchStart).trimStart()
            buffer.setLength(0)
            buffer.append(remaining)
        }

        return sentences
    }

    /**
     * Flushes any remaining text in the buffer when the stream completes.
     */
    @Synchronized
    fun flush(): String? {
        val remaining = buffer.toString().trim()
        buffer.setLength(0)
        return if (remaining.isNotBlank()) remaining else null
    }

    @Synchronized
    fun reset() {
        buffer.setLength(0)
        sentenceCounter = 0
    }
}
