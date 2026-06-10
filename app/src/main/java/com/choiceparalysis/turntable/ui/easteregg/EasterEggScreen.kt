package com.choiceparalysis.turntable.ui.easteregg

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp

private data class Lane(
    val texts: List<String>,
    val fontSize: Float,
    val speed: Float,
    val alpha: Float,
    val fontWeight: FontWeight,
)

private val appName = listOf(
    "就决定是你了",
    "It's you!",
    "君に決めた",
    "너로 정했어",
    "¡Eres tú!",
    "C'est toi !",
    "Du bist es!",
    "Это ты!",
    "É você!",
    "أنت المختار!",
    "就決定是你了",
)

// Lanes fill the screen vertically, each with multiple texts for continuous scrolling
private val lanes = listOf(
    Lane(listOf(appName[0], appName[3], appName[6]), 42f, 350f, 0.9f, FontWeight.Bold),
    Lane(listOf(appName[1], appName[4], appName[8]), 28f, 280f, 0.7f, FontWeight.Medium),
    Lane(listOf(appName[2], appName[10], appName[0]), 48f, 400f, 0.95f, FontWeight.Bold),
    Lane(listOf(appName[5], appName[7], appName[1]), 32f, 320f, 0.8f, FontWeight.Medium),
    Lane(listOf(appName[9], appName[3], appName[6]), 24f, 250f, 0.65f, FontWeight.Light),
    Lane(listOf(appName[10], appName[0], appName[4]), 44f, 380f, 0.9f, FontWeight.Bold),
    Lane(listOf(appName[1], appName[8], appName[2]), 36f, 300f, 0.85f, FontWeight.Medium),
    Lane(listOf(appName[6], appName[5], appName[9]), 22f, 260f, 0.6f, FontWeight.Light),
    Lane(listOf(appName[7], appName[10], appName[3]), 40f, 360f, 0.9f, FontWeight.Bold),
    Lane(listOf(appName[4], appName[2], appName[7]), 30f, 290f, 0.75f, FontWeight.Medium),
    Lane(listOf(appName[0], appName[9], appName[1]), 46f, 370f, 0.95f, FontWeight.Bold),
    Lane(listOf(appName[3], appName[6], appName[5]), 26f, 270f, 0.7f, FontWeight.Light),
    Lane(listOf(appName[8], appName[10], appName[0]), 38f, 340f, 0.85f, FontWeight.Bold),
    Lane(listOf(appName[2], appName[1], appName[9]), 34f, 310f, 0.8f, FontWeight.Medium),
)

@Composable
fun EasterEggScreen(
    followSystemTheme: Boolean = true,
    darkMode: Boolean = false,
) {
    val isDark = if (followSystemTheme) isSystemInDarkTheme() else darkMode

    val bgColor = if (isDark) Color.Black else Color.White
    val baseColor = if (isDark) Color.White else Color.Black

    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "danmaku")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .onSizeChanged { canvasSize = it }
    ) {
        if (canvasSize.width > 0) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val laneHeight = h / lanes.size

                // Pre-measure all texts
                val measured = lanes.map { lane ->
                    lane to lane.texts.map { text ->
                        textMeasurer.measure(
                            text = text,
                            style = TextStyle(
                                fontSize = lane.fontSize.sp,
                                fontWeight = lane.fontWeight,
                                color = baseColor.copy(alpha = lane.alpha)
                            )
                        )
                    }
                }

                measured.forEachIndexed { laneIndex, (lane, layouts) ->
                    val y = laneIndex * laneHeight + (laneHeight - layouts.first().size.height) / 2

                    // Total width of all texts in this lane with spacing
                    val spacing = 80f
                    val totalTextWidth = layouts.sumOf { it.size.width } + spacing * (layouts.size - 1)
                    val totalCycle = totalTextWidth + w  // full cycle distance

                    // Offset based on speed and time
                    val offset = (animTime * lane.speed * 20) % totalCycle

                    // Draw texts repeatedly to fill the cycle
                    var xPos = -offset
                    while (xPos < w) {
                        for (layout in layouts) {
                            if (xPos + layout.size.width > 0 && xPos < w) {
                                drawText(layout, topLeft = Offset(xPos, y))
                            }
                            xPos += layout.size.width + spacing
                        }
                    }
                    // Draw one more pass to cover the gap
                    xPos = -offset + totalTextWidth + spacing
                    for (layout in layouts) {
                        if (xPos + layout.size.width > 0 && xPos < w) {
                            drawText(layout, topLeft = Offset(xPos, y))
                        }
                        xPos += layout.size.width + spacing
                    }
                }
            }
        }
    }
}
