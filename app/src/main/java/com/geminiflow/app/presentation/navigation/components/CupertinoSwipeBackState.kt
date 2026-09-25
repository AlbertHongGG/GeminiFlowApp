package com.geminiflow.app.presentation.navigation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.geminiflow.app.presentation.navigation.model.AppRoute
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt

/**
 * 嚴格互斥的導航轉場狀態列舉。
 */
enum class CupertinoMotionState {
    /** 靜止狀態：無任何手勢或轉場動畫正在執行 */
    IDLE,
    /** 推進動畫中：新頁面由 screenWidth 向左滑入至 0 */
    PUSHING,
    /** 邊緣手勢拖曳中：手指正跟隨拖曳頂層頁面（0 ~ screenWidth） */
    DRAGGING,
    /** 放手結算中：根據速度與閥值自動回彈或滑出 */
    SETTLING,
    /** 退棧動畫中：頂層頁面由 0 向右滑出至 screenWidth */
    POPPING
}

/**
 * 企業級純物理運動驅動器 (Layer 1: Pure Motion Driver)。
 *
 * 核心保證：
 * 1. 同步直接賦值 (Synchronous Tracking)：手勢拖曳位移為同步浮點操作，杜絕協程調度時差。
 * 2. 嚴格互斥狀態機 (Deterministic State Machine)：狀態切換封閉自洽，杜絕死鎖。
 * 3. 數學級零閃爍首幀不變量 (Zero-Flash Frame Invariance)。
 */
@Stable
class CupertinoSwipeBackState(
    val coroutineScope: CoroutineScope
) {
    /** 當前狀態機狀態 */
    var motionState by mutableStateOf(CupertinoMotionState.IDLE)
        private set

    /** 螢幕寬度（物理像素），於根容器 layout 時動態校準 */
    var screenWidthPx by mutableFloatStateOf(0f)

    /** 當前頂層畫面水平位移（同步浮點數，絕對貼合實體玻璃座標） */
    var currentOffset by mutableFloatStateOf(0f)
        private set

    /** 推進中的目標路由 */
    var pushTarget by mutableStateOf<AppRoute?>(null)
        private set

    val cupertinoEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    /** 是否正在進行任何形式的轉場或手勢 */
    val isTransitioning: Boolean
        get() = motionState != CupertinoMotionState.IDLE

    /**
     * 當前頂層頁面的向右位移百分比 (0f: 完全處於當前頁, 1f: 完全移出螢幕)
     */
    val progress: Float
        get() = if (screenWidthPx > 0f) (currentOffset / screenWidthPx).coerceIn(0f, 1f) else 0f

    /**
     * 觸發向右推進 (Push) 新頁面。
     * 首幀強制將 currentOffset 鎖定於 screenWidthPx，保證第 0 幀絕無全螢幕閃跳。
     */
    suspend fun push(target: AppRoute, onCommit: (AppRoute) -> Unit) {
        if (isTransitioning || screenWidthPx <= 0f) return

        motionState = CupertinoMotionState.PUSHING
        pushTarget = target
        currentOffset = screenWidthPx

        val anim = Animatable(screenWidthPx)
        anim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 320, easing = cupertinoEasing)
        ) {
            currentOffset = value
        }

        onCommit(target)
        pushTarget = null
        currentOffset = 0f
        motionState = CupertinoMotionState.IDLE
    }

    /**
     * 觸發程式化退棧 (Pop)。
     */
    suspend fun pop(backStackSize: Int, onCommit: () -> Unit) {
        if (isTransitioning || backStackSize <= 1 || screenWidthPx <= 0f) return

        motionState = CupertinoMotionState.POPPING
        currentOffset = 0f

        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = screenWidthPx,
            animationSpec = tween(durationMillis = 280, easing = cupertinoEasing)
        ) {
            currentOffset = value
        }

        onCommit()
        currentOffset = 0f
        motionState = CupertinoMotionState.IDLE
    }

    /**
     * 邊緣滑動手勢捕獲成功時調用。
     * @return 若成功切換為 DRAGGING 狀態返回 true，否則返回 false
     */
    fun onDragStart(backStackSize: Int): Boolean {
        if (motionState != CupertinoMotionState.IDLE || backStackSize <= 1 || screenWidthPx <= 0f) {
            return false
        }
        motionState = CupertinoMotionState.DRAGGING
        return true
    }

    /**
     * 邊緣滑動拖曳位移同步更新（100% 物理玻璃座標，零協程延遲，零反饋震盪）。
     */
    fun setDragOffset(dragDistance: Float) {
        if (motionState != CupertinoMotionState.DRAGGING) return
        currentOffset = dragDistance.coerceIn(0f, screenWidthPx)
    }

    /**
     * 邊緣滑動放手結算。
     * 滑動大於 35% 或向右瞬時速度大於 800px/s 判定為滑出退棧，否則以彈簧物理回彈原點。
     */
    suspend fun onDragEnd(velocityX: Float, onCommitPop: () -> Unit) {
        if (motionState != CupertinoMotionState.DRAGGING) return

        motionState = CupertinoMotionState.SETTLING
        val ratio = if (screenWidthPx > 0f) currentOffset / screenWidthPx else 0f
        val shouldPop = ratio > 0.35f || velocityX > 800f

        if (shouldPop) {
            val remainingDistance = screenWidthPx - currentOffset
            val remainingRatio = if (screenWidthPx > 0f) remainingDistance / screenWidthPx else 0f
            val duration = (remainingRatio * 260).roundToInt().coerceIn(100, 260)

            val anim = Animatable(currentOffset)
            anim.animateTo(
                targetValue = screenWidthPx,
                animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
            ) {
                currentOffset = value
            }

            onCommitPop()
            currentOffset = 0f
            motionState = CupertinoMotionState.IDLE
        } else {
            val anim = Animatable(currentOffset)
            anim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) {
                currentOffset = value
            }
            currentOffset = 0f
            motionState = CupertinoMotionState.IDLE
        }
    }

    /**
     * 手勢中途被系統取消時安全回彈。
     */
    suspend fun onDragCancel() {
        if (motionState != CupertinoMotionState.DRAGGING) return

        motionState = CupertinoMotionState.SETTLING
        val anim = Animatable(currentOffset)
        anim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) {
            currentOffset = value
        }
        currentOffset = 0f
        motionState = CupertinoMotionState.IDLE
    }
}

@Composable
fun rememberCupertinoSwipeBackState(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): CupertinoSwipeBackState {
    return remember(coroutineScope) {
        CupertinoSwipeBackState(coroutineScope)
    }
}
