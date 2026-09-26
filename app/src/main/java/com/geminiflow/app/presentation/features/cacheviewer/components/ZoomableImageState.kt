package com.geminiflow.app.presentation.features.cacheviewer.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 具有真實仿射變換焦點不變量 (Focal Invariant) 數學模型的高效縮放與平移狀態管理器。
 * 支援雙指捏合以觸控質心為焦點縮放、雙擊以點擊實體座標為目標展開放大、以及嚴密的邊界動態約束裁切。
 */
@Stable
class ZoomableImageState(
    private val coroutineScope: CoroutineScope
) {
    val scale = Animatable(1f)
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)

    var viewportSize: IntSize = IntSize.Zero
        private set
    var intrinsicWidth: Int = 0
        private set
    var intrinsicHeight: Int = 0
        private set

    val minScale: Float = 1f
    val maxScale: Float = 5f

    val isZoomed: Boolean
        get() = scale.value > 1.05f

    fun updateDimensions(viewport: IntSize, imageWidth: Int, imageHeight: Int) {
        this.viewportSize = viewport
        if (imageWidth > 0) this.intrinsicWidth = imageWidth
        if (imageHeight > 0) this.intrinsicHeight = imageHeight
    }

    fun clampOffset(targetOffset: Offset, currentScale: Float): Offset {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) {
            return Offset.Zero
        }

        val imgW = if (intrinsicWidth > 0) intrinsicWidth.toFloat() else viewportSize.width.toFloat()
        val imgH = if (intrinsicHeight > 0) intrinsicHeight.toFloat() else viewportSize.height.toFloat()

        val imageAspectRatio = imgW / imgH.coerceAtLeast(1f)
        val viewportAspectRatio = viewportSize.width.toFloat() / viewportSize.height.coerceAtLeast(1).toFloat()

        val (fittedWidth, fittedHeight) = if (imageAspectRatio > viewportAspectRatio) {
            viewportSize.width.toFloat() to (viewportSize.width / imageAspectRatio)
        } else {
            (viewportSize.height * imageAspectRatio) to viewportSize.height.toFloat()
        }

        val scaledWidth = fittedWidth * currentScale
        val scaledHeight = fittedHeight * currentScale

        val maxOffsetX = maxOf(0f, (scaledWidth - viewportSize.width) / 2f)
        val maxOffsetY = maxOf(0f, (scaledHeight - viewportSize.height) / 2f)

        return Offset(
            x = targetOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
            y = targetOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
        )
    }

    /**
     * 處理雙指縮放與平移手勢。
     * @param centroid 雙指接觸點之實體螢幕質心座標
     * @param pan 本次手勢位移量
     * @param zoom 本次手勢縮放變化比率
     */
    fun onTransform(centroid: Offset, pan: Offset, zoom: Float) {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return

        coroutineScope.launch {
            val currentScale = scale.value
            val newScale = (currentScale * zoom).coerceIn(minScale, maxScale)
            val effectiveZoom = newScale / currentScale

            val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
            val focal = centroid - center
            val currentOffset = offset.value

            // 焦點不變量補償公式：保持手指下覆蓋的圖檔內容點在螢幕上的物理座標完全恆等
            val targetOffset = currentOffset + pan - (focal - currentOffset) * (effectiveZoom - 1f)
            val clampedOffset = clampOffset(targetOffset, newScale)

            scale.snapTo(newScale)
            offset.snapTo(clampedOffset)
        }
    }

    /**
     * 處理雙擊縮放手勢。
     * @param tapPosition 雙擊觸控點實體座標，以該點為中心對焦放大至 2.5x 或平滑重置為 1x
     * @param animate 是否平滑動畫過渡（單元測試或特定情境可設為 false 即時生效）
     */
    fun handleDoubleTap(tapPosition: Offset, animate: Boolean = true) {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return

        coroutineScope.launch {
            if (scale.value > 1.05f) {
                if (animate) {
                    launch { scale.animateTo(1f, tween(260)) }
                    launch { offset.animateTo(Offset.Zero, tween(260)) }
                } else {
                    scale.snapTo(1f)
                    offset.snapTo(Offset.Zero)
                }
            } else {
                val targetScale = 2.5f
                val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                val focal = tapPosition - center
                // 以點擊目標為錨點展開放大
                val targetOffset = -focal * (targetScale - 1f)
                val clamped = clampOffset(targetOffset, targetScale)
                if (animate) {
                    launch { scale.animateTo(targetScale, tween(260)) }
                    launch { offset.animateTo(clamped, tween(260)) }
                } else {
                    scale.snapTo(targetScale)
                    offset.snapTo(clamped)
                }
            }
        }
    }

    fun reset() {
        coroutineScope.launch {
            scale.snapTo(1f)
            offset.snapTo(Offset.Zero)
        }
    }
}

@Composable
fun rememberZoomableImageState(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): ZoomableImageState {
    return remember {
        ZoomableImageState(coroutineScope)
    }
}
