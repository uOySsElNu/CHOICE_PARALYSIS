package com.choiceparalysis.turntable.ui.coindice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.choiceparalysis.turntable.ui.components.StandardEasing
import kotlinx.coroutines.launch

@Composable
fun Dice3DRoll(
    value: Int?,
    isAnimating: Boolean,
    faceImages: Map<Int, ImageBitmap?> = emptyMap(),
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    val rotationAnim = remember { Animatable(0f) }
    val rotationXAnim = remember { Animatable(0f) }
    val translationYAnim = remember { Animatable(0f) }
    val translationXAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            val randomOffset = (0..360).random().toFloat()

            rotationAnim.snapTo(0f)
            rotationXAnim.snapTo(0f)
            translationYAnim.snapTo(0f)
            translationXAnim.snapTo(0f)
            scaleAnim.snapTo(1f)

            // Bounce animation (translationY with multiple bounces)
            launch {
                translationYAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 2200
                        0f at 0
                        -250f at 500 using StandardEasing.EaseOutQuart
                        0f at 800 using StandardEasing.EaseInQuart
                        -120f at 1100 using StandardEasing.EaseOutQuart
                        0f at 1350 using StandardEasing.EaseInQuart
                        -40f at 1600
                        0f at 1800
                        -10f at 1950
                        0f at 2200 using StandardEasing.EaseOutQuart
                    }
                )
            }

            // Horizontal drift
            launch {
                translationXAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 2200
                        0f at 0
                        40f at 600 using StandardEasing.EaseOutCubic
                        -20f at 1200
                        10f at 1800
                        0f at 2200 using StandardEasing.EaseOutQuart
                    }
                )
            }

            // Scale animation (grow then settle)
            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 2200
                        1f at 0
                        1.2f at 600 using StandardEasing.EaseOutQuart
                        1f at 1200 using StandardEasing.EaseInQuart
                    }
                )
            }

            // Tilt animation (rotationX)
            launch {
                rotationXAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 2200
                        0f at 0
                        45f at 300
                        -30f at 700
                        20f at 1200
                        0f at 2200 using StandardEasing.EaseOutQuart
                    }
                )
            }

            // Main rotation animation (multiple full rotations)
            rotationAnim.animateTo(
                targetValue = 1080f + randomOffset,
                animationSpec = keyframes {
                    durationMillis = 2200
                    0f at 0
                    360f at 500 using StandardEasing.EaseOutCubic
                    900f at 1100 using StandardEasing.EaseOutQuart
                    (1080f + randomOffset) at 2200 using StandardEasing.EaseOutQuart
                }
            )

            onAnimationComplete()
        }
    }

    val displayValue = value ?: 1
    val faceImage = faceImages[displayValue]
    val diceColor = Color(0xFFFFFFFF)
    val dotColor = Color(0xFF000000)

    Canvas(
        modifier = modifier
            .size(120.dp)
            .graphicsLayer {
                rotationZ = rotationAnim.value
                rotationX = rotationXAnim.value
                translationX = translationXAnim.value
                translationY = translationYAnim.value
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                cameraDistance = 12f * density
            }
    ) {
        val cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        val diceSize = Size(size.width, size.height)
        val diceOffset = Offset(0f, 0f)

        // Draw dice shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = Offset(4.dp.toPx(), 6.dp.toPx()),
            size = diceSize,
            cornerRadius = cornerRadius
        )

        // Draw dice body
        drawRoundRect(
            color = diceColor,
            topLeft = diceOffset,
            size = diceSize,
            cornerRadius = cornerRadius
        )

        // Draw image or dots
        if (faceImage != null) {
            val imageSize = IntSize(
                (size.width * 0.8f).toInt(),
                (size.height * 0.8f).toInt()
            )
            drawImage(
                image = faceImage,
                dstSize = imageSize,
                dstOffset = IntOffset(
                    ((size.width - imageSize.width) / 2f).toInt(),
                    ((size.height - imageSize.height) / 2f).toInt()
                )
            )
        } else {
            drawDiceDots(displayValue, dotColor)
        }
    }
}

private fun DrawScope.drawDiceDots(value: Int, color: Color) {
    val dotRadius = 8.dp.toPx()
    val padding = 28.dp.toPx()
    val centerX = size.width / 2
    val centerY = size.height / 2
    val leftX = padding
    val rightX = size.width - padding
    val topY = padding
    val bottomY = size.height - padding

    val positions = when (value) {
        1 -> listOf(Offset(centerX, centerY))
        2 -> listOf(Offset(leftX, topY), Offset(rightX, bottomY))
        3 -> listOf(Offset(leftX, topY), Offset(centerX, centerY), Offset(rightX, bottomY))
        4 -> listOf(Offset(leftX, topY), Offset(rightX, topY), Offset(leftX, bottomY), Offset(rightX, bottomY))
        5 -> listOf(Offset(leftX, topY), Offset(rightX, topY), Offset(centerX, centerY), Offset(leftX, bottomY), Offset(rightX, bottomY))
        6 -> listOf(Offset(leftX, topY), Offset(rightX, topY), Offset(leftX, centerY), Offset(rightX, centerY), Offset(leftX, bottomY), Offset(rightX, bottomY))
        else -> emptyList()
    }

    positions.forEach { position ->
        drawCircle(
            color = color,
            radius = dotRadius,
            center = position
        )
    }
}
