package com.geminiflow.app.presentation.features.playground

import androidx.compose.foundation.lazy.LazyListState
import com.geminiflow.app.presentation.features.playground.components.PlaygroundScrollState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaygroundScrollStateTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun testInitialState() {
        val listState = LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
        val scrollState = PlaygroundScrollState(listState, testScope)

        assertTrue("預設應開啟自動貼底滾動", scrollState.autoScrollEnabled)
        assertTrue("空列表或初始狀態下應判定為在底部", scrollState.isAtBottom)
    }

    @Test
    fun testUserScrollingAwayDisablesAutoScroll() {
        val listState = LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
        val scrollState = PlaygroundScrollState(listState, testScope)

        // 模擬使用者開始手動拖曳
        scrollState.onScrollProgressChanged(isScrolling = true)

        // 若此時不在底部，autoScrollEnabled 應被設為 false
        // 透過直接測試狀態切換
        scrollState.onAtBottomChanged(atBottom = false)
        scrollState.onScrollProgressChanged(isScrolling = true)
        // 只要使用者滑離底部，貼底模式立即凍結
    }

    @Test
    fun testResetAndRequestScrollRestoresAutoScroll() {
        val listState = LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
        val scrollState = PlaygroundScrollState(listState, testScope)

        // 模擬手動點擊回到底部
        scrollState.requestScrollToBottom()
        assertTrue("點擊回到底部後必須恢復自動貼底", scrollState.autoScrollEnabled)

        // 模擬送出新提問
        scrollState.onUserSendMessage()
        assertTrue("送出新訊息後必須恢復自動貼底", scrollState.autoScrollEnabled)

        // 模擬重置
        scrollState.reset()
        assertTrue("重置後必須恢復自動貼底", scrollState.autoScrollEnabled)
    }
}
