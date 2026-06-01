package com.choiceparalysis.turntable.audio

/**
 * Strategy interface for haptic feedback engines.
 * Implementations provide device-specific haptic feedback
 * (MiHaptic for Xiaomi, Composition API for Android 11+, legacy Vibrator fallback).
 */
interface HapticEngine {
    /** Whether this engine is available on the current device. */
    fun isAvailable(): Boolean

    /** Ultra-fast single tick for per-frame feedback (spin wheel boundary crossing). */
    fun playTick()

    /** Play a haptic pattern matching the given sound effect. */
    fun playEffect(effect: HapticEffect)

    /** Release any resources held by this engine. */
    fun release()
}

/**
 * Haptic effect types mapped from sound effects.
 * Each engine provides device-appropriate patterns for these.
 */
enum class HapticEffect {
    CLICK,       // sharp impact (spin ding, elimination drum)
    TICK,        // light high-freq tick (wheel tick)
    THUD,        // heavy low-freq impact (dice roll, coin landing)
    RISE,        // ascending tension (yes/no chime)
    CELEBRATION, // multi-hit celebration (winner cheer, coin button/drag)
}
