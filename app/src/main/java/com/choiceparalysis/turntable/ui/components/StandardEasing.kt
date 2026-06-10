package com.choiceparalysis.turntable.ui.components

import androidx.compose.animation.core.CubicBezierEasing

object StandardEasing {
    val EaseOutCubic = CubicBezierEasing(0.33f, 1.0f, 0.68f, 1.0f)
    val EaseInCubic = CubicBezierEasing(0.32f, 0.0f, 0.67f, 0.0f)
    val EaseOutQuart = CubicBezierEasing(0.25f, 1.0f, 0.5f, 1.0f)
    val EaseInQuart = CubicBezierEasing(0.5f, 0.0f, 0.75f, 0.0f)
    val EaseInOutQuart = CubicBezierEasing(0.76f, 0.0f, 0.24f, 1.0f)
    val EaseInOutCubic = CubicBezierEasing(0.65f, 0.0f, 0.35f, 1.0f)
}
