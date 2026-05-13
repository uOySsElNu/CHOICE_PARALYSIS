package com.choiceparalysis.turntable.ui.coindice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choiceparalysis.turntable.viewmodel.CoinSide

@Composable
fun CoinFlip(
    result: CoinSide?,
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            rotationAnim.snapTo(0f)
            rotationAnim.animateTo(
                targetValue = 1800f + (0..360).random().toFloat(), // 5+ full rotations
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
                )
            )
        }
    }

    val showHeads = result == CoinSide.HEADS || result == null
    val backgroundColor = if (showHeads) Color(0xFFFFD700) else Color(0xFFC0C0C0)
    val emoji = if (showHeads) "👑" else "🦅"

    Box(
        modifier = modifier
            .size(150.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(backgroundColor)
            .graphicsLayer {
                rotationY = rotationAnim.value % 360f
                cameraDistance = 12f * density
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 60.sp
        )
    }
}
