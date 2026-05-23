package com.choiceparalysis.turntable.ui.coindice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.audio.SoundEffect
import com.choiceparalysis.turntable.ui.components.StandardEasing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Standard die face adjacency (right-hand convention):
 * Opposite pairs: 1↔6, 2↔5, 3↔4
 * Given (front, top) → right face.
 * Verified cyclic identity: (f,t)->r implies (t,r)->f and (r,f)->t.
 */
private val FACE_TABLE = mapOf(
    (1 to 2) to 4, (1 to 3) to 2, (1 to 4) to 5, (1 to 5) to 3,
    (2 to 1) to 3, (2 to 3) to 6, (2 to 4) to 1, (2 to 6) to 4,
    (3 to 1) to 5, (3 to 2) to 1, (3 to 5) to 6, (3 to 6) to 2,
    (4 to 1) to 2, (4 to 2) to 6, (4 to 5) to 1, (4 to 6) to 5,
    (5 to 1) to 4, (5 to 3) to 1, (5 to 4) to 6, (5 to 6) to 3,
    (6 to 2) to 3, (6 to 3) to 5, (6 to 4) to 2, (6 to 5) to 4,
)

private fun rightFace(front: Int, top: Int) = FACE_TABLE[front to top] ?: 3
private fun topFaceFor(front: Int) = when (front) {
    1 -> 2; 2 -> 6; 3 -> 1; 4 -> 1; 5 -> 2; 6 -> 5; else -> 2
}

// Constant colors (avoid per-frame allocation)
private val COLOR_TOP = Color(0xFFF0F0F0)
private val COLOR_RIGHT = Color(0xFFD6D6D6)
private val COLOR_FRONT = Color(0xFFE4E4E4)
private val COLOR_DOT_DARK = Color(0xFF444444)
private val COLOR_DOT_MED = Color(0xFF707070)
private val COLOR_DOT_LIGHT = Color(0xFF686868)
private val COLOR_SHADOW = Color.Black.copy(alpha = 0.15f)
private val COLOR_HIGHLIGHT_80 = Color.White.copy(alpha = 0.8f)
private val COLOR_HIGHLIGHT_60 = Color.White.copy(alpha = 0.6f)
private val COLOR_HIGHLIGHT_50 = Color.White.copy(alpha = 0.5f)
private val COLOR_HIGHLIGHT_40 = Color.White.copy(alpha = 0.4f)
private val COLOR_SHADOW_08 = Color.Black.copy(alpha = 0.08f)
private val COLOR_SHADOW_06 = Color.Black.copy(alpha = 0.06f)
private val COLOR_SHADOW_10 = Color.Black.copy(alpha = 0.10f)
private val COLOR_DOT_SHADOW = Color.Black.copy(alpha = 0.08f)

