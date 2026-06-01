package com.choiceparalysis.turntable.audio

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Haptic engine using VibrationEffect.Composition API (Android 11+).
 * Each pattern matches audio frequency characteristics using composition primitives:
 *   TICK — high freq clicks (3-5kHz), light/fast
 *   CLICK — mid freq impacts (1-2kHz), medium/tactile
 *   LOW_TICK — low freq thuds (200-500Hz), heavy/slow
 *   THUD — sub-bass booms (50-100Hz), heaviest
 *   SLOW_RISE — ascending tone, tension build
 *   QUICK_RISE — sharp crescendo, excitement
 */
class CompositionEngine(private val vibrator: Vibrator) : HapticEngine {

    private val supported: Boolean = checkSupport()

    private fun checkSupport(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val result = vibrator.arePrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                )
                result.any { it }
            } else {
                vibrator.vibrate(VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.01f, 0)
                    .compose()
                )
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun isAvailable(): Boolean = supported

    override fun playTick() {
        val c = VibrationEffect.startComposition()
        c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f, 0)
        vibrator.vibrate(c.compose())
    }

    override fun playEffect(effect: HapticEffect) {
        val c = VibrationEffect.startComposition()
        when (effect) {
            HapticEffect.TICK -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f, 0)
            }
            HapticEffect.CLICK -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 80)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.3f, 200)
            }
            HapticEffect.THUD -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.7f, 180)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.45f, 350)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.25f, 500)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.12f, 630)
            }
            HapticEffect.RISE -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.7f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 350)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 500)
            }
            HapticEffect.CELEBRATION -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.6f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 200)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 350)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 500)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f, 700)
            }
        }
        vibrator.vibrate(c.compose())
    }

    override fun release() {
        // No resources to release
    }
}
