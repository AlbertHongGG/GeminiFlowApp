package com.geminiflow.app.presentation.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 螢幕最左邊界右滑返回手勢監聽元件。
 * 放置於畫面最左側 (0 ~ edgeWidth)，使用者自邊緣向右滑動即流暢觸發上一頁 (onNavigateBack)。
 */
@Composable
fun EdgeSwipeBackDetector(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    edgeWidth: Dp = 40.dp
) {
    var totalDrag by remember { mutableFloatStateOf(0f) }
    var hasTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(edgeWidth)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        hasTriggered = false
                    },
                    onDragEnd = {
                        totalDrag = 0f
                        hasTriggered = false
                    },
                    onDragCancel = {
                        totalDrag = 0f
                        hasTriggered = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (!hasTriggered && dragAmount > 0f) {
                            totalDrag += dragAmount
                            if (totalDrag > 35f) {
                                hasTriggered = true
                                onNavigateBack()
                            }
                        }
                    }
                )
            }
    )
}
