package com.choiceparalysis.turntable.ui.home

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.choiceparalysis.turntable.R

enum class WheelDesign(
    @StringRes val displayNameRes: Int,
    val emoji: String,
    val colors: List<Color>,
    val borderColor: Color,
    val textColor: Color,
    val indicatorColor: Color,
) {
    CLASSIC_RAINBOW(
        displayNameRes = R.string.design_classic_rainbow,
        emoji = "🌈",
        colors = listOf(
            Color(0xFFE91E63),
            Color(0xFF9C27B0),
            Color(0xFF673AB7),
            Color(0xFF3F51B5),
            Color(0xFF2196F3),
            Color(0xFF00BCD4),
            Color(0xFF009688),
            Color(0xFF4CAF50),
            Color(0xFFFFEB3B),
            Color(0xFFFF9800),
        ),
        borderColor = Color.White,
        textColor = Color.White,
        indicatorColor = Color(0xFFE91E63),
    ),
    MONOCHROME(
        displayNameRes = R.string.design_monochrome,
        emoji = "🎨",
        colors = listOf(
            Color(0xFF7C4DFF),
            Color(0xFF651FFF),
            Color(0xFF6200EA),
            Color(0xFF536DFE),
            Color(0xFF3D5AFE),
            Color(0xFF304FFE),
            Color(0xFF5C6BC0),
            Color(0xFF3949AB),
            Color(0xFF7986CB),
            Color(0xFF9FA8DA),
        ),
        borderColor = Color.White,
        textColor = Color.White,
        indicatorColor = Color(0xFF6200EA),
    ),
    WARM_SUNSET(
        displayNameRes = R.string.design_warm_sunset,
        emoji = "🌅",
        colors = listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFF8E72),
            Color(0xFFFFA07A),
            Color(0xFFFFB347),
            Color(0xFFFFC857),
            Color(0xFFFFD700),
            Color(0xFFFF9800),
            Color(0xFFFF7043),
            Color(0xFFE64A19),
            Color(0xFFD84315),
        ),
        borderColor = Color.White,
        textColor = Color.White,
        indicatorColor = Color(0xFFFF6B6B),
    ),
    OCEAN_BREEZE(
        displayNameRes = R.string.design_ocean_breeze,
        emoji = "🌊",
        colors = listOf(
            Color(0xFF00BCD4),
            Color(0xFF26C6DA),
            Color(0xFF4DD0E1),
            Color(0xFF00ACC1),
            Color(0xFF0097A7),
            Color(0xFF00838F),
            Color(0xFF006064),
            Color(0xFF00897B),
            Color(0xFF00695C),
            Color(0xFF004D40),
        ),
        borderColor = Color.White,
        textColor = Color.White,
        indicatorColor = Color(0xFF00BCD4),
    ),
    MINIMAL(
        displayNameRes = R.string.design_minimal,
        emoji = "⚪",
        colors = listOf(
            Color(0xFF424242),
            Color(0xFF616161),
            Color(0xFF757575),
            Color(0xFF9E9E9E),
            Color(0xFFBDBDBD),
            Color(0xFF424242),
            Color(0xFF616161),
            Color(0xFF757575),
            Color(0xFF9E9E9E),
            Color(0xFFBDBDBD),
        ),
        borderColor = Color(0xFFE0E0E0),
        textColor = Color.White,
        indicatorColor = Color(0xFF212121),
    );

    fun getColorForIndex(index: Int): Color = colors[index % colors.size]
}
