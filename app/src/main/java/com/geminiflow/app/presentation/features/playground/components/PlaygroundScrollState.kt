package com.geminiflow.app.presentation.features.playground.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 負責管理 Playground 聊天視口滾動行為的狀態控制器。
 * 封裝即時幾何計算、貼底狀態機判斷、手勢衝突防護與平滑滾動控制。
 */
@Stable
class PlaygroundScrollState(
    val lazyListState: LazyListState,
    private val coroutineScope: CoroutineScope
) {
    /**
     * 是否處於貼底自動滾動模式。
     * 當使用者在底部時為 true；使用者主動向上滾動離開底部時為 false。
     */
    var autoScrollEnabled by mutableStateOf(true)
        private set

    /**
     * 即時計算列表是否處於最底部（含容錯緩衝區）。
     */
    val isAtBottom: Boolean by derivedStateOf {
        if (!lazyListState.canScrollForward) return@derivedStateOf true
        val layoutInfo = lazyListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return@derivedStateOf true

        val lastVisibleItem = visibleItems.last()
        val isLastItem = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
        val isBottomWithinViewport = (lastVisibleItem.offset + lastVisibleItem.size) <= (layoutInfo.viewportEndOffset + 32)
        isLastItem && isBottomWithinViewport
    }

    /**
     * 當使用者手動滾動進程改變時調用。
     */
    fun onScrollProgressChanged(isScrolling: Boolean) {
        if (isScrolling) {
            // 使用者正在拖曳中，若不在底部則解除貼底鎖定
            if (!isAtBottom) {
                autoScrollEnabled = false
            }
        } else {
            // 停止拖曳或慣性滑動結束時，若已在底部則自動恢復貼底鎖定
            if (isAtBottom) {
                autoScrollEnabled = true
            }
        }
    }

    /**
     * 當即時底部狀態改變時調用。
     */
    fun onAtBottomChanged(atBottom: Boolean) {
        if (atBottom) {
            autoScrollEnabled = true
        }
    }

    /**
     * 串流高頻追加內容時調用：無動畫瞬間同步至最底端錨點。
     * 若使用者正在手動拖曳或已脫離底部，則安全略過，絕不打斷手勢。
     */
    suspend fun scrollToBottomImmediate(force: Boolean = false) {
        if (!force && (!autoScrollEnabled || lazyListState.isScrollInProgress)) {
            return
        }
        val totalCount = lazyListState.layoutInfo.totalItemsCount
        if (totalCount <= 0) return
        val targetIndex = totalCount - 1
        try {
            lazyListState.scrollToItem(targetIndex)
        } catch (_: Exception) {
        }
    }

    /**
     * 觸發滾動至最底端。
     * @param smooth 是否啟用平滑動畫
     * @param force 是否忽略貼底鎖定狀態強制滾動
     */
    fun scrollToBottom(smooth: Boolean = false, force: Boolean = false) {
        if (!force && (!autoScrollEnabled || lazyListState.isScrollInProgress)) {
            return
        }
        val totalCount = lazyListState.layoutInfo.totalItemsCount
        if (totalCount <= 0) return
        val targetIndex = totalCount - 1
        coroutineScope.launch {
            try {
                if (smooth) {
                    lazyListState.animateScrollToItem(targetIndex)
                } else {
                    lazyListState.scrollToItem(targetIndex)
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 使用者點擊懸浮「回到底部」按鈕時調用。
     */
    fun requestScrollToBottom() {
        autoScrollEnabled = true
        scrollToBottom(smooth = true, force = true)
    }

    /**
     * 使用者送出新提問時調用：鎖定貼底並平滑滾動至末端。
     */
    fun onUserSendMessage() {
        autoScrollEnabled = true
        scrollToBottom(smooth = true, force = true)
    }

    /**
     * 重置滾動狀態控制器。
     */
    fun reset() {
        autoScrollEnabled = true
    }
}

/**
 * 記憶並綁定 [PlaygroundScrollState] 至 Composable 生命週期。
 */
@Composable
fun rememberPlaygroundScrollState(
    lazyListState: LazyListState = rememberLazyListState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): PlaygroundScrollState {
    val scrollState = remember(lazyListState, coroutineScope) {
        PlaygroundScrollState(lazyListState, coroutineScope)
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                scrollState.onScrollProgressChanged(isScrolling)
            }
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isAtBottom }
            .distinctUntilChanged()
            .collect { atBottom ->
                scrollState.onAtBottomChanged(atBottom)
            }
    }

    return scrollState
}
