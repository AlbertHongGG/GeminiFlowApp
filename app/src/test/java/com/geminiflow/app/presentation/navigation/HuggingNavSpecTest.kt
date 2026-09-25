package com.geminiflow.app.presentation.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.geminiflow.app.presentation.navigation.components.HuggingNavSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HuggingNavSpecTest {

    @Test
    fun defaultSpec_verifiesHuggingCapsuleGeometry() {
        val spec = HuggingNavSpec()

        // 緊緻包裹按鈕，杜絕兩側過寬多餘空白
        assertEquals(8.dp, spec.horizontalPadding)
        assertEquals(6.dp, spec.verticalPadding)
        assertEquals(14.dp, spec.itemSpacing)
        assertEquals(50.dp, spec.itemDiameter)
        assertEquals(24.dp, spec.iconSize)
        assertEquals(32.dp, spec.containerCornerRadius)

        // 計算 3 個項目下的膠囊理論總寬度: 8 + 50 + 14 + 50 + 14 + 50 + 8 = 194.dp
        val estimatedTotalWidth = spec.calculateEstimatedTotalWidth(3)
        assertEquals(194.dp, estimatedTotalWidth)
    }

    @Test
    fun defaultSpec_verifiesSlidingIndicatorOffsets() {
        val spec = HuggingNavSpec()

        // 單一實體滑動圓盤的軌道水平偏移計算
        assertEquals(0.dp, spec.calculateIndicatorOffset(0))
        assertEquals(64.dp, spec.calculateIndicatorOffset(1)) // 50dp + 14dp
        assertEquals(128.dp, spec.calculateIndicatorOffset(2)) // (50dp + 14dp) * 2

        // 負數安全保護
        assertEquals(0.dp, spec.calculateIndicatorOffset(-1))

        // 彈簧阻尼係數驗證
        assertEquals(0.85f, spec.springDampingRatio, 0.001f)
    }

    @Test
    fun defaultSpec_verifiesVisionOsWhitePorcelainPalette() {
        val spec = HuggingNavSpec()

        // Apple VisionOS 晶透白瓷風格配色
        assertEquals(Color.White, spec.activeCircleColor)
        assertEquals(Color(0xFF0F172A), spec.activeIconColor) // 深邃石墨黑
        assertEquals(Color(0xFF64748B), spec.inactiveIconColor) // Slate-500
        assertEquals(Color.Transparent, spec.inactiveCircleColor)

        // 晶透白瓷材質 (高透光，零 GPU 記憶體風暴)
        assertEquals(0.82f, spec.glassBackgroundColor.alpha, 0.01f)
        assertEquals(0.88f, spec.fallbackBackgroundColor.alpha, 0.01f)
        assertEquals(24.dp, spec.blurRadius)
        assertTrue(spec.activeCircleElevation > 0.dp)
    }
}
