package com.geminiflow.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.theme.AppColors

/**
 * 沉浸式下拉抽屜彈窗容器元件。
 * 提供 32dp 頂部大圓角、純白底色、拖曳指示條、置中標題與彈性操作區。
 *
 * 核心幾何約束架構：
 * 抽屜外部保持全視窗測量，高度上限約束在內部 Content Column 執行（預設上限 80% 螢幕高）。
 * 確保抽屜永遠嚴格貼齊螢幕底緣（Bottom-anchored），徹底杜絕懸空與頂到最頂部的問題。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeightRatio: Float = 0.80f,
    trailingAction: (@Composable () -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * maxHeightRatio).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.surfaceLight,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = null,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 32.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(48.dp)
                    .height(5.dp)
                    .background(Color(0xFFD1D5DB), RoundedCornerShape(2.5.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (trailingAction != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color(0xFF64748B)
                    )

                    Box(
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        trailingAction()
                    }
                }
            } else {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            content()
        }
    }
}

/**
 * 適用於表單編輯的 ImmersiveBottomSheet 多載，右上角預設提供打勾確認按鈕。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeightRatio: Float = 0.80f,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) = ImmersiveBottomSheet(
    title = title,
    onDismiss = onDismiss,
    trailingAction = {
        IconButton(
            onClick = onConfirm,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "確認儲存",
                tint = Color(0xFF334155),
                modifier = Modifier.size(30.dp)
            )
        }
    },
    modifier = modifier,
    maxHeightRatio = maxHeightRatio,
    sheetState = sheetState,
    content = content
)
