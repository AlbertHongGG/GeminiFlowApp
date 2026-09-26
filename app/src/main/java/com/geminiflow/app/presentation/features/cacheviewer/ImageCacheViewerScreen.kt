package com.geminiflow.app.presentation.features.cacheviewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.components.PremiumConfigHeader
import com.geminiflow.app.presentation.components.navigationSafeBottomPadding
import com.geminiflow.app.presentation.features.cacheviewer.components.CachedImageGridItem
import com.geminiflow.app.presentation.features.cacheviewer.components.MonthSectionHeader
import com.geminiflow.app.presentation.theme.AppColors

@Composable
fun ImageCacheViewerScreen(
    viewModel: ImageCacheViewerViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CacheViewerHeader()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AppColors.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else if (uiState.monthGroups.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CacheViewerHeader()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Collections,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.2f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "目前無快取圖片",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.textPrimaryLight.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "經由對話所生成的圖片將會在此集中管理與預覽",
                                fontSize = 13.sp,
                                color = Color.Black.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = navigationSafeBottomPadding(extraPadding = 32.dp)
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                        key = "header_premium_config"
                    ) {
                        CacheViewerHeader()
                    }

                    uiState.monthGroups.forEach { group ->
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "header_${group.yearMonthKey}"
                        ) {
                            MonthSectionHeader(group = group)
                        }

                        items(
                            items = group.items,
                            key = { it.filename }
                        ) { item ->
                            CachedImageGridItem(
                                item = item,
                                onClick = { onNavigateToDetail(item.filename) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CacheViewerHeader(
    modifier: Modifier = Modifier
) {
    PremiumConfigHeader(
        title = "圖床快取",
        subtitle = "IMAGE CACHE VIEWER",
        modifier = modifier.statusBarsPadding()
    )
}
