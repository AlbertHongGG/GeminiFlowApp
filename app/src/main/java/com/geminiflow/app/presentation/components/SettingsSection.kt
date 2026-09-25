package com.geminiflow.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.theme.AppColors

/**
 * SettingsSection: 100% directly ported from LensWise (lib/features/settings/presentation/widgets/settings_section.dart)
 * Single white rounded 16dp container with soft 10dp blur shadow and indented dividers.
 */
@Composable
fun SettingsSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textSecondaryLight,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 22.dp)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color.Black.copy(alpha = 0.04f),
                    ambientColor = Color.Black.copy(alpha = 0.03f)
                ),
            shape = RoundedCornerShape(16.dp),
            color = AppColors.surfaceLight
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * SettingsDivider: Standard 1px divider with indent 56dp and endIndent 16dp from LensWise.
 */
@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
        thickness = 1.dp,
        color = Color.Black.copy(alpha = 0.06f)
    )
}
