package com.geminiflow.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.geminiflow.app.presentation.features.cacheviewer.components.ZoomableImageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomableImageStateTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun testInitialState() {
        val state = ZoomableImageState(testScope)
        assertEquals(1f, state.scale.value, 0.001f)
        assertEquals(Offset.Zero, state.offset.value)
        assertEquals(false, state.isZoomed)
    }

    @Test
    fun testFocalInvariantZoom_KeepsCentroidStationary() = runBlocking {
        val state = ZoomableImageState(testScope)
        // Viewport: 1000 x 1000, Center: (500, 500)
        state.updateDimensions(
            viewport = IntSize(1000, 1000),
            imageWidth = 1000,
            imageHeight = 1000
        )

        // Pinch at (800, 500), which is +300 px to the right of center
        val centroid = Offset(800f, 500f)
        val zoomFactor = 1.2f

        state.onTransform(centroid = centroid, pan = Offset.Zero, zoom = zoomFactor)

        assertEquals(1.2f, state.scale.value, 0.001f)
        // Focal invariant formula: T = T_old - (centroid - center) * (effectiveZoom - 1)
        // T = 0 - (300) * 0.2 = -60
        assertEquals(-60f, state.offset.value.x, 1f)
        assertEquals(0f, state.offset.value.y, 1f)

        // Content point under finger before zoom was at screen 800 (relative 300)
        // After zoom: screen = center (500) + content (300) * scale (1.2) + offset (-60) = 500 + 360 - 60 = 800!
        val center = 500f
        val contentRelative = 300f
        val newScreenX = center + contentRelative * state.scale.value + state.offset.value.x
        assertEquals(800f, newScreenX, 1f)
    }

    @Test
    fun testClampingLimits() {
        val state = ZoomableImageState(testScope)
        state.updateDimensions(
            viewport = IntSize(1000, 1000),
            imageWidth = 1000,
            imageHeight = 1000
        )

        // At 1x scale, clamping must be Offset.Zero
        val clamped1x = state.clampOffset(Offset(200f, 200f), 1f)
        assertEquals(0f, clamped1x.x, 0.001f)
        assertEquals(0f, clamped1x.y, 0.001f)

        // At 2x scale, max offset is (2000 - 1000) / 2 = 500
        val clamped2x = state.clampOffset(Offset(800f, -800f), 2f)
        assertEquals(500f, clamped2x.x, 0.001f)
        assertEquals(-500f, clamped2x.y, 0.001f)
    }

    @Test
    fun testDoubleTapZoom() = runBlocking {
        val state = ZoomableImageState(testScope)
        state.updateDimensions(
            viewport = IntSize(1000, 1000),
            imageWidth = 1000,
            imageHeight = 1000
        )

        // Double tap at center
        state.handleDoubleTap(Offset(500f, 500f), animate = false)

        assertEquals(2.5f, state.scale.value, 0.01f)
        assertEquals(0f, state.offset.value.x, 0.01f)
        assertEquals(0f, state.offset.value.y, 0.01f)
        assertTrue(state.isZoomed)

        // Second double tap resets
        state.handleDoubleTap(Offset(500f, 500f), animate = false)

        assertEquals(1f, state.scale.value, 0.01f)
        assertEquals(Offset.Zero, state.offset.value)
    }
}
