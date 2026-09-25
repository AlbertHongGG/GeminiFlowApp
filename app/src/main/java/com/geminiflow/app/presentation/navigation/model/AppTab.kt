package com.geminiflow.app.presentation.navigation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部導航分頁資料規格（純圖標極簡設計，專注無障礙與圖標表達）。
 */
data class TabItemSpec(
    val icon: ImageVector,
    val contentDescription: String
)

/**
 * 主畫面核心分頁枚舉 (Single Source of Truth)。
 */
enum class AppTab(val spec: TabItemSpec) {
    DASHBOARD(TabItemSpec(Icons.Default.Dns, "主介面")),
    SANDBOX(TabItemSpec(Icons.Default.Terminal, "沙盒")),
    SETTINGS(TabItemSpec(Icons.Default.Tune, "設定"))
}
