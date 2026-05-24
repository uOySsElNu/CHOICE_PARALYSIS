package com.choiceparalysis.turntable.audio

import android.content.Context
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

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
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
        // Pre-load sounds so first play() doesn't fail due to async loading
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
     * Unified feedback: plays sound and haptic simultaneously.
     * Haptic pattern is designed to match the sound envelope.
     */
    fun playFeedback(effect: SoundEffect) {
        try {
            // Sound
            if (_soundEnabled.value && audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                if (!loaded) loadSounds()
                val soundId = soundMap[effect]
                if (soundId != null && soundId != 0) {
                    val streamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                    // If play returns 0, sound not loaded yet - retry once after short delay
                    if (streamId == 0) {
                        scope.launch {
                            kotlinx.coroutines.delay(50)
                            try {
                                soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            // Haptic - synced with sound envelope
            if (_hapticEnabled.value) {
                playHapticFor(effect)
            }
        } catch (_: Exception) {}
    }

    private fun playHapticFor(effect: SoundEffect) {
        if (supportsComposition) {
            playCompositionHaptic(effect)
        } else {
            playLegacyHaptic(effect)
        }
    }

    /**
     * Composition API (API 30+): uses haptic primitives that match each sound's character.
     * These primitives drive the X-axis linear motor with precise timing.
     */
    private fun playCompositionHaptic(effect: SoundEffect) {
        val composition = VibrationEffect.startComposition()

        when (effect) {
            SoundEffect.SPIN_DING -> {
                // Metallic ding: sharp click at impact, matching the 0.8s decay
                composition
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 80)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.3f, 200)
            }
            SoundEffect.YESNO_CHIME -> {
                // Mysterious chime: slow rise matching the ascending notes over 1.0s
                composition
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.7f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 350)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 500)
            }
            SoundEffect.ELIMINATION_DRUM -> {
                // Tense drum: heavy impact matching the 0.4s boom
                composition
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.8f, 60)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.4f, 150)
            }
            SoundEffect.WINNER_CHEER -> {
                // Celebration: quick rise + triple clicks matching the 1.2s chord
                composition
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.6f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 200)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 350)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 500)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 700)
            }
            SoundEffect.WHEEL_TICK -> {
                // Mechanical tick: single sharp tick for segment boundary crossing
                composition
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f, 0)
            }
            SoundEffect.DICE_ROLL -> {
                // 5 impacts matching real recording: 0ms, 180ms, 350ms, 500ms, 630ms
                composition
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.7f, 180)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.45f, 350)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.25f, 500)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.12f, 630)
            }
            SoundEffect.COIN_BUTTON -> {
                // 2s combined: flick at start, settling at end
                composition
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f, 800)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.6f, 1700)
            }
            SoundEffect.COIN_DRAG -> {
                // 4.8s combined: flick + air + landing
                composition
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.7f, 2500)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f, 2800)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f, 3200)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.12f, 630)
            }
        }

        vibrator.vibrate(composition.compose())
    }

    /**
     * Legacy fallback (pre-API 30): waveform-based patterns.
     */
    private fun playLegacyHaptic(effect: SoundEffect) {
        when (effect) {
            SoundEffect.SPIN_DING -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 30, 80, 15),
                    intArrayOf(255, 0, 180, 0), -1
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
            SoundEffect.WHEEL_TICK -> {
                vibrator.vibrate(VibrationEffect.createOneShot(8, 150))
            }
            SoundEffect.DICE_ROLL -> {
                // 5 impacts at 0, 180, 350, 500, 630ms
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 30, 150, 30, 140, 25, 125, 20, 110),
                    intArrayOf(255, 0, 180, 0, 120, 0, 70, 0, 30), -1
                ))
            }
            SoundEffect.COIN_BUTTON -> {
                vibrator.vibrate(VibrationEffect.createOneShot(15, 200))
            }
            SoundEffect.COIN_DRAG -> {
                vibrator.vibrate(VibrationEffect.createOneShot(15, 200))
            }
        }
    }

    /**
     * Haptic-only feedback (no sound). Used for continuous touch feedback like spin wheel drag.
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
