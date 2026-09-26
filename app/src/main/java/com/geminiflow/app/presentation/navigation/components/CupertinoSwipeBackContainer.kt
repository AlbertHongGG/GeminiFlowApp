package com.geminiflow.app.presentation.navigation.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import com.geminiflow.app.presentation.navigation.contract.CupertinoNavigator
import com.geminiflow.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.geminiflow.app.presentation.navigation.model.AppRoute
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun Modifier.touchShield(enabled: Boolean): Modifier = if (!enabled) this else {
    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            }
        }
    }
}
@Composable
fun CupertinoSwipeBackContainer(
    backStack: List<AppRoute>,
    onPushRoute: (AppRoute) -> Unit,
    onPopRoute: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (route: AppRoute) -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val state = rememberCupertinoSwipeBackState(coroutineScope = scope)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        state.screenWidthPx = screenWidthPx

        val navigator = remember(state, backStack.size) {
            object : CupertinoNavigator {
                override fun push(route: AppRoute) {
                    scope.launch {
                        state.push(route) { onPushRoute(it) }
                    }
                }
                override fun pop() {
                    scope.launch {
                        state.pop(backStack.size) {
                            val popped = backStack.lastOrNull()
                            onPopRoute()
                            if (popped != null) {
                                saveableStateHolder.removeState(popped.key)
                            }
                        }
                    }
                }
            }
        }

        // 系統返回鍵攔截 (BackHandler)
        BackHandler(enabled = backStack.size > 1 && !state.isTransitioning) {
            scope.launch {
                state.pop(backStack.size) {
                    val popped = backStack.lastOrNull()
                    onPopRoute()
                    if (popped != null) {
                        saveableStateHolder.removeState(popped.key)
                    }
                }
            }
        }

        // 計算目前需要參與渲染的底層與頂層路由
        val underlyingRoute: AppRoute? = when {
            state.pushTarget != null -> backStack.lastOrNull()
            backStack.size > 1 -> backStack[backStack.size - 2]
            else -> null
        }

        val topRoute: AppRoute = when {
            state.pushTarget != null -> state.pushTarget!!
            else -> backStack.lastOrNull() ?: AppRoute.Main
        }

        // 統一數學模型驅動之位移與遮罩透明度（C0 連續性保證）
        val topTranslationX = state.currentOffset
        val progress = state.progress
        val underlyingTranslationX = -screenWidthPx * 0.33f * (1f - progress)
        val scrimAlpha = 0.25f * (1f - progress)

        CompositionLocalProvider(LocalCupertinoNavigator provides navigator) {
            // ==========================================
            // Layer 3: Dual-Layer Viewport (雙層隔離視口)
            // ==========================================

            // 1. 底層前一頁畫面 (Underlying Screen) 渲染
            if (underlyingRoute != null && (state.isTransitioning || backStack.size > 1)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = underlyingTranslationX
                        }
                        // 強制加裝輸入護盾：當前存在頂層頁面時，底層 100% 絕對屏蔽任何觸摸與滾動
                        .touchShield(enabled = backStack.size > 1 || state.isTransitioning)
                ) {
                    saveableStateHolder.SaveableStateProvider(key = underlyingRoute.key) {
                        content(underlyingRoute)
                    }

                    // 深色 Scrim 漸層遮罩
                    if (scrimAlpha > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = scrimAlpha))
                        )
                    }
                }
            }

            // 2. 頂層當前畫面 (Top Screen) 渲染：純視覺位移渲染，內部絕不包含任何位移手勢！
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = topTranslationX
                    }
            ) {
                // 左側 16dp 縱深立體陰影（向左投射至底層）
                if (topTranslationX > 0f) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                            .offset(x = (-16).dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.22f)
                                    )
                                )
                            )
                    )
                }

                // 頂層視圖內容本體
                saveableStateHolder.SaveableStateProvider(key = topRoute.key) {
                    content(topRoute)
                }
            }

            // =========================================================================
            // Layer 2: Root-Level Input Engine (根座標手勢引擎)
            // 掛載於【絕對靜止的宿主根節點】(translationX = 0)，座標系恆定，100% 根除座標反饋震盪！
            // =========================================================================
            if (backStack.size > 1 || state.isTransitioning) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(36.dp)
                        .align(Alignment.CenterStart)
                        .pointerInput(state, backStack.size) {
                            val velocityTracker = VelocityTracker()
                            val edgeThresholdPx = 36.dp.toPx()
                            val touchSlop = viewConfiguration.touchSlop

                            coroutineScope {
                                while (isActive) {
                                    awaitPointerEventScope {
                                        // 步驟 A：等待邊緣按下事件 (Down)
                                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                        if (down.position.x > edgeThresholdPx || state.isTransitioning || backStack.size <= 1) {
                                            return@awaitPointerEventScope
                                        }

                                        val startX = down.position.x
                                        val startY = down.position.y
                                        velocityTracker.resetTracking()
                                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                                        var isGestureClaimed = false

                                        try {
                                            // 步驟 B：持續追蹤手指位移與斜率判定
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                                if (!change.pressed) {
                                                    // 手指放開 (Up)
                                                    if (isGestureClaimed) {
                                                        change.consume()
                                                        val velocityX = velocityTracker.calculateVelocity().x
                                                        launch {
                                                            state.onDragEnd(velocityX) {
                                                                val popped = backStack.lastOrNull()
                                                                onPopRoute()
                                                                if (popped != null) {
                                                                    saveableStateHolder.removeState(popped.key)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    break
                                                }

                                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                                val totalDeltaX = change.position.x - startX
                                                val totalDeltaY = change.position.y - startY

                                                if (!isGestureClaimed) {
                                                    // 斜率判定：向右滑動大於 touchSlop 且顯著大於垂直移動
                                                    if (totalDeltaX > touchSlop && totalDeltaX > kotlin.math.abs(totalDeltaY) * 1.1f) {
                                                        isGestureClaimed = state.onDragStart(backStack.size)
                                                        if (isGestureClaimed) {
                                                            change.consume()
                                                            state.setDragOffset(totalDeltaX - touchSlop)
                                                        }
                                                    } else if (kotlin.math.abs(totalDeltaY) > touchSlop) {
                                                        // 垂直滾動主導，立即退出邊緣捕獲，完全讓渡給子列表滾動
                                                        break
                                                    }
                                                } else {
                                                    // 手勢已捕獲：全額消費事件，同步直接更新位移（零延遲，零反饋）
                                                    change.consume()
                                                    state.setDragOffset(totalDeltaX - touchSlop)
                                                }
                                            }
                                        } finally {
                                            // 若中途異常中斷但處於 DRAGGING 狀態，安全回彈原點
                                            if (isGestureClaimed && state.motionState == CupertinoMotionState.DRAGGING) {
                                                launch {
                                                    state.onDragCancel()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                )
            }
        }
    }
}
