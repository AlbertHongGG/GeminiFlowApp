package com.geminiflow.app.presentation.navigation

import androidx.compose.runtime.MonotonicFrameClock
import com.geminiflow.app.presentation.navigation.components.CupertinoMotionState
import com.geminiflow.app.presentation.navigation.components.CupertinoSwipeBackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CupertinoSwipeBackStateTest {

    private val testClock = object : MonotonicFrameClock {
        private var time = 0L
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            time += 16_000_000L // Simulate 16ms per frame
            return onFrame(time)
        }
    }

    private val testScope = CoroutineScope(Dispatchers.Unconfined + testClock)

    @Test
    fun testInitialState() {
        val state = CupertinoSwipeBackState(testScope)
        assertEquals(CupertinoMotionState.IDLE, state.motionState)
        assertFalse(state.isTransitioning)
        assertEquals(0f, state.currentOffset, 0.001f)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun testDragStartValidation() {
        val state = CupertinoSwipeBackState(testScope)
        state.screenWidthPx = 1080f

        // When backStackSize <= 1, edge drag must be rejected
        val acceptedOnRoot = state.onDragStart(backStackSize = 1)
        assertFalse(acceptedOnRoot)
        assertEquals(CupertinoMotionState.IDLE, state.motionState)

        // When backStackSize > 1 and screenWidthPx > 0, drag is accepted
        val acceptedOnSubScreen = state.onDragStart(backStackSize = 2)
        assertTrue(acceptedOnSubScreen)
        assertEquals(CupertinoMotionState.DRAGGING, state.motionState)
        assertTrue(state.isTransitioning)
    }

    @Test
    fun testDragOffsetAndClamping() {
        val state = CupertinoSwipeBackState(testScope)
        state.screenWidthPx = 1000f
        assertTrue(state.onDragStart(backStackSize = 2))

        // Normal drag right - synchronous direct tracking
        state.setDragOffset(300f)
        assertEquals(300f, state.currentOffset, 0.001f)
        assertEquals(0.3f, state.progress, 0.001f)

        // Exceed screen width -> clamped
        state.setDragOffset(1500f)
        assertEquals(1000f, state.currentOffset, 0.001f)
        assertEquals(1.0f, state.progress, 0.001f)

        // Negative drag -> clamped to 0
        state.setDragOffset(-500f)
        assertEquals(0f, state.currentOffset, 0.001f)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun testDragCancelRestoresIdle() = runBlocking(testClock) {
        val state = CupertinoSwipeBackState(this)
        state.screenWidthPx = 1000f
        assertTrue(state.onDragStart(backStackSize = 2))
        state.setDragOffset(200f)

        state.onDragCancel()
        assertEquals(CupertinoMotionState.IDLE, state.motionState)
        assertFalse(state.isTransitioning)
        assertEquals(0f, state.currentOffset, 0.001f)
    }

    @Test
    fun testSettleSpringBackBelowThreshold() = runBlocking(testClock) {
        val state = CupertinoSwipeBackState(this)
        state.screenWidthPx = 1000f
        assertTrue(state.onDragStart(backStackSize = 2))
        state.setDragOffset(200f) // 20% (< 35%), low velocity

        var committedPop = false
        state.onDragEnd(velocityX = 100f) {
            committedPop = true
        }

        assertFalse("Below threshold should NOT trigger pop", committedPop)
        assertEquals(CupertinoMotionState.IDLE, state.motionState)
        assertEquals(0f, state.currentOffset, 0.001f)
    }

    @Test
    fun testSettlePopAboveThreshold() = runBlocking(testClock) {
        val state = CupertinoSwipeBackState(this)
        state.screenWidthPx = 1000f
        assertTrue(state.onDragStart(backStackSize = 2))
        state.setDragOffset(400f) // 40% (> 35%)

        var committedPop = false
        state.onDragEnd(velocityX = 200f) {
            committedPop = true
        }

        assertTrue("Above threshold must trigger pop", committedPop)
        assertEquals(CupertinoMotionState.IDLE, state.motionState)
        assertEquals(0f, state.currentOffset, 0.001f)
    }

    @Test
    fun testSettlePopHighVelocity() = runBlocking(testClock) {
        val state = CupertinoSwipeBackState(this)
        state.screenWidthPx = 1000f
        assertTrue(state.onDragStart(backStackSize = 2))
        state.setDragOffset(100f) // Only 10%, but high fling velocity > 800px/s

        var committedPop = false
        state.onDragEnd(velocityX = 1200f) {
            committedPop = true
        }

        assertTrue("High fling velocity must trigger pop", committedPop)
        assertEquals(CupertinoMotionState.IDLE, state.motionState)
        assertEquals(0f, state.currentOffset, 0.001f)
    }
}
