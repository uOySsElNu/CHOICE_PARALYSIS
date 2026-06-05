package com.choiceparalysis.turntable.ui.coindice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.audio.SoundEffect
import com.choiceparalysis.turntable.ui.components.StandardEasing
import com.choiceparalysis.turntable.viewmodel.CoinSide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private val CoinSpinEasing = CubicBezierEasing(0.12f, 0.0f, 0.12f, 1.0f)

/**
 * 3D 硬币翻面组件。
 *
 * 交互方式：
 * - 手动拖拽：手指向右滑动硬币逆时针旋转，向左滑动顺时针旋转。
 *   拖过 90° 中线则完成翻面，图片自动切换。
 * - 点击按钮：触发射击动画，始终逆时针旋转 5 圈后停在随机面上。
 *
 * 面-角度映射：
 *   HEADS = 0°（正面）
 *   TAILS = 180°（反面）
 *
 * graphicsLayer.rotationY：
 *   0° → 正面可见
 *   90°~270° → 反面可见（图片需水平镜像抵消 graphicsLayer 翻转）
 */
@Composable
fun Coin3DFlip(
    result: CoinSide?,
    pendingResult: CoinSide?,
    isAnimating: Boolean,
    headsImage: ImageBitmap?,
    tailsImage: ImageBitmap?,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {},
    onDragFlipComplete: (CoinSide) -> Unit = {},
    onFlingChanged: (Boolean) -> Unit = {},
) {
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val highlight = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // 是否正在拖拽
    var isDragging by remember { mutableStateOf(false) }
    // 当前稳定面角度 (0f = HEADS, 180f = TAILS)
    var settledAngle by remember { mutableFloatStateOf(0f) }
    // 高光扫过触发计数器
    var highlightTrigger by remember { mutableIntStateOf(0) }
    // 是否正在 fling 动画（阻止按钮动画和新手势）
    var isFling by remember { mutableStateOf(false) }

    // 首帧用轻量静态图，避免 Canvas+graphicsLayer 阻塞导航动画
    var canvasReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        canvasReady = true
    }

    val audioHaptic = AudioHapticManager.getInstance(androidx.compose.ui.platform.LocalContext.current)

    // SpinWheel 风格逐帧速度追踪
    var prevTime by remember { mutableLongStateOf(0L) }
    val velocities = remember { mutableListOf<Float>() }

    val anyAnimating = isAnimating || isFling

    // Report fling state to ViewModel for button enabled/disabled
    LaunchedEffect(isFling) {
        onFlingChanged(isFling)
    }

    // idle 时同步 settledAngle
    LaunchedEffect(result, isAnimating) {
        if (!isAnimating && !isFling && result != null) {
            val targetAngle = faceAngle(result)
            if (!isDragging) {
                var finalTarget = targetAngle
                while (finalTarget - rotation.value > 180f) finalTarget -= 360f
                while (finalTarget - rotation.value < -180f) finalTarget += 360f
                rotation.snapTo(finalTarget)
            }
            settledAngle = targetAngle
        }
    }

    // === 按钮动画：始终逆时针旋转 5 圈 + 停在随机面 ===
    LaunchedEffect(isAnimating) {
        if (!isAnimating || isFling) return@LaunchedEffect

        audioHaptic.playFeedback(SoundEffect.COIN_BUTTON)
        val target = pendingResult ?: CoinSide.HEADS
        val endAngle = faceAngle(target)
        // 逆时针 = 正方向，固定5整圈(1800°)，修正负角度取模
        val remainder = ((rotation.value % 360f) + 360f) % 360f
        val totalRotation = rotation.value + 1800f - remainder + endAngle

        // 缩放动画
        launch {
            scale.snapTo(1f)
            scale.animateTo(1f, keyframes {
                durationMillis = 2000
                1f at 0
                1.06f at 400 using StandardEasing.EaseOutQuart
                1f at 1000 using StandardEasing.EaseInQuart
            })
        }

        rotation.animateTo(totalRotation, tween(2000, easing = CoinSpinEasing))

        // 精准停在目标角度
        var finalAngle = endAngle
        while (finalAngle - rotation.value > 180f) finalAngle -= 360f
        while (finalAngle - rotation.value < -180f) finalAngle += 360f
        rotation.snapTo(finalAngle)

        settledAngle = endAngle
        highlightTrigger++
        onAnimationComplete()
    }

    // === 高光扫过 ===
    LaunchedEffect(highlightTrigger) {
        if (highlightTrigger > 0) {
            delay(200)
            highlight.snapTo(0f)
            highlight.animateTo(1f, tween(800, easing = StandardEasing.EaseOutCubic))
        }
    }

    // === 修复：标准化角度计算 + 精准正反面判定 ===
    val normalizedAngle = ((rotation.value % 360f) + 360f) % 360f
    val showTails = normalizedAngle in 90f..270f
    val activeImage = if (showTails) tailsImage else headsImage
    // 精准镜像补偿：仅在3D旋转需要时水平翻转图片
    val needImageMirror = normalizedAngle in 90f..270f

    BoxWithConstraints(modifier = modifier) {
        val coinSize = minOf(maxWidth, 225.dp)

        if (!canvasReady) {
            // 首帧：轻量静态图，不创建 graphicsLayer/pointerInput
            val img = headsImage
            Canvas(modifier = Modifier.size(coinSize)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val coinR = size.width / 2
                val faceR = coinR * 0.94f
                val imgR = coinR * 0.88f
                drawOval(Color.Black.copy(alpha = 0.25f), topLeft = Offset(cx - coinR * 0.85f, cy - coinR * 0.6f + 12f), size = Size(coinR * 1.7f, coinR * 1.0f))
                drawCircle(Color(0xFFB0B0B0), radius = coinR, center = Offset(cx, cy))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFD0D0D0), Color(0xFFA8A8A8)), center = Offset(cx, cy), radius = faceR), radius = faceR, center = Offset(cx, cy))
                if (img != null) {
                    val imgDiameter = (imgR * 2).toInt()
                    val circlePath = Path().apply { addOval(Rect(cx - imgR, cy - imgR, cx + imgR, cy + imgR)) }
                    clipPath(circlePath, ClipOp.Intersect) {
                        drawImage(img, dstSize = IntSize(imgDiameter, imgDiameter), dstOffset = IntOffset((cx - imgR).toInt(), (cy - imgR).toInt()))
                    }
                }
            }
            return@BoxWithConstraints
        }

        Canvas(
            modifier = Modifier
                .size(coinSize)
                .pointerInput(anyAnimating) {
                    detectDragGestures(
                        onDragStart = {
                            if (anyAnimating) return@detectDragGestures
                            isDragging = true
                            velocities.clear()
                            prevTime = System.currentTimeMillis()
                        },
                        onDrag = { change, dragAmount ->
                            if (anyAnimating) return@detectDragGestures
                            change.consume()
                            // 右滑→逆时针(+)，左滑→顺时针(-)
                            val delta = dragAmount.x * 0.8f
                            coroutineScope.launch {
                                rotation.snapTo(rotation.value + delta)
                            }
                            // 逐帧速度追踪（°/秒）
                            val now = System.currentTimeMillis()
                            val dt = (now - prevTime).coerceAtLeast(1)
                            velocities.add(delta / dt * 1000f)
                            if (velocities.size > 5) velocities.removeAt(0)
                            prevTime = now
                        },
                        onDragEnd = {
                            if (anyAnimating) {
                                isDragging = false
                                return@detectDragGestures
                            }
                            isDragging = false

                            // 中位数过滤，取中间 50% 样本
                            val avgVelocity = if (velocities.isNotEmpty()) {
                                velocities.sorted().let {
                                    it.subList(it.size / 4, it.size * 3 / 4).average().toFloat()
                                }
                            } else 0f

                            if (abs(avgVelocity) > 400f) {
                                // ===== Fling：无缝衔接滑动方向 =====
                                val sign = if (avgVelocity > 0) 1f else -1f
                                val absV = abs(avgVelocity)
                                val minRotation = 1440f
                                val current = rotation.value
                                val target = current + sign * maxOf(absV * 1.5f, minRotation)
                                val animDuration = maxOf(2000, (absV * 1.5f / 720f * 2000f).toInt()).coerceAtMost(5000)

                                isFling = true
                                audioHaptic.playFeedback(SoundEffect.COIN_DRAG)
                                coroutineScope.launch {
                                    rotation.animateTo(target, tween(animDuration, easing = StandardEasing.EaseOutQuart))

                                    // fling 结束后非线性减速转到最近的目标面
                                    val endAngle = rotation.value
                                    val endNorm = ((endAngle % 360f) + 360f) % 360f
                                    val newFace = if (endNorm in 90f..270f) CoinSide.TAILS else CoinSide.HEADS
                                    var snap = faceAngle(newFace)
                                    while (snap - endAngle > 180f) snap -= 360f
                                    while (snap - endAngle < -180f) snap += 360f
                                    rotation.animateTo(snap, tween(400, easing = StandardEasing.EaseOutQuart))
                                    settledAngle = faceAngle(newFace)

                                    highlightTrigger++
                                    isFling = false
                                    onDragFlipComplete(newFace)
                                }
                            } else {
                                // ===== 慢速拖拽：中线判定 =====
                                val currentAngle = rotation.value
                                val currentNorm = ((currentAngle % 360f) + 360f) % 360f
                                val startNorm = ((settledAngle % 360f) + 360f) % 360f
                                val startBack = startNorm in 90f..270f
                                val endBack = currentNorm in 90f..270f

                                if (startBack != endBack) {
                                    val newFace = if (endBack) CoinSide.TAILS else CoinSide.HEADS
                                    val targetAngle = faceAngle(newFace)
                                    var snapTarget = targetAngle
                                    while (snapTarget - currentAngle > 180f) snapTarget -= 360f
                                    while (snapTarget - currentAngle < -180f) snapTarget += 360f

                                    coroutineScope.launch {
                                        rotation.animateTo(snapTarget, tween(300, easing = StandardEasing.EaseOutQuart))
                                        settledAngle = targetAngle
                                        onDragFlipComplete(newFace)
                                    }
                                } else {
                                    var snapBack = settledAngle
                                    while (snapBack - currentAngle > 180f) snapBack -= 360f
                                    while (snapBack - currentAngle < -180f) snapBack += 360f

                                    coroutineScope.launch {
                                        rotation.animateTo(snapBack, tween(200, easing = StandardEasing.EaseOutQuart))
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            if (anyAnimating) {
                                isDragging = false
                                return@detectDragGestures
                            }
                            isDragging = false
                            val currentAngle = rotation.value
                            var snapBack = settledAngle
                            while (snapBack - currentAngle > 180f) snapBack -= 360f
                            while (snapBack - currentAngle < -180f) snapBack += 360f

                            coroutineScope.launch {
                                rotation.animateTo(snapBack, tween(200, easing = StandardEasing.EaseOutQuart))
                            }
                        }
                    )
                }
                .graphicsLayer {
                    rotationY = rotation.value
                    scaleX = scale.value
                    scaleY = scale.value
                    cameraDistance = 12f * density
                }
        ) {
            val cx = size.width / 2
            val cy = size.height / 2
            val coinR = size.width / 2
            val faceR = coinR * 0.94f
            val imgR = coinR * 0.88f

            // 地面阴影
            drawOval(
                color = Color.Black.copy(alpha = 0.25f),
                topLeft = Offset(cx - coinR * 0.85f, cy - coinR * 0.6f + 12f),
                size = Size(coinR * 1.7f, coinR * 1.0f)
            )

            // 硬币外缘
            drawCircle(Color(0xFFB0B0B0), radius = coinR, center = Offset(cx, cy))

            // 硬币面渐变底色
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD0D0D0), Color(0xFFA8A8A8)),
                    center = Offset(cx, cy),
                    radius = faceR
                ),
                radius = faceR,
                center = Offset(cx, cy)
            )

            // 修复：精准图片绘制 + 镜像补偿
            activeImage?.let { image ->
                val imgSize = (imgR * 2).toInt()
                val imgOffset = IntOffset((cx - imgR).toInt(), (cy - imgR).toInt())
                val circlePath = Path().apply {
                    addOval(Rect(cx - imgR, cy - imgR, cx + imgR, cy + imgR))
                }
                clipPath(circlePath, ClipOp.Intersect) {
                    if (needImageMirror) {
                        // 修复：仅在需要时执行水平镜像，抵消3D旋转翻转
                        drawContext.canvas.save()
                        drawContext.canvas.concat(
                            Matrix().apply {
                                translate(cx, cy)
                                scale(-1f, 1f)
                                translate(-cx, -cy)
                            }
                        )
                        drawImage(image, dstSize = IntSize(imgSize, imgSize), dstOffset = imgOffset)
                        drawContext.canvas.restore()
                    } else {
                        drawImage(image, dstSize = IntSize(imgSize, imgSize), dstOffset = imgOffset)
                    }
                }
            }

            // 高光扫过（反面时反镜像抵消 graphicsLayer 翻转，保持左上→右下方向）
            val hl = highlight.value
            if (hl > 0f) {
                val bandPos = -coinR * 2f + hl * coinR * 4f
                val bandW = coinR * 0.5f
                if (showTails) {
                    drawContext.canvas.save()
                    drawContext.canvas.concat(
                        Matrix().apply {
                            translate(cx, cy)
                            scale(-1f, 1f)
                            translate(-cx, -cy)
                        }
                    )
                }
                clipPath(Path().apply { addOval(Rect(cx - coinR, cy - coinR, cx + coinR, cy + coinR)) }) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.5f),
                                Color.Transparent
                            ),
                            start = Offset(cx + bandPos - bandW, cy + bandPos - bandW),
                            end = Offset(cx + bandPos + bandW, cy + bandPos + bandW)
                        ),
                        topLeft = Offset(cx - coinR, cy - coinR),
                        size = Size(coinR * 2, coinR * 2)
                    )
                }
                if (showTails) {
                    drawContext.canvas.restore()
                }
            }
        }
    }
}

/** 将 CoinSide 映射到规范角度: HEADS = 0°, TAILS = 180° */
private fun faceAngle(face: CoinSide): Float = when (face) {
    CoinSide.HEADS -> 0f
    CoinSide.TAILS -> 180f
}