package com.geminiflow.app.presentation.features.playground

import com.geminiflow.app.presentation.features.playground.streaming.StreamingTypewriterEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingTypewriterEngineTest {

    @Test
    fun testPacesTextProgressively() = runBlocking {
        val outputs = mutableListOf<String>()
        val engine = StreamingTypewriterEngine(
            scope = this,
            baseDelayMs = 10L,
            onTextEmitted = { outputs.add(it) }
        )

        engine.enqueue("Hello")
        // Wait 25ms: should have emitted 1 or 2 characters, not full string
        delay(25L)
        assertTrue(outputs.isNotEmpty())
        assertTrue(outputs.first().length < 5)

        // Wait to finish
        delay(100L)
        assertEquals("Hello", engine.getEmittedText())

        engine.cancel()
    }

    @Test
    fun testAdaptiveCatchUpOnLargeQueue() = runBlocking {
        val outputs = mutableListOf<String>()
        val engine = StreamingTypewriterEngine(
            scope = this,
            baseDelayMs = 5L,
            onTextEmitted = { outputs.add(it) }
        )

        val longText = "A".repeat(150)
        engine.enqueue(longText)

        // Advance 30ms: because queue > 100, each tick consumes multiple characters
        delay(35L)
        assertTrue(outputs.last().length > 15)

        engine.complete()
        engine.awaitCompletion()
        assertEquals(150, engine.getEmittedText().length)
    }

    @Test
    fun testCompletesCleanlyWhenQueueDrained() = runBlocking {
        var finalResult = ""
        val engine = StreamingTypewriterEngine(
            scope = this,
            baseDelayMs = 5L,
            onTextEmitted = { finalResult = it }
        )

        engine.enqueue("World")
        engine.complete()
        engine.awaitCompletion()

        assertEquals("World", finalResult)
    }

    @Test
    fun testCancelStopsTypingImmediately() = runBlocking {
        val outputs = mutableListOf<String>()
        val engine = StreamingTypewriterEngine(
            scope = this,
            baseDelayMs = 10L,
            onTextEmitted = { outputs.add(it) }
        )

        engine.enqueue("This is a long sentence that should be interrupted.")
        delay(35L)
        val countBeforeCancel = outputs.size
        engine.cancel()

        delay(80L)
        // No new emissions after cancel
        assertEquals(countBeforeCancel, outputs.size)
    }
}
