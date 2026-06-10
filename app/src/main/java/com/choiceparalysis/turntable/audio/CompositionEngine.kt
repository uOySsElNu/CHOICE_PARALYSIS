package com.choiceparalysis.turntable.audio

import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Haptic engine using VibrationEffect.Composition API (Android 11+).
 * 
 * Based on Android Composition API best practices and Xiaomi linear motor tuning:
 * - X-axis linear motor optimal frequency range: 50-500Hz
 * - Resonance frequency: ~130Hz
 * - Fast response: <5ms start/stop
 * 
 * Primitive characteristics:
 *   TICK — high freq clicks (3-5kHz), light/fast, high sharpness
 *   CLICK — mid freq impacts (1-2kHz), medium/tactile
 *   LOW_TICK — low freq thuds (200-500Hz), heavy/slow
 *   SLOW_RISE — ascending tone, tension build
 *   QUICK_RISE — sharp crescendo, excitement
 */
class CompositionEngine(private val vibrator: Vibrator) : HapticEngine {

    private val supported: Boolean = checkSupport()

    private fun checkSupport(): Boolean {
        return try {
            val result = vibrator.arePrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK
            )
            result.any { it }
        } catch (_: Exception) {
            false
        }
    }

    override fun isAvailable(): Boolean = supported

    /**
     * Picker/roller detent tick — constant, short, sharp.
     *
     * Tuning:
     * - TICK at 0.7 scale for crisp, consistent click
     * - Each tick identical; fast spin = more ticks/sec (natural frequency)
     */
    override fun playTick() {
        val c = VibrationEffect.startComposition()
        c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f, 0)
        vibrator.vibrate(c.compose())
    }

    override fun playEffect(effect: HapticEffect) {
        val c = VibrationEffect.startComposition()
        when (effect) {
            HapticEffect.TICK -> {
                // Wheel boundary crossing - crisp tick
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.55f, 0)
            }
            HapticEffect.CLICK -> {
                // Button press - sharp attack with satisfying decay
                // PRIMARY_CLICK: Main click sensation
                // SECONDARY_TICK: Subtle follow-through
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f, 60)
            }
            HapticEffect.THUD -> {
                // Heavy impact with natural decay - coin landing, dice roll
                // LOW_TICK provides deep bass feel for impact
                // TICK adds crispness to the decay
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.6f, 60)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.35f, 120)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.2f, 180)
            }
            HapticEffect.RISE -> {
                // Ascending tension - yes/no suspense building
                // SLOW_RISE creates tension build
                // CLICK/TICK accents create climax
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.6f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 200)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 400)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 550)
            }
            HapticEffect.CELEBRATION -> {
                // Festive rhythm - winner celebration
                // QUICK_RISE for opening burst
                // CLICK patterns for rhythmic celebration
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.7f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 100)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f, 220)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.95f, 350)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 500)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 650)
            }
        }
        vibrator.vibrate(c.compose())
    }

    override fun release() {
        // No resources to release
    }
}