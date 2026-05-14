package com.choiceparalysis.turntable.ui.coindice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.choiceparalysis.turntable.ui.components.StandardEasing
import com.choiceparalysis.turntable.viewmodel.CoinSide
import kotlinx.coroutines.launch

@Composable
fun Coin3DFlip(
    result: CoinSide?,
    isAnimating: Boolean,
    headsImage: ImageBitmap?,
    tailsImage: ImageBitmap?,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    val rotationAnim = remember { Animatable(0f) }
    val translationYAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            val randomOffset = (0..360).random().toFloat()

            rotationAnim.snapTo(0f)
            translationYAnim.snapTo(0f)
            scaleAnim.snapTo(1f)

            launch {
                translationYAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 2000
                        0f at 0
                        -200f at 600 using StandardEasing.EaseOutQuart
                        0f at 1200 using StandardEasing.EaseInQuart
                        -30f at 1500
                        0f at 2000 using StandardEasing.EaseOutQuart
                    }
                )
            }

            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 2000
                        1f at 0
                        1.2f at 600 using StandardEasing.EaseOutQuart
                        1f at 1200 using StandardEasing.EaseInQuart
                    }
                )
            }

            rotationAnim.animateTo(
                targetValue = 1800f + randomOffset,
                animationSpec = keyframes {
                    durationMillis = 2000
                    0f at 0
                    360f at 600 using StandardEasing.EaseOutCubic
                    1440f at 1400 using StandardEasing.EaseOutQuart
                    (1800f + randomOffset) at 2000 using StandardEasing.EaseOutQuart
                }
            )

            onAnimationComplete()
        }
    }

    val showHeads = result == CoinSide.HEADS || result == null
    val displayImage = if (showHeads) headsImage else tailsImage
    val backgroundColor = if (showHeads) Color(0xFFFFD700) else Color(0xFFC0C0C0)

    Canvas(
        modifier = modifier
            .size(150.dp)
            .graphicsLayer {
                rotationY = rotationAnim.value % 360f
                translationY = translationYAnim.value
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                cameraDistance = 12f * density
            }
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.width / 2

        // Draw coin shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = radius * 0.9f,
            center = Offset(centerX, centerY + 10f)
        )

        // Draw coin body
        drawCircle(
            color = backgroundColor,
            radius = radius,
            center = Offset(centerX, centerY)
        )

        // Draw image if available
        displayImage?.let { image ->
            val imageSize = IntSize((radius * 1.8f).toInt(), (radius * 1.8f).toInt())
            drawImage(
                image = image,
                dstSize = imageSize,
                dstOffset = IntOffset(
                    (centerX - imageSize.width / 2f).toInt(),
                    (centerY - imageSize.height / 2f).toInt()
                )
            )
        }
    }
}
