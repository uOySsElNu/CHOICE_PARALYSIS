package com.choiceparalysis.turntable.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Calendar
import kotlin.math.roundToInt

object TimeBasedColorGenerator {

    data class TimeAnchor(
        val hour: Int,
        val baseHue: Float,
        val baseSaturation: Float,
        val baseLightness: Float,
        val borderColor: Color,
        val textColor: Color,
        val indicatorColor: Color,
    )

    private val anchors = listOf(
        TimeAnchor(5, 20f, 0.6f, 0.85f, Color.White, Color.White, Color(0xFFFFB74D)),   // Dawn: soft pink-orange
        TimeAnchor(7, 210f, 0.4f, 0.88f, Color.White, Color.White, Color(0xFF64B5F6)),  // Morning: light blue
        TimeAnchor(11, 45f, 0.7f, 0.90f, Color.White, Color.White, Color(0xFFFFD54F)),  // Noon: bright golden
        TimeAnchor(14, 25f, 0.75f, 0.85f, Color.White, Color.White, Color(0xFFFF8A65)), // Afternoon: warm orange
        TimeAnchor(17, 10f, 0.8f, 0.75f, Color.White, Color.White, Color(0xFFE57373)),  // Evening: deep orange-red
        TimeAnchor(20, 260f, 0.5f, 0.45f, Color(0xFFE0E0E0), Color.White, Color(0xFF7E57C2)), // Night: deep blue-purple
        TimeAnchor(24, 260f, 0.5f, 0.45f, Color(0xFFE0E0E0), Color.White, Color(0xFF7E57C2)), // Wrap to night
    )

    fun generateColorScheme(optionCount: Int): WheelColorScheme {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val currentHourFloat = hour + minute / 60f

        val (prev, next, fraction) = interpolateAnchors(currentHourFloat)

        val baseHue = lerpAngle(prev.baseHue, next.baseHue, fraction)
        val baseSat = lerpFloat(prev.baseSaturation, next.baseSaturation, fraction)
        val baseLight = lerpFloat(prev.baseLightness, next.baseLightness, fraction)
        val borderColor = lerpColor(prev.borderColor, next.borderColor, fraction)
        val textColor = lerpColor(prev.textColor, next.textColor, fraction)
        val indicatorColor = lerpColor(prev.indicatorColor, next.indicatorColor, fraction)

        val segmentColors = (0 until optionCount).map { index ->
            val hueOffset = index * (360f / optionCount.coerceAtLeast(1))
            val hue = (baseHue + hueOffset) % 360f
            val satVariation = 0.05f * (index % 3)
            val lightVariation = 0.03f * (index % 2)
            Color.hsl(
                hue = hue,
                saturation = (baseSat + satVariation).coerceIn(0f, 1f),
                lightness = (baseLight + lightVariation).coerceIn(0f, 1f),
            )
        }

        return WheelColorScheme(
            segmentColors = segmentColors,
            borderColor = borderColor,
            textColor = textColor,
            indicatorColor = indicatorColor,
        )
    }

    private fun interpolateAnchors(hour: Float): Triple<TimeAnchor, TimeAnchor, Float> {
        for (i in 0 until anchors.size - 1) {
            if (hour >= anchors[i].hour && hour < anchors[i + 1].hour) {
                val range = anchors[i + 1].hour - anchors[i].hour
                val fraction = (hour - anchors[i].hour) / range
                return Triple(anchors[i], anchors[i + 1], fraction)
            }
        }
        return Triple(anchors.last(), anchors.last(), 0f)
    }

    private fun lerpFloat(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var diff = b - a
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return (a + diff * t + 360f) % 360f
    }

    private fun lerpColor(a: Color, b: Color, t: Float): Color {
        return Color(
            red = lerpFloat(a.red, b.red, t),
            green = lerpFloat(a.green, b.green, t),
            blue = lerpFloat(a.blue, b.blue, t),
            alpha = lerpFloat(a.alpha, b.alpha, t),
        )
    }
}

data class WheelColorScheme(
    val segmentColors: List<Color>,
    val borderColor: Color,
    val textColor: Color,
    val indicatorColor: Color,
) {
    fun getColorForIndex(index: Int): Color = segmentColors[index % segmentColors.size]
}
