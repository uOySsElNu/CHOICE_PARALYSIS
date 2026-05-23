package com.choiceparalysis.turntable.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import com.choiceparalysis.turntable.ui.components.StandardEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.audio.SoundEffect
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SpinWheel(
    options: List<String>,
    weights: List<Int>,
    colorScheme: WheelColorScheme,
    modifier: Modifier = Modifier,
    onSpinResult: (String) -> Unit = {},
    onSpinStart: () -> Unit = {},
    onSpinEnd: () -> Unit = {},
    onResultDragged: () -> Unit = {},
) {
    val audioHaptic = AudioHapticManager.getInstance(LocalContext.current)
    val animatable = remember { Animatable(0f) }
    var settledResult by remember { mutableStateOf<String?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    // Trigger counter for button-initiated spins
    var spinTrigger by remember { mutableIntStateOf(0) }

    // Compute weighted segment angles
    val safeWeights = options.indices.map { weights.getOrElse(it) { 1 } }
    val totalWeight = safeWeights.sum().coerceAtLeast(options.size)
    val segmentAngles = safeWeights.map { (it.toFloat() / totalWeight) * 360f }

    // Helper: which segment is under the indicator
    fun segmentIndexAt(rotation: Float): Int {
        val norm = ((rotation % 360f) + 360f) % 360f
        val indicatorAngle = (360f - norm) % 360f
        var acc = 0f
        for (i in segmentAngles.indices) {
            acc += segmentAngles[i]
            if (indicatorAngle < acc) return i
        }
        return segmentAngles.indices.last
    }

    // Button-initiated spin
    LaunchedEffect(spinTrigger) {
        if (spinTrigger == 0) return@LaunchedEffect
        onSpinStart()
        settledResult = null
        val target = (1440..2160).random().toFloat() + (0..360).random().toFloat()
        animatable.snapTo(0f)
        animatable.animateTo(target, tween(3000, easing = StandardEasing.EaseOutQuart))
        onSpinEnd()
        val idx = segmentIndexAt(animatable.value)
        settledResult = options[idx]
        onSpinResult(options[idx])
        audioHaptic.playFeedback(SoundEffect.SPIN_DING)
    }

    // Result-drag detection: fires AFTER drag stops, with debounce
    // Key includes both isDragging and settledResult so it re-checks after each spin result
    LaunchedEffect(isDragging, settledResult) {
        if (!isDragging && settledResult != null) {
            delay(300)
            val idx = segmentIndexAt(animatable.value)
            if (options.getOrNull(idx) != settledResult) {
                onResultDragged()
                settledResult = null
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val wheelSize = minOf(maxWidth, 300.dp)
        val density = LocalDensity.current

        val textStrokePaint = remember(density) {
            android.graphics.Paint().apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = with(density) { 3.dp.toPx() }
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                color = android.graphics.Color.BLACK
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
        }
        val textFillPaint = remember(density) {
            android.graphics.Paint().apply {
                style = android.graphics.Paint.Style.FILL
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
        }

        Canvas(
            modifier = Modifier
                .size(wheelSize)
                .padding(12.dp)
                .pointerInput(options, weights) {
                    // Use coroutineScope + launch so we can call animatable suspend functions
                    // directly from gesture callbacks — no async lag
                    coroutineScope {
                        var prevAngle = 0f
                        var prevTime = 0L
                        val velocities = mutableListOf<Float>()
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                prevAngle = atan2(offset.y - cy, offset.x - cx)
                                prevTime = System.currentTimeMillis()
                                velocities.clear()
                            },
                            onDrag = { change, _ ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val curAngle = atan2(change.position.y - cy, change.position.x - cx)
                                var delta = Math.toDegrees((curAngle - prevAngle).toDouble()).toFloat()
                                if (delta > 180f) delta -= 360f
                                if (delta < -180f) delta += 360f
                                launch { animatable.snapTo(animatable.value + delta) }
                                prevAngle = curAngle
                                val now = System.currentTimeMillis()
                                val dt = (now - prevTime).coerceAtLeast(1)
                                velocities.add(delta / dt * 1000f)
                                if (velocities.size > 5) velocities.removeAt(0)
                                prevTime = now
                                audioHaptic.tick()
                                change.consume()
                            },
                            onDragEnd = {
                                isDragging = false
                                val avgVelocity = if (velocities.isNotEmpty()) {
                                    velocities.sorted().let {
                                        it.subList(it.size / 4, it.size * 3 / 4).average().toFloat()
                                    }
                                } else 0f
                                if (kotlin.math.abs(avgVelocity) > 400f) {
                                    settledResult = null
                                    launch {
                                        onSpinStart()
                                        val absV = kotlin.math.abs(avgVelocity)
                                        // Enforce minimum spin distance (same as button: ~1440°)
                                        // If velocity too low for meaningful spin, animate to minimum
                                        val minRotation = 1440f
                                        val sign = if (avgVelocity > 0) 1f else -1f
                                        val current = animatable.value
                                        val target = current + sign * maxOf(absV * 1.5f, minRotation)
                                        animatable.animateTo(
                                            target,
                                            tween(maxOf(2000, (absV * 1.5f / 720f * 2000f).toInt()).coerceAtMost(5000),
                                                easing = StandardEasing.EaseOutQuart)
                                        )
                                        onSpinEnd()
                                        val idx = segmentIndexAt(animatable.value)
                                        settledResult = options[idx]
                                        onSpinResult(options[idx])
                                        audioHaptic.playFeedback(SoundEffect.SPIN_DING)
                                    }
                                }
                            }
                        )
                    }
                }
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = minOf(centerX, centerY) - 8f

            drawCircle(
                color = Color.Black.copy(alpha = 0.15f),
                radius = radius + 4f,
                center = Offset(centerX + 2f, centerY + 4f)
            )

            rotate(degrees = animatable.value) {
                var startAngle = -90f
                options.forEachIndexed { index, option ->
                    val sweepAngle = segmentAngles[index]
                    val color = colorScheme.getColorForIndex(index)

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                    drawArc(
                        color = colorScheme.borderColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    val textAngleDeg = startAngle + sweepAngle / 2
                    val textAngleRad = Math.toRadians(textAngleDeg.toDouble())
                    val textRadius = radius * 0.58f
                    val textCx = centerX + cos(textAngleRad).toFloat() * textRadius
                    val textCy = centerY + sin(textAngleRad).toFloat() * textRadius

                    val baseTextSize = radius * 0.18f
                    val availWidth = radius * 0.45f
                    textFillPaint.textSize = baseTextSize
                    textStrokePaint.textSize = baseTextSize
                    val measured = textFillPaint.measureText(option)
                    if (measured > availWidth) {
                        textFillPaint.textSize = baseTextSize * (availWidth / measured)
                        textStrokePaint.textSize = textFillPaint.textSize
                    }
                    textFillPaint.color = colorScheme.textColor.copy(alpha = 0.9f).toArgb()

                    // Align text baseline along the bisector (radial direction)
                    // Top half: rotate so text flows outward (center → edge)
                    // Bottom half: flip 180° so text stays readable
                    val isBottomHalf = textAngleDeg % 360f in 90f..270f
                    val rotation = if (isBottomHalf) textAngleDeg else textAngleDeg + 180f

                    val canvas = drawContext.canvas.nativeCanvas
                    canvas.save()
                    canvas.translate(textCx, textCy)
                    canvas.rotate(rotation)
                    canvas.drawText(option, 0f, textFillPaint.textSize * 0.35f, textStrokePaint)
                    canvas.drawText(option, 0f, textFillPaint.textSize * 0.35f, textFillPaint)
                    canvas.restore()

                    startAngle += sweepAngle
                }
            }

            drawCircle(
                color = colorScheme.borderColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        val indicatorWidth = wheelSize * 0.08f
        TriangleIndicator(
            color = colorScheme.indicatorColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
                .size(width = indicatorWidth, height = indicatorWidth * 0.83f)
        )
    }

    // Expose spin trigger for button
    SpinWheelSpinTrigger { spinTrigger++ }
}

private val spinTriggerCallbacks = mutableListOf<() -> Unit>()

@Composable
private fun SpinWheelSpinTrigger(callback: () -> Unit) {
    androidx.compose.runtime.DisposableEffect(callback) {
        spinTriggerCallbacks.add(callback)
        onDispose { spinTriggerCallbacks.remove(callback) }
    }
}

fun triggerSpinWheelSpin() {
    spinTriggerCallbacks.lastOrNull()?.invoke()
}

@Composable
private fun TriangleIndicator(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val shadowPath = Path().apply {
            moveTo(width / 2f + 1f, height + 2f)
            lineTo(1f, 1f + 2f)
            lineTo(width - 1f, 1f + 2f)
            close()
        }
        drawPath(path = shadowPath, color = Color.Black.copy(alpha = 0.2f), style = Fill)

        val trianglePath = Path().apply {
            moveTo(width / 2f, height)
            lineTo(0f, 0f)
            lineTo(width, 0f)
            close()
        }
        drawPath(path = trianglePath, color = color, style = Fill)
    }
}

private fun Color.toArgb(): Int {
    val alpha = (this.alpha * 255).toInt()
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
