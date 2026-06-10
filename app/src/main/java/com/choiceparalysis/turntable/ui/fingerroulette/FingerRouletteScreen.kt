package com.choiceparalysis.turntable.ui.fingerroulette

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.audio.SoundEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerRouletteScreen(
    modifier: Modifier = Modifier,
    viewModel: FingerRouletteViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val fingers by viewModel.fingers.collectAsState()
    val phase by viewModel.phase.collectAsState()
    val winnerId by viewModel.winnerId.collectAsState()
    val targetFlash by viewModel.targetFlash.collectAsState()
    val context = LocalContext.current
    val audioHaptic = remember { AudioHapticManager.getInstance(context) }
    val markerRadius = with(LocalDensity.current) { 52.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary

    // Winner animation
    val winnerScale = remember { Animatable(1f) }
    LaunchedEffect(winnerId) {
        if (winnerId != null) {
            audioHaptic.playFeedback(SoundEffect.WINNER_CHEER)
            winnerScale.animateTo(2f, tween(600))
            winnerScale.animateTo(1.5f, tween(300))
        }
    }

    // Target flash haptic
    LaunchedEffect(targetFlash) {
        if (targetFlash > 0) audioHaptic.playHapticTick()
    }

    // Elimination feedback
    var lastEliminatedCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(fingers) {
        val eliminatedCount = fingers.count { it.isEliminated }
        if (eliminatedCount > lastEliminatedCount && phase == RoulettePhase.PLAYING) {
            audioHaptic.playFeedback(SoundEffect.ELIMINATION_DRUM)
        }
        lastEliminatedCount = eliminatedCount
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.finger_roulette_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    // Map PointerId → our finger ID
                    val pointerToFinger = mutableMapOf<PointerId, Int>()

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val currentIds = mutableSetOf<PointerId>()

                            for (change in event.changes) {
                                if (!change.pressed) continue
                                currentIds.add(change.id)

                                val existingFingerId = pointerToFinger[change.id]
                                if (existingFingerId != null) {
                                    // Existing finger → update position
                                    viewModel.updateFingerPosition(
                                        existingFingerId,
                                        change.position.x,
                                        change.position.y
                                    )
                                } else {
                                    // New finger → add
                                    val fingerId = viewModel.addFinger(
                                        change.position.x,
                                        change.position.y
                                    )
                                    pointerToFinger[change.id] = fingerId
                                    audioHaptic.playHapticTick()
                                }
                                change.consume()
                            }

                            // Remove fingers that are no longer pressed
                            val released = pointerToFinger.keys - currentIds
                            for (pointerId in released) {
                                pointerToFinger[pointerId]?.let { viewModel.removeFinger(it) }
                                pointerToFinger.remove(pointerId)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .semantics {
                    val activeCount = fingers.count { !it.isEliminated }
                    contentDescription = if (activeCount > 0) {
                        context.getString(R.string.status_ready_count, activeCount)
                    } else {
                        context.getString(R.string.status_waiting_fingers)
                    }
                }
            ) {
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
                        style = Stroke(width = 4f)
                    )

                    val number = fingers.indexOf(finger) + 1
                    val textResult = textMeasurer.measure(
                        text = number.toString(),
                        style = TextStyle(
                            fontSize = (22 * scale).sp,
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
                        text = stringResource(R.string.winner_chosen_one),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.reset() }) {
                        Text(stringResource(R.string.btn_play_again))
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
                RoulettePhase.WAITING -> stringResource(R.string.status_waiting_fingers)
                RoulettePhase.READY -> stringResource(R.string.status_ready_count, activeCount)
                RoulettePhase.PLAYING -> stringResource(R.string.status_eliminating, activeCount)
                RoulettePhase.FINISHED -> stringResource(R.string.status_game_over)
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
                Text(stringResource(R.string.btn_start_now))
            }
        }
    }
}
