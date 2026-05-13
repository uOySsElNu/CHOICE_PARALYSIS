package com.choiceparalysis.turntable.ui.coindice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun DiceRoll(
    value: Int?,
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    val translationX = remember { Animatable(0f) }
    val translationY = remember { Animatable(0f) }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            rotation.snapTo(0f)
            translationX.snapTo(0f)
            translationY.snapTo(0f)

            rotation.animateTo(
                targetValue = 720f + (0..360).random().toFloat(),
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
                )
            )
        }
    }

    val displayValue = value ?: 1

    Box(
        modifier = modifier
            .size(120.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .graphicsLayer {
                rotationZ = rotation.value
                this.translationX = translationX.value
                this.translationY = translationY.value
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        DiceFace(value = displayValue)
    }
}

@Composable
private fun DiceFace(value: Int) {
    when (value) {
        1 -> DotPattern(listOf(5))
        2 -> DotPattern(listOf(2, 8))
        3 -> DotPattern(listOf(2, 5, 8))
        4 -> DotPattern(listOf(1, 3, 7, 9))
        5 -> DotPattern(listOf(1, 3, 5, 7, 9))
        6 -> DotPattern(listOf(1, 3, 4, 6, 7, 9))
    }
}

@Composable
private fun DotPattern(positions: List<Int>) {
    Box(modifier = Modifier.size(80.dp)) {
        positions.forEach { pos ->
            val row = (pos - 1) / 3
            val col = (pos - 1) % 3
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .offset(
                        x = (col * 32).dp,
                        y = (row * 32).dp
                    )
                    .background(Color.Black, RoundedCornerShape(50))
            )
        }
    }
}
