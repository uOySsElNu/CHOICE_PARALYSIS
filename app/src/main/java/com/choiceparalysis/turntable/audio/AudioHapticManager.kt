package com.choiceparalysis.turntable.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SoundEffect(val resId: Int) {
    SPIN_DING(R.raw.spin_ding),
    YESNO_CHIME(R.raw.yesno_chime),
    ELIMINATION_DRUM(R.raw.elimination_drum),
    WINNER_CHEER(R.raw.winner_cheer),
    WHEEL_TICK(R.raw.wheel_tick),
    DICE_ROLL(R.raw.dice_roll),
    COIN_BUTTON(R.raw.coin_button),
    COIN_DRAG(R.raw.coin_drag),
}

class AudioHapticManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: AudioHapticManager? = null

        fun getInstance(context: Context): AudioHapticManager {
            return instance ?: synchronized(this) {
                instance ?: AudioHapticManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val settingsRepository = SettingsRepository(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    // Low-latency audio path: USAGE_GAME routes through fast mixer
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(audioAttributes)
        .build()

    private val soundMap = mutableMapOf<SoundEffect, Int>()
    private var loaded = false
    private var loadPending = 0

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val supportsComposition = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        scope.launch {
            settingsRepository.soundEnabled.collect { _soundEnabled.value = it }
        }
        scope.launch {
            settingsRepository.hapticEnabled.collect { _hapticEnabled.value = it }
        }
        loadSounds()
    }

    fun loadSounds() {
        if (loaded || loadPending > 0) return
        loadPending = SoundEffect.entries.size
        soundPool.setOnLoadCompleteListener { _, _, _ ->
            loadPending--
            if (loadPending <= 0) {
                loaded = true
            }
        }
        SoundEffect.entries.forEach { effect ->
            soundMap[effect] = soundPool.load(context, effect.resId, 1)
        }
    }

    /**
     * Unified feedback: plays sound and haptic simultaneously on the same frame.
     * Haptic primitives are chosen based on each sound's frequency characteristics.
     */
    fun playFeedback(effect: SoundEffect) {
        // Trigger both on same frame for sync
        val soundReady = _soundEnabled.value && audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT
        val hapticReady = _hapticEnabled.value

        if (soundReady) {
            try {
                if (!loaded) loadSounds()
                val soundId = soundMap[effect]
                if (soundId != null && soundId != 0) {
                    val streamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                    if (streamId == 0) {
                        // Sound not loaded yet, retry
                        scope.launch {
                            kotlinx.coroutines.delay(50)
                            try { soundPool.play(soundId, 1f, 1f, 1, 0, 1f) } catch (_: Exception) {}
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (hapticReady) {
            try { playHapticFor(effect) } catch (_: Exception) {}
        }
    }

    private fun playHapticFor(effect: SoundEffect) {
        if (supportsComposition) playCompositionHaptic(effect)
        else playLegacyHaptic(effect)
    }

    /**
     * Composition API haptics — each pattern matches the audio's frequency/envelope.
     *
     * Primitive selection by frequency:
     *   TICK      — high freq clicks (3-5kHz), light/fast
     *   CLICK     — mid freq impacts (1-2kHz), medium/tactile
     *   LOW_TICK  — low freq thuds (200-500Hz), heavy/slow
     *   THUD      — sub-bass booms (50-100Hz), heaviest
     *   SLOW_RISE — ascending tone, tension build
     *   QUICK_RISE— sharp crescendo, excitement
     */
    private fun playCompositionHaptic(effect: SoundEffect) {
        val c = VibrationEffect.startComposition()

        when (effect) {
            // 4.2kHz click, 40ms, fast decay
            // High freq = light, sharp tick
            SoundEffect.WHEEL_TICK -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f, 0)
            }

            // 880Hz fundamental + 2640Hz harmonic, 0.8s exponential decay
            // Medium freq = resonant click at impact, tick shimmer on decay
            SoundEffect.SPIN_DING -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 80)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.3f, 200)
            }

            // 2.0s: metallic spin (3-4kHz) + landing (200-500Hz)
            // Start: light ticks for spin, end: heavy low_tick for landing
            SoundEffect.COIN_BUTTON -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f, 600)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.8f, 1600)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f, 1800)
            }

            // 4.8s: spin + air + landing
            // Sparse ticks during air, heavy impacts at landing
            SoundEffect.COIN_DRAG -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f, 800)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.2f, 1600)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.8f, 2500)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f, 2800)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f, 3200)
            }

            // 1.1s: 5 impacts at 200-500Hz, decreasing amplitude
            // Low freq = heavy thuds matching each bounce
            SoundEffect.DICE_ROLL -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.7f, 180)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.45f, 350)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.25f, 500)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.12f, 630)
            }

            // 1.0s: ascending C5-E5-G5 chord
            // Rising tone = slow_rise, resolution = click
            SoundEffect.YESNO_CHIME -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.7f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 350)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 500)
            }

            // 0.4s: 80Hz sub-bass boom
            // Lowest freq = heaviest impact: thud + click attack
            SoundEffect.ELIMINATION_DRUM -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f, 30)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.3f, 150)
            }

            // 1.2s: major chord + shimmer
            // Excitement = quick_rise, celebration = click sequence
            SoundEffect.WINNER_CHEER -> {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.6f, 0)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 200)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 350)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 500)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f, 700)
            }
        }

        vibrator.vibrate(c.compose())
    }

    /**
     * Legacy fallback (pre-API 30): waveform patterns approximating the composition.
     */
    private fun playLegacyHaptic(effect: SoundEffect) {
        when (effect) {
            SoundEffect.WHEEL_TICK -> {
                vibrator.vibrate(VibrationEffect.createOneShot(8, 150))
            }
            SoundEffect.SPIN_DING -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 30, 80, 15),
                    intArrayOf(255, 0, 180, 0), -1
                ))
            }
            SoundEffect.COIN_BUTTON -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 15, 585, 30, 170, 25),
                    intArrayOf(180, 0, 200, 0, 130, 0), -1
                ))
            }
            SoundEffect.COIN_DRAG -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 15, 785, 10, 790, 30, 270, 25, 375),
                    intArrayOf(180, 0, 80, 0, 200, 0, 130, 0, 80), -1
                ))
            }
            SoundEffect.DICE_ROLL -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 30, 150, 30, 140, 25, 125, 20, 110),
                    intArrayOf(255, 0, 180, 0, 120, 0, 70, 0, 30), -1
                ))
            }
            SoundEffect.YESNO_CHIME -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 200, 100, 200),
                    intArrayOf(100, 0, 255, 0), -1
                ))
            }
            SoundEffect.ELIMINATION_DRUM -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 40, 30, 80),
                    intArrayOf(255, 0, 200, 0), -1
                ))
            }
            SoundEffect.WINNER_CHEER -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 150, 80, 30, 50, 30, 50, 30),
                    intArrayOf(150, 0, 255, 0, 200, 0, 255, 0), -1
                ))
            }
        }
    }

    /**
     * Haptic-only feedback for continuous touch (spin wheel drag).
     */
    fun tick() {
        try {
            if (!_hapticEnabled.value) return
            if (supportsComposition) {
                vibrator.vibrate(VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 0)
                    .compose()
                )
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(10, 150))
            }
        } catch (_: Exception) {}
    }

    fun release() {
        soundPool.release()
    }
}
