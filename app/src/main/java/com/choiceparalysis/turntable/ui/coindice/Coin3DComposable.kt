package com.choiceparalysis.turntable.ui.coindice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.choiceparalysis.turntable.ui.components.StandardEasing
import com.choiceparalysis.turntable.viewmodel.CoinSide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CoinSpinEasing = CubicBezierEasing(0.12f, 0.0f, 0.12f, 1.0f)

@Composable
fun Coin3DFlip(
    result: CoinSide?,
    pendingResult: CoinSide?,
    isAnimating: Boolean,
    headsImage: ImageBitmap?,
    tailsImage: ImageBitmap?,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    val rotationAnim = remember { Animatable(0f) }
    val bounceAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }
    val highlightAnim = remember { Animatable(0f) }
    var showHighlight by remember { mutableIntStateOf(0) }
    // Track the settled result for display (decoupled from animation state)
    var settledResult by remember { mutableStateOf<CoinSide?>(null) }

    // Main flip animation
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            settledResult = null
            val targetRotation = when (pendingResult) {
                CoinSide.HEADS -> 1800f
                CoinSide.TAILS -> 1980f
                null -> 1800f
            }

            rotationAnim.snapTo(0f)
            bounceAnim.snapTo(0f)
            scaleAnim.snapTo(1f)

            launch {
                bounceAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 2000
                        0f at 0
                        -20f at 1600
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
                        1.06f at 400 using StandardEasing.EaseOutQuart
                        1f at 1000 using StandardEasing.EaseInQuart
                    }
                )
            }

            rotationAnim.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(
                    durationMillis = 2000,
                    easing = CoinSpinEasing
                )
            )

            // Settle: record result, reset rotation, trigger highlight
            settledResult = pendingResult
            rotationAnim.snapTo(0f)
            showHighlight++
            onAnimationComplete()
        }
    }

    // Highlight sweep
    LaunchedEffect(showHighlight) {
        if (showHighlight > 0) {
            delay(200)
            highlightAnim.snapTo(0f)
            highlightAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = StandardEasing.EaseOutCubic
                )
            )
        }
    }

    // Rotation for graphicsLayer: actual rotation during animation, 0 when idle
    val displayRotation = if (isAnimating) rotationAnim.value % 360f else 0f

    // Image selection: use settledResult when idle, rotation angle when animating
    val activeImage: ImageBitmap?
    val needsFlip: Boolean

    if (isAnimating && settledResult == null) {
        val rot = rotationAnim.value % 360f
        val showingFront = rot < 90f || rot >= 270f
        activeImage = if (showingFront) headsImage else tailsImage
        needsFlip = !showingFront
    } else {
        val r = settledResult ?: result
        activeImage = if (r == CoinSide.TAILS) tailsImage else headsImage
        needsFlip = false
    }

    Canvas(
        modifier = modifier
            .size(225.dp)
            .graphicsLayer {
                rotationY = displayRotation
                translationY = bounceAnim.value
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                cameraDistance = 12f * density
            }
    ) {
        val cx = size.width / 2
        val cy = size.height / 2
        val coinR = size.width / 2
        val faceR = coinR * 0.94f
        val imgR = coinR * 0.88f

        // 1. Shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.25f),
            topLeft = Offset(cx - coinR * 0.85f, cy - coinR * 0.6f + 12f),
            size = Size(coinR * 1.7f, coinR * 1.0f)
        )

        // 2. Coin rim
        drawCircle(Color(0xFFB0B0B0), radius = coinR, center = Offset(cx, cy))

        // 3. Coin face gradient
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Color(0xFFD0D0D0), Color(0xFFA8A8A8)),
                center = Offset(cx, cy),
                radius = faceR
            ),
            radius = faceR,
            center = Offset(cx, cy)
        )

        // 4. Image with circular clip using clipPath
        activeImage?.let { image ->
            val imgSize = (imgR * 2).toInt()
            val imgOffset = IntOffset((cx - imgR).toInt(), (cy - imgR).toInt())
            val circlePath = Path().apply {
                addOval(Rect(cx - imgR, cy - imgR, cx + imgR, cy + imgR))
            }

            clipPath(circlePath, ClipOp.Intersect) {
                if (needsFlip) {
                    // Flip horizontally for back face (counteracts graphicsLayer mirroring)
                    drawContext.canvas.save()
                    drawContext.canvas.concat(
                        androidx.compose.ui.graphics.Matrix().apply {
                            translate(cx, cy)
                            scale(-1f, 1f)
                            translate(-cx, -cy)
                        }
                    )
                    drawImage(
                        image = image,
                        dstSize = IntSize(imgSize, imgSize),
                        dstOffset = imgOffset
                    )
                    drawContext.canvas.restore()
                } else {
                    drawImage(
                        image = image,
                        dstSize = IntSize(imgSize, imgSize),
                        dstOffset = imgOffset
                    )
                }
            }
        }

        // 5. Highlight sweep
        val hl = highlightAnim.value
        if (hl > 0f) {
            val bandPos = -coinR * 2f + hl * coinR * 4f
            val bandW = coinR * 0.5f
            clipPath(Path().apply { addOval(Rect(cx - coinR, cy - coinR, cx + coinR, cy + coinR)) }) {
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.5f),
                            Color.Transparent,
                        ),
                        start = Offset(cx + bandPos - bandW, cy + bandPos - bandW),
                        end = Offset(cx + bandPos + bandW, cy + bandPos + bandW)
                    ),
                    topLeft = Offset(cx - coinR, cy - coinR),
                    size = Size(coinR * 2, coinR * 2)
                )
            }
        }
    }
}