@Composable
fun Dice3DRoll(
    value: Int?,
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    val bounceAnim = remember { Animatable(0f) }
    val rotationAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }
    val popAnim = remember { Animatable(1f) }
    val breathAnim = remember { Animatable(0f) }

    // Face values shown on the dice (mutableIntStateOf triggers Canvas recomposition)
    var frontVal by remember { mutableIntStateOf(value ?: 1) }
    var topVal by remember { mutableIntStateOf(topFaceFor(value ?: 1)) }
    var rightVal by remember { mutableIntStateOf(rightFace(frontVal, topVal)) }

    // Rolling face values (updated by throttled coroutine, not per-frame random)
    var spinFront by remember { mutableIntStateOf(1) }
    var spinTop by remember { mutableIntStateOf(2) }
    var spinRight by remember { mutableIntStateOf(3) }
    var isSpinning by remember { mutableStateOf(false) }

    // Bounce sound on each landing during dice roll
    val audioHaptic = AudioHapticManager.getInstance(LocalContext.current)
    LaunchedEffect(Unit) {
        var wasNegative = false
        snapshotFlow { bounceAnim.value }.collect { value ->
            val isNegative = value < -2f
            if (wasNegative && !isNegative) {
                // Transition from negative to zero/positive = bounce landing
                audioHaptic.playFeedback(SoundEffect.DICE_BOUNCE)
            }
            wasNegative = isNegative
        }
    }

    // Sync result from ViewModel + pop animation
    LaunchedEffect(value) {
        value?.let {
            frontVal = it
            topVal = topFaceFor(it)
            rightVal = rightFace(frontVal, topVal)
            // Pop bounce: current size → enlarge → shrink back
            popAnim.snapTo(1f)
            popAnim.animateTo(1f, keyframes {
                durationMillis = 350
                1f at 0
                1.2f at 80 using StandardEasing.EaseOutQuart
                1f at 350 using StandardEasing.EaseInQuart
            })
        }
    }

    // Roll animation
    LaunchedEffect(isAnimating) {
        if (!isAnimating) return@LaunchedEffect

        breathAnim.stop()
        bounceAnim.snapTo(0f)
        rotationAnim.snapTo(0f)
        scaleAnim.snapTo(1f)
        popAnim.snapTo(1f)
        isSpinning = true

        val spinJob = launch {
            while (true) {
                spinFront = (1..6).random()
                spinTop = topFaceFor(spinFront)
                spinRight = rightFace(spinFront, spinTop)
                delay(60L)
            }
        }

        // All three run in parallel — isSpinning stays true throughout
        val rotTarget = 720f + (0..180).random().toFloat()

        val rotJob = launch {
            rotationAnim.animateTo(rotTarget, keyframes {
                durationMillis = 1000
                0f at 0; 360f at 400; 600f at 700; rotTarget at 1000
            })
        }

        launch {
            bounceAnim.animateTo(0f, keyframes {
                durationMillis = 1000
                0f at 0; -60f at 150; 0f at 350; -25f at 500
                0f at 650; -8f at 750; 0f at 850
            })
        }

        launch {
            scaleAnim.animateTo(1f, keyframes {
                durationMillis = 1000
                1f at 0; 1.15f at 100; 0.95f at 300; 1.05f at 500; 1f at 700
            })
        }

        // Wait for all parallel animations to complete
        rotJob.join()

        // Stop spinning — from here, showFront/showTop/showRight use frontVal/topVal/rightVal
        spinJob.cancel()
        isSpinning = false

        // Notify ViewModel — it will set value, which triggers LaunchedEffect(value)
        // to update frontVal/topVal/rightVal with the actual result
        onAnimationComplete()
    }

    // Idle breathing (reset to baseline on start)
    LaunchedEffect(isAnimating) {
        if (!isAnimating) {
            breathAnim.snapTo(0f)
            breathAnim.animateTo(1f, InfiniteRepeatableSpec(
                animation = tween(2000, easing = StandardEasing.EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ))
        }
    }

    // Determine which face values to show
    val showFront = if (isSpinning) spinFront else frontVal
    val showTop = if (isSpinning) spinTop else topVal
    val showRight = if (isSpinning) spinRight else rightVal

    BoxWithConstraints(modifier = modifier) {
        val diceSize = minOf(maxWidth, 225.dp)
        Canvas(
            modifier = Modifier
                .size(diceSize)
                .graphicsLayer {
                    translationY = bounceAnim.value + breathAnim.value * -6f
                    scaleX = scaleAnim.value * popAnim.value
                    scaleY = scaleAnim.value * popAnim.value
                }
        ) {
        val sizeCube = size.width * 0.35f
        val depth = sizeCube * 0.6f
        val cx = size.width / 2f - depth * 0.5f
        val cy = size.height / 2f + sizeCube * 0.1f

        // Front face
        val frontTL = Offset(cx - sizeCube, cy - sizeCube)
        val frontTR = Offset(cx + sizeCube, cy - sizeCube)
        val frontBR = Offset(cx + sizeCube, cy + sizeCube)
        val frontBL = Offset(cx - sizeCube, cy + sizeCube)

        // Top face: extends upper-right
        val topTL = Offset(cx - sizeCube + depth, cy - sizeCube - depth)
        val topTR = Offset(cx + sizeCube + depth, cy - sizeCube - depth)

        // Right face: extends lower-right, shares topTR
        val rightBR = Offset(cx + sizeCube + depth, cy + sizeCube - depth)

        // Ground shadow
        drawOval(
            COLOR_SHADOW,
            topLeft = Offset(cx - sizeCube - depth * 0.2f, cy + sizeCube + 6f),
            size = Size(sizeCube * 2f + depth * 1.2f, depth * 0.6f)
        )

        // === Draw order: top → right → front ===
        drawPath(Path().apply {
            moveTo(frontTL.x, frontTL.y)
            lineTo(topTL.x, topTL.y)
            lineTo(topTR.x, topTR.y)
            lineTo(frontTR.x, frontTR.y)
            close()
        }, COLOR_TOP)

        drawPath(Path().apply {
            moveTo(frontTR.x, frontTR.y)
            lineTo(topTR.x, topTR.y)
            lineTo(rightBR.x, rightBR.y)
            lineTo(frontBR.x, frontBR.y)
            close()
        }, COLOR_RIGHT)

        drawPath(Path().apply {
            moveTo(frontTL.x, frontTL.y)
            lineTo(frontTR.x, frontTR.y)
            lineTo(frontBR.x, frontBR.y)
            lineTo(frontBL.x, frontBL.y)
            close()
        }, COLOR_FRONT)

        // === Edge highlights ===
        drawLine(COLOR_HIGHLIGHT_80, frontTL, frontTR, 1.8f)
        drawLine(COLOR_HIGHLIGHT_60, frontTL, frontBL, 1.2f)
        drawLine(COLOR_HIGHLIGHT_50, frontTL, topTL, 1.2f)
        drawLine(COLOR_HIGHLIGHT_40, topTL, topTR, 1f)

        // === Edge shadows ===
        drawLine(COLOR_SHADOW_08, frontTR, frontBR, 2f)
        drawLine(COLOR_SHADOW_06, frontBL, frontBR, 2f)
        drawLine(COLOR_SHADOW_10, topTR, rightBR, 1.5f)
        drawLine(COLOR_SHADOW_10, rightBR, frontBR, 1.5f)

        // === Dots ===
        drawFrontDots(showFront, cx, cy, sizeCube)
        drawTopDots(showTop, topTL, topTR, frontTL, frontTR, sizeCube)
        drawRightDots(showRight, frontTR, topTR, rightBR, frontBR, sizeCube)

        if (isAnimating && rotationAnim.value < 300f) {
            val alpha = 0.2f * (1f - rotationAnim.value / 300f)
            drawRect(Color.White.copy(alpha), topLeft = frontTL, size = Size(sizeCube * 2, sizeCube * 2))
        }
        }
    }
}

