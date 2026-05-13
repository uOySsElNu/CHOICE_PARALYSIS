package com.choiceparalysis.turntable.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private val wheelColors = listOf(
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF673AB7), // Deep Purple
    Color(0xFF3F51B5), // Indigo
    Color(0xFF2196F3), // Blue
    Color(0xFF00BCD4), // Cyan
    Color(0xFF009688), // Teal
    Color(0xFF4CAF50), // Green
    Color(0xFFFFEB3B), // Yellow
    Color(0xFFFF9800), // Orange
)

@Composable
fun SpinWheel(
    options: List<String>,
    rotationDegrees: Float,
    isSpinning: Boolean,
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
                    easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
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
        // Wheel
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = minOf(centerX, centerY) - 10f

            rotate(degrees = animatable.value) {
                val segmentAngle = 360f / options.size

                options.forEachIndexed { index, option ->
                    val startAngle = index * segmentAngle - 90f
                    val color = wheelColors[index % wheelColors.size]

                    // Draw segment
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = segmentAngle,
                        useCenter = true,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2)
                    )

                    // Draw border
                    drawArc(
                        color = Color.White,
                        startAngle = startAngle,
                        sweepAngle = segmentAngle,
                        useCenter = true,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 2f)
                    )

                    // Draw text
                    val textAngle = startAngle + segmentAngle / 2
                    val textRadius = radius * 0.65f
                    val textX = centerX + cos(Math.toRadians(textAngle.toDouble())).toFloat() * textRadius
                    val textY = centerY + sin(Math.toRadians(textAngle.toDouble())).toFloat() * textRadius

                    drawContext.canvas.nativeCanvas.drawText(option, textX, textY + 12f, textPaint)
                }
            }

            // Center circle
            drawCircle(
                color = Color.White,
                radius = 30f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color(0xFF6650a4),
                radius = 25f,
                center = Offset(centerX, centerY)
            )
        }

        // Pointer at top
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "指针",
            tint = Color(0xFFE91E63),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(40.dp)
        )
    }
}
