package com.geminiflow.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.geminiflow.app.presentation.theme.AppColors

/**
 * ImmersiveScaffold: Ported directly from LensWise (lib/core/widgets/immersive_scaffold.dart)
 * Provides standard background (AppColors.backgroundLight) and statusBarsPadding,
 * enforcing the gesture-only minimalist layout without traditional AppBars.
 */
@Composable
fun ImmersiveScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.backgroundLight)
            .statusBarsPadding(),
        content = content
    )
}