// === Dot drawing ===

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrontDots(
    value: Int, cx: Float, cy: Float, s: Float
) {
    val r = s * 0.12f; val pad = s * 0.48f
    faceDotPositions(value, cx - pad, cx + pad, cy - pad, cy + pad, cx, cy).forEach { p ->
        drawCircle(COLOR_DOT_SHADOW, r * 1.2f, Offset(p.x + 0.8f, p.y + 0.8f))
        drawCircle(COLOR_DOT_DARK, r, p)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTopDots(
    value: Int, tBL: Offset, tBR: Offset, fTL: Offset, fTR: Offset, s: Float
) {
    val r = s * 0.08f
    faceDotPositionsNorm(value).forEach { (u, v) ->
        val top = lerp(tBL, tBR, u); val bot = lerp(fTL, fTR, u)
        drawCircle(COLOR_DOT_MED, r, lerp(top, bot, v))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRightDots(
    value: Int, fTR: Offset, tBR: Offset, rBR: Offset, fBR: Offset, s: Float
) {
    val r = s * 0.08f
    faceDotPositionsNorm(value).forEach { (u, v) ->
        val top = lerp(fTR, tBR, u); val bot = lerp(fBR, rBR, u)
        drawCircle(COLOR_DOT_LIGHT, r, lerp(top, bot, v))
    }
}

private fun lerp(a: Offset, b: Offset, t: Float) = Offset(
    a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t
)

// Dot positions in pixel coordinates (for front face)
private fun faceDotPositions(
    v: Int, l: Float, r: Float, t: Float, b: Float, cx: Float, cy: Float
): List<Offset> = when (v) {
    1 -> listOf(Offset(cx, cy))
    2 -> listOf(Offset(l, t), Offset(r, b))
    3 -> listOf(Offset(l, t), Offset(cx, cy), Offset(r, b))
    4 -> listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b))
    5 -> listOf(Offset(l, t), Offset(r, t), Offset(cx, cy), Offset(l, b), Offset(r, b))
    6 -> listOf(Offset(l, t), Offset(r, t), Offset(l, cy), Offset(r, cy), Offset(l, b), Offset(r, b))
    else -> emptyList()
}

// Dot positions as normalized (u, v) for parallelogram faces
private fun faceDotPositionsNorm(v: Int): List<Pair<Float, Float>> {
    val p = 0.22f; val q = 1f - p
    return when (v) {
        1 -> listOf(0.5f to 0.5f)
        2 -> listOf(p to p, q to q)
        3 -> listOf(p to p, 0.5f to 0.5f, q to q)
        4 -> listOf(p to p, q to p, p to q, q to q)
        5 -> listOf(p to p, q to p, 0.5f to 0.5f, p to q, q to q)
        6 -> listOf(p to p, q to p, p to 0.5f, q to 0.5f, p to q, q to q)
        else -> emptyList()
    }
}
