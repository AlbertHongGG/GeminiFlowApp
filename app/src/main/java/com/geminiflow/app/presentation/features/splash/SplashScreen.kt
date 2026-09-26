package com.geminiflow.app.presentation.features.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "M E K U R U",
    subtitle: String = "COMICS"
) {
    val progress = remember { Animatable(0f) }

    val easeOutCubic = remember { CubicBezierEasing(0.215f, 0.61f, 0.355f, 1.0f) }
    val easeOut = remember { FastOutSlowInEasing }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = LinearEasing)
        )
        delay(150)
        onAnimationFinished()
    }

    val t = progress.value

    val logoFade = when {
        t < 0.2f -> 0f
        t > 0.6f -> 1f
        else -> easeOut.transform((t - 0.2f) / 0.4f)
    }

    val logoScale = when {
        t < 0.2f -> 0.9f
        t > 0.7f -> 1f
        else -> 0.9f + (0.1f * easeOutCubic.transform((t - 0.2f) / 0.5f))
    }

    val titleUnfold = when {
        t < 0.5f -> 0f
        t > 0.9f -> 1f
        else -> easeOutCubic.transform((t - 0.5f) / 0.4f)
    }

    val titleFade = when {
        t < 0.6f -> 0f
        t >= 1.0f -> 1f
        else -> easeOut.transform((t - 0.6f) / 0.4f)
    }

    val slideOffsetPx = (-24f * (1f - titleUnfold)).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale)
                    .alpha(logoFade)
            )

            Box(
                modifier = Modifier
                    .clipToBounds()
                    .alpha(titleFade)
                    .offset { IntOffset(0, slideOffsetPx.roundToPx()) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .width(120.dp * titleFade)
                            .height(1.dp)
                            .background(Color(0xFF1D1D1F).copy(alpha = 0.30f * titleFade))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1D1D1F),
                        letterSpacing = 8.0.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF86868B),
                        letterSpacing = 4.0.sp
                    )
                }
            }
        }
    }
}
