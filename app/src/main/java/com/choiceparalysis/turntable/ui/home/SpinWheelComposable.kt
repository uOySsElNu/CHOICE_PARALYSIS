package com.choiceparalysis.turntable.ui.home

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.withTranslation
import kotlin.time.Duration.Companion.milliseconds

/** Pre-computed per-segment data that doesn't depend on Canvas size. */
private data class SegmentLayout(
    val startAngle: Float,    // degrees, starting from -90° (top)
    val sweepAngle: Float,
    val color: Color,
    val borderColor: Color,
    val text: String,
    val textAngleDeg: Float,  // bisector angle for text placement
)

@Composable
fun SpinWheel(
    options: List<String>,
    weights: List<Int>,
    colorScheme: WheelColorScheme,
    modifier: Modifier = Modifier,
    lastResult: String? = null,
    contentDescription: String = "",
    onSpinResult: (String) -> Unit = {},
    onSpinStart: () -> Unit = {},
    onSpinEnd: () -> Unit = {},
    onResultDragged: () -> Unit = {},
) {
    val context = LocalContext.current
    val audioHaptic = remember { AudioHapticManager.getInstance(context) }
    val animatable = remember { Animatable(0f) }
    var settledResult by remember { mutableStateOf<String?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var isSpinning by remember { mutableStateOf(false) }

    // Snap wheel to lastResult when it arrives from persistence
    LaunchedEffect(lastResult, options, weights) {
        if (lastResult == null || isSpinning) return@LaunchedEffect
        val idx = options.indexOf(lastResult)
        if (idx < 0) return@LaunchedEffect
        if (settledResult == lastResult) return@LaunchedEffect
        val safeW = options.indices.map { weights.getOrElse(it) { 1 } }
        val total = safeW.sum().coerceAtLeast(options.size)
        val angles = safeW.map { (it.toFloat() / total) * 360f }
        val centerAngle = angles.take(idx).sum() + angles[idx] / 2f
        val targetRotation = (360f - centerAngle) % 360f
        animatable.snapTo(targetRotation)
        settledResult = lastResult
    }
    var spinTrigger by remember { mutableIntStateOf(0) }

    // --- Pre-compute segment layout (angles, colors, text) — independent of Canvas size ---
    val segmentLayouts = remember(options, weights, colorScheme) {
        val safeW = options.indices.map { weights.getOrElse(it) { 1 } }
        val total = safeW.sum().coerceAtLeast(options.size)
        val angles = safeW.map { (it.toFloat() / total) * 360f }
        var acc = -90f  // start from top
        options.mapIndexed { index, option ->
            val start = acc
            val sweep = angles[index]
            acc += sweep
            SegmentLayout(
                startAngle = start,
                sweepAngle = sweep,
                color = colorScheme.getColorForIndex(index),
                borderColor = colorScheme.borderColor,
                text = option,
                textAngleDeg = start + sweep / 2,
            )
        }
    }

    fun segmentIndexAt(rotation: Float): Int {
        val norm = ((rotation % 360f) + 360f) % 360f
        val indicatorAngle = (360f - norm) % 360f
        var acc = 0f
        for (i in segmentLayouts.indices) {
            acc += segmentLayouts[i].sweepAngle
            if (indicatorAngle < acc) return i
        }
        return segmentLayouts.indices.last
    }

    // --- Haptic tick — picker/roller detent feel ---
    // Each tick is identical: short, sharp, constant.
    // Fast spin → ticks arrive faster (natural frequency increase).
    // Slow spin → ticks spaced out (natural deceleration feel).
    // Throttle: 50ms minimum between ticks (Android docs: ≥50ms for discernible gaps).
    LaunchedEffect(options, weights) {
        var lastRotation = animatable.value
        var lastSegment = segmentIndexAt(lastRotation)
        var lastTickTime = 0L
        snapshotFlow { animatable.value }.collect { rotation ->
            val delta = abs(rotation - lastRotation)
            lastRotation = rotation
            if (delta > 180f) return@collect
            val currentSegment = segmentIndexAt(rotation)
            if (currentSegment != lastSegment) {
                val now = System.currentTimeMillis()
                if (now - lastTickTime > 50) {
                    audioHaptic.playFeedback(SoundEffect.WHEEL_TICK)
                    lastTickTime = now
                }
                lastSegment = currentSegment
            }
        }
    }

    // --- Button spin — reuses same physics as drag fling ---
    LaunchedEffect(spinTrigger) {
        if (spinTrigger == 0) return@LaunchedEffect
        isSpinning = true
        onSpinStart()
        settledResult = null
        try {
            // Random velocity in range of a moderate manual fling
            // Random extra rotation beyond minimum to land on a random segment
            val extraDegrees = (0..360).random().toFloat()
            val totalRotation = 1440f + extraDegrees  // 4+ full turns + random offset
            val duration = maxOf(2000, (totalRotation / 720f * 2000f).toInt()).coerceAtMost(5000)

            if (!animatable.isRunning) {
                animatable.snapTo(0f)
            }
            val animateTarget = animatable.value + totalRotation
            animatable.animateTo(animateTarget, tween(duration, easing = StandardEasing.EaseOutQuart))
            val idx = segmentIndexAt(animatable.value)
            settledResult = options[idx]
            onSpinResult(options[idx])
            audioHaptic.playFeedback(SoundEffect.SPIN_DING)
        } finally {
            isSpinning = false
            onSpinEnd()
        }
    }

    // --- Result-drag detection ---
    LaunchedEffect(isDragging, settledResult) {
        if (!isDragging && settledResult != null) {
            delay(300.milliseconds)
            val idx = segmentIndexAt(animatable.value)
            if (options.getOrNull(idx) != settledResult) {
                onResultDragged()
                settledResult = null
            }
        }
    }

    // --- Layout ---
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
                .semantics {
                    if (contentDescription.isNotEmpty()) {
                        this.contentDescription = contentDescription
                    }
                }
                .pointerInput(options, weights) {
                    coroutineScope {
                        var prevAngle = 0f
                        var prevTime = 0L
                        val velocities = mutableListOf<Float>()
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                launch { animatable.stop() }
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
                                change.consume()
                            },
                            onDragEnd = {
                                isDragging = false
                                val avgVelocity = if (velocities.isNotEmpty()) {
                                    velocities.sorted().let {
                                        it.subList(it.size / 4, it.size * 3 / 4).average().toFloat()
                                    }
                                } else 0f
                                if (abs(avgVelocity) > 400f) {
                                    if (isSpinning) return@detectDragGestures
                                    settledResult = null
                                    launch {
                                        isSpinning = true
                                        onSpinStart()
                                        try {
                                            val absV = abs(avgVelocity)
                                            val minRotation = 1440f
                                            val sign = if (avgVelocity > 0) 1f else -1f
                                            val current = animatable.value
                                            val target = current + sign * maxOf(absV * 1.5f, minRotation)
                                            animatable.animateTo(
                                                target,
                                                tween(maxOf(2000, (absV * 1.5f / 720f * 2000f).toInt()).coerceAtMost(5000),
                                                    easing = StandardEasing.EaseOutQuart)
                                            )
                                            val idx = segmentIndexAt(animatable.value)
                                            settledResult = options[idx]
                                            onSpinResult(options[idx])
                                            audioHaptic.playFeedback(SoundEffect.SPIN_DING)
                                        } finally {
                                            isSpinning = false
                                            onSpinEnd()
                                        }
                                    }
                                } else {
                                    isSpinning = false
                                    onSpinEnd()
                                }
                            }
                        )
                    }
                }
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = minOf(centerX, centerY) - 8f
            val diameter = radius * 2
            val arcSize = Size(diameter, diameter)
            val arcTopLeft = Offset(centerX - radius, centerY - radius)

            // Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.15f),
                radius = radius + 4f,
                center = Offset(centerX + 2f, centerY + 4f)
            )

            rotate(degrees = animatable.value) {
                // Draw arcs using pre-computed layouts
                for (seg in segmentLayouts) {
                    drawArc(
                        color = seg.color,
                        startAngle = seg.startAngle,
                        sweepAngle = seg.sweepAngle,
                        useCenter = true,
                        topLeft = arcTopLeft,
                        size = arcSize
                    )
                    drawArc(
                        color = seg.borderColor,
                        startAngle = seg.startAngle,
                        sweepAngle = seg.sweepAngle,
                        useCenter = true,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Draw text — only position and size depend on radius
                val textRadius = radius * 0.58f
                val baseTextSize = radius * 0.18f
                val availWidth = radius * 0.45f
                val canvas = drawContext.canvas.nativeCanvas

                for (seg in segmentLayouts) {
                    val angleRad = Math.toRadians(seg.textAngleDeg.toDouble())
                    val textCx = centerX + cos(angleRad).toFloat() * textRadius
                    val textCy = centerY + sin(angleRad).toFloat() * textRadius

                    textFillPaint.textSize = baseTextSize
                    textStrokePaint.textSize = baseTextSize
                    val measured = textFillPaint.measureText(seg.text)
                    if (measured > availWidth) {
                        val scale = availWidth / measured
                        textFillPaint.textSize = baseTextSize * scale
                        textStrokePaint.textSize = baseTextSize * scale
                    }

                    val isBottomHalf = seg.textAngleDeg % 360f in 90f..270f
                    val rot = if (isBottomHalf) seg.textAngleDeg else seg.textAngleDeg + 180f

                    canvas.withTranslation(textCx, textCy) {
                        rotate(rot)
                        drawText(seg.text, 0f, textFillPaint.textSize * 0.35f, textStrokePaint)
                        drawText(seg.text, 0f, textFillPaint.textSize * 0.35f, textFillPaint)
                    }
                }
            }

            // Outer ring
            drawCircle(
                color = colorScheme.borderColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Indicator
        val indicatorWidth = wheelSize * 0.08f
        TriangleIndicator(
            color = colorScheme.indicatorColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
                .size(width = indicatorWidth, height = indicatorWidth * 0.83f)
        )
    }

    // Button trigger guard
    SpinWheelSpinTrigger {
        if (!isSpinning) spinTrigger++
    }
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

