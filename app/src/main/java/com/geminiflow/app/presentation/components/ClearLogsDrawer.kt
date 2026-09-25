package com.geminiflow.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 底部滑動抹除抽屜 (ClearLogsDrawer)，復刻 LensWise 的 _showClearAllDrawer 設計。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearLogsDrawer(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val isDark = isSystemInDarkTheme()
    val sheetBackgroundColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val handleColor = if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.12f)
    val titleColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = sheetBackgroundColor,
        dragHandle = null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 48.dp)
        ) {
            // 頂部把手指示條
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(handleColor)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 抽屜副標題
            Text(
                text = title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.0.sp,
                color = titleColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 滑動抹除核心按鈕
            SwipeToObliterateButton(
                title = "SLIDE TO WIPE",
                activeColor = Color(0xFFEF4444),
                onConfirmed = {
                    onConfirm()
                }
            )
        }
    }
}
