package com.geminiflow.app.presentation.navigation.model


/**
 * 全域導航狀態模型，作為單一真實來源 (Single Source of Truth)。
 * 納入分頁 (activeTab) 與堆疊 (backStack)，保證生命週期與轉場下狀態永不丟失。
 */
data class NavigationState(
    val activeTab: AppTab = AppTab.DASHBOARD,
    val backStack: List<AppRoute> = listOf(AppRoute.Main)
) {
    val currentRoute: AppRoute
        get() = backStack.lastOrNull() ?: AppRoute.Main

    val previousRoute: AppRoute?
        get() = if (backStack.size > 1) backStack[backStack.size - 2] else null

    val canPop: Boolean
        get() = backStack.size > 1
}
