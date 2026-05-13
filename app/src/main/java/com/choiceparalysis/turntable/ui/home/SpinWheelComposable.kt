package com.choiceparalysis.turntable.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpinWheel(
    options: List<String>,
    rotationDegrees: Float,
    isSpinning: Boolean,
    colorScheme: WheelColorScheme,
    modifier: Modifier = Modifier,
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(rotationDegrees) {
        if (rotationDegrees > 0) {
            animatable.snapTo(0f)
            animatable.animateTo(
                targetValue = rotationDegrees,
                animationSpec = tween(
                    durationMillis = 3000,
                    easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
                )
            )
        }
    }

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    Box(
        modifier = modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        // Wheel with shadow
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = minOf(centerX, centerY) - 10f

            // Draw shadow beneath wheel
            drawCircle(
                color = Color.Black.copy(alpha = 0.15f),
                radius = radius + 4f,
                center = Offset(centerX + 2f, centerY + 4f)
            )

            rotate(degrees = animatable.value) {
                val segmentAngle = 360f / options.size

                options.forEachIndexed { index, option ->
                    val startAngle = index * segmentAngle - 90f
                    val color = colorScheme.getColorForIndex(index)

                    // Draw segment
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = segmentAngle,
                        useCenter = true,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2)
                    )

                    // Draw border (3dp thickness)
                    drawArc(
                        color = colorScheme.borderColor,
                        startAngle = startAngle,
                        sweepAngle = segmentAngle,
                        useCenter = true,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw text
                    val textAngle = startAngle + segmentAngle / 2
                    val textRadius = radius * 0.65f
                    val textX = centerX + cos(Math.toRadians(textAngle.toDouble())).toFloat() * textRadius
                    val textY = centerY + sin(Math.toRadians(textAngle.toDouble())).toFloat() * textRadius

                    textPaint.color = colorScheme.textColor.copy(alpha = 0.9f).toArgb()
                    drawContext.canvas.nativeCanvas.drawText(option, textX, textY + 12f, textPaint)
                }
            }

            // Outer border
            drawCircle(
                color = colorScheme.borderColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Triangle indicator at top
        TriangleIndicator(
            color = colorScheme.indicatorColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
                .size(width = 24.dp, height = 20.dp)
        )
    }
}

@Composable
private fun TriangleIndicator(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Shadow
        val shadowPath = Path().apply {
            moveTo(width / 2f + 1f, height + 2f)
            lineTo(1f, 1f + 2f)
            lineTo(width - 1f, 1f + 2f)
            close()
        }
        drawPath(
            path = shadowPath,
            color = Color.Black.copy(alpha = 0.2f),
            style = Fill
        )

        // Triangle pointing down
        val trianglePath = Path().apply {
            moveTo(width / 2f, height)
            lineTo(0f, 0f)
            lineTo(width, 0f)
            close()
        }
        drawPath(
            path = trianglePath,
            color = color,
            style = Fill
        )
    }
}

private fun Color.toArgb(): Int {
    val alpha = (this.alpha * 255).toInt()
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
