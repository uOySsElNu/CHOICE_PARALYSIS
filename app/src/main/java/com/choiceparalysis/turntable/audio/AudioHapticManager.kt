package com.choiceparalysis.turntable.audio

import android.content.Context
import android.util.Log
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.data.datastore.dataStore
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
        private const val TAG = "AudioHaptic"
        @Volatile
        private var instance: AudioHapticManager? = null

        fun getInstance(context: Context): AudioHapticManager {
            return instance ?: synchronized(this) {
                instance ?: AudioHapticManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val settingsRepository = SettingsRepository(context.dataStore, context)
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
        .setMaxStreams(8)
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

    private val supportsComposition = checkCompositionSupport()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun checkCompositionSupport(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            // Check if the device actually supports composition primitives
            // 0809 motors and similar may not support them
            val v = vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val supported = v.arePrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                )
                supported.any { it }
            } else {
                // Pre-API 33: try a test vibration
                v.vibrate(VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.01f, 0)
                    .compose()
                )
                true
            }
        } catch (_: Exception) {
            false
        }
    }

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
        // WHEEL_TICK: haptic only (no sound), pure boundary click
        if (effect == SoundEffect.WHEEL_TICK) {
            if (_hapticEnabled.value) {
                try { playHapticFor(effect) } catch (_: Exception) {}
            }
            return
        }

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
        // WHEEL_TICK: MiHaptic tick for crisp boundary click
        if (effect == SoundEffect.WHEEL_TICK) {
            val mi = MiHapticEngine.isAvailable()
            Log.d(TAG, "WHEEL_TICK miHaptic=$mi composition=$supportsComposition")
            if (mi) {
                MiHapticEngine.playTick()
            } else if (supportsComposition) {
                playCompositionHaptic(effect)
            } else {
                playLegacyHaptic(effect)
            }
            return
        }
        // Priority: MiHaptic (Xiaomi) > Composition API > Legacy waveform
        val miAvailable = MiHapticEngine.isAvailable()
        if (miAvailable) {
            playMiHaptic(effect)
        } else if (supportsComposition) {
            playCompositionHaptic(effect)
        } else {
            playLegacyHaptic(effect)
        }
    }

    /**
     * MiHaptic (Xiaomi): finest-grained control with intensity + frequency.
     * Transient = sharp click, Continuous = sustained vibration with envelope.
     */
    private fun playMiHaptic(effect: SoundEffect) {
        val primitives = when (effect) {
            SoundEffect.WHEEL_TICK -> listOf(
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 80, 70)
            )
            SoundEffect.SPIN_DING -> listOf(
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 100, 50),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.CONTINUOUS, 40, 30, 80, 200)
            )
            SoundEffect.COIN_BUTTON -> listOf(
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 100, 60),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.CONTINUOUS, 80, 50, 200, 1400),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 100, 40, 1600),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 90, 35, 1800)
            )
            SoundEffect.COIN_DRAG -> listOf(
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 100, 60),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.CONTINUOUS, 70, 40, 400, 2000),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 100, 35, 2500),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 90, 30, 2800),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 80, 25, 3200)
            )
            SoundEffect.DICE_ROLL -> listOf(
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 100, 20),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 80, 25, 180),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 60, 30, 350),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 40, 35, 500),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 25, 40, 630)
            )
            SoundEffect.YESNO_CHIME -> listOf(
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.CONTINUOUS, 50, 60, 0, 600),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 100, 50, 350),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 60, 40, 500)
            )
            SoundEffect.ELIMINATION_DRUM -> listOf(
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 100, 10),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.CONTINUOUS, 60, 15, 30, 150)
            )
            SoundEffect.WINNER_CHEER -> listOf(
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.CONTINUOUS, 40, 50, 0, 800),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 100, 60, 200),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 80, 50, 350),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 100, 60, 500),
                MiHapticEngine.HapticPrimitive(MiHapticEngine.PrimitiveType.TRANSIENT, 50, 40, 700)
            )
        }
        MiHapticEngine.playComposed(primitives)
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
     * Legacy fallback: waveform patterns with max amplitude for small motors (0809 etc).
     * All amplitudes are 200-255 to ensure the motor responds.
     */
    private fun playLegacyHaptic(effect: SoundEffect) {
        when (effect) {
            SoundEffect.WHEEL_TICK -> {
                vibrator.vibrate(VibrationEffect.createOneShot(15, 255))
            }
            SoundEffect.SPIN_DING -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 40, 60, 25),
                    intArrayOf(255, 0, 220, 0), -1
                ))
            }
            SoundEffect.COIN_BUTTON -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 20, 580, 40, 160, 30),
                    intArrayOf(255, 0, 220, 0, 200, 0), -1
                ))
            }
            SoundEffect.COIN_DRAG -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 20, 780, 15, 785, 40, 260, 30, 370),
                    intArrayOf(255, 0, 200, 0, 220, 0, 200, 0, 200), -1
                ))
            }
            SoundEffect.DICE_ROLL -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 40, 140, 35, 135, 30, 120, 25, 105),
                    intArrayOf(255, 0, 220, 0, 200, 0, 180, 0, 160), -1
                ))
            }
            SoundEffect.YESNO_CHIME -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 250, 80, 250),
                    intArrayOf(200, 0, 255, 0), -1
                ))
            }
            SoundEffect.ELIMINATION_DRUM -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 60, 20, 100),
                    intArrayOf(255, 0, 220, 0), -1
                ))
            }
            SoundEffect.WINNER_CHEER -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 180, 60, 40, 40, 40, 40, 40),
                    intArrayOf(200, 0, 255, 0, 220, 0, 255, 0), -1
                ))
            }
        }
    }

    /**
     * Haptic-only feedback for continuous touch (spin wheel drag).
     * Uses direct vibrator for minimum latency.
     */
    fun tick() {
        try {
            if (!_hapticEnabled.value) return
            vibrator.vibrate(VibrationEffect.createOneShot(15, 255))
        } catch (_: Exception) {}
    }

    fun release() {
        soundPool.release()
    }
}
