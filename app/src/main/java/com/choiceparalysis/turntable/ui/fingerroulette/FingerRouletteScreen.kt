package com.choiceparalysis.turntable.ui.fingerroulette

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.audio.HapticType
import com.choiceparalysis.turntable.audio.SoundEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerRouletteScreen(
    modifier: Modifier = Modifier,
    viewModel: FingerRouletteViewModel = viewModel(),
) {
    val fingers by viewModel.fingers.collectAsState()
    val phase by viewModel.phase.collectAsState()
    val winnerId by viewModel.winnerId.collectAsState()
    val audioHaptic = AudioHapticManager.getInstance(LocalContext.current)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val markerRadius = with(LocalDensity.current) { 36.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary

    // Winner animation
    val winnerScale = remember { Animatable(1f) }
    LaunchedEffect(winnerId) {
        if (winnerId != null) {
            audioHaptic.playSound(SoundEffect.WINNER_CHEER)
            audioHaptic.performHaptic(HapticType.WINNER)
            winnerScale.animateTo(2f, tween(600))
            winnerScale.animateTo(1.5f, tween(300))
        }
    }

    // Elimination feedback
    var lastEliminatedCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(fingers) {
        val eliminatedCount = fingers.count { it.isEliminated }
        if (eliminatedCount > lastEliminatedCount && phase == RoulettePhase.PLAYING) {
            audioHaptic.playSound(SoundEffect.ELIMINATION_DRUM)
            audioHaptic.performHaptic(HapticType.ELIMINATION)
        }
        lastEliminatedCount = eliminatedCount
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "指尖轮盘",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(phase) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        val fingerId = viewModel.addFinger(
                            down.position.x,
                            down.position.y
                        )
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.find { it.id == pointerId }
                                if (pointer == null || !pointer.pressed) {
                                    viewModel.removeFinger(fingerId)
                                    break
                                } else {
                                    viewModel.updateFingerPosition(
                                        fingerId,
                                        pointer.position.x,
                                        pointer.position.y
                                    )
                                    pointer.consume()
                                }
                            }
                        } catch (_: Exception) {
                            viewModel.removeFinger(fingerId)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                fingers.forEach { finger ->
                    if (finger.isEliminated) return@forEach
                    val scale = if (finger.id == winnerId) winnerScale.value else 1f
                    val radius = markerRadius * scale
                    val color = when {
                        finger.id == winnerId -> Color(0xFFFFD700) // Gold
                        finger.isCurrentlyTargeted -> Color.Red
                        else -> primaryColor
                    }

                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(finger.x, finger.y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = radius,
                        center = Offset(finger.x, finger.y),
                        style = Stroke(width = 3f)
                    )

                    val number = fingers.indexOf(finger) + 1
                    val textResult = textMeasurer.measure(
                        text = number.toString(),
                        style = TextStyle(
                            fontSize = (16 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    drawText(
                        textResult,
                        topLeft = Offset(
                            finger.x - textResult.size.width / 2,
                            finger.y - textResult.size.height / 2
                        )
                    )
                }
            }

            // Winner overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = phase == RoulettePhase.FINISHED,
                enter = fadeIn() + scaleIn(initialScale = 0.5f),
                exit = fadeOut() + scaleOut(targetScale = 0.5f),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "天选之人！",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.reset() }) {
                        Text("再来一局")
                    }
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val activeCount = fingers.count { !it.isEliminated }
            val statusText = when (phase) {
                RoulettePhase.WAITING -> "请放置 2-6 根手指"
                RoulettePhase.READY -> "检测到 $activeCount 根手指"
                RoulettePhase.PLAYING -> "淘汰中... 剩余 $activeCount 人"
                RoulettePhase.FINISHED -> "游戏结束"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.startElimination() },
                enabled = phase == RoulettePhase.READY
            ) {
                Text("开始淘汰")
            }
        }
    }
}
