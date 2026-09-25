package com.geminiflow.app.presentation.navigation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.geminiflow.app.presentation.navigation.model.AppTab

/**
 * 點對點定向視差平滑分頁過渡容器 (NavTabTransitionContainer)。
 *
 * 架構亮點：
 * 1. 點對點直接切換（Point-to-Point）：從 Tab 0 切換至 Tab 2 時，直接過渡，
 *    完全不加載、不渲染、不佈局中間無關分頁（根除 JIT 編譯與無效重組）。
 * 2. 1/6 螢幕輕量視差滑移（Slide ~60dp）+ 平滑淡入（220ms）：
 *    大廠旗艦級動效曲線，具備明確空間方向感，同時保持極致輕快迅捷。
 * 3. 狀態完整持久化（SaveableStateHolder）：
 *    智慧保留各分頁的滾動位置、輸入框文字與內部狀態，離開分頁時停止繪製，零 GPU/CPU 負擔。
 */
@Composable
fun NavTabTransitionContainer(
    activeTab: AppTab,
    modifier: Modifier = Modifier,
    content: @Composable (AppTab) -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    AnimatedContent(
        targetState = activeTab,
        transitionSpec = {
            val isForward = targetState.ordinal > initialState.ordinal
            val slideOffsetFraction = 6 // 1/6 螢幕寬度視差平移 (約 60dp)

            val enterSlide = slideInHorizontally(
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                initialOffsetX = { fullWidth -> if (isForward) fullWidth / slideOffsetFraction else -fullWidth / slideOffsetFraction }
            ) + fadeIn(
                animationSpec = tween(durationMillis = 220, easing = LinearEasing)
            )

            val exitSlide = slideOutHorizontally(
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                targetOffsetX = { fullWidth -> if (isForward) -fullWidth / slideOffsetFraction else fullWidth / slideOffsetFraction }
            ) + fadeOut(
                animationSpec = tween(durationMillis = 180, easing = LinearEasing)
            )

            enterSlide togetherWith exitSlide
        },
        modifier = modifier.fillMaxSize(),
        label = "NavTabTransition"
    ) { tab ->
        saveableStateHolder.SaveableStateProvider(tab) {
            content(tab)
        }
    }
}
