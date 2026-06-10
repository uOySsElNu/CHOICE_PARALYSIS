package com.choiceparalysis.turntable.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.datastore.preferences.core.Preferences
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.data.datastore.DataStoreKeys
import com.choiceparalysis.turntable.data.datastore.dataStore
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

enum class SoundEffect(val resId: Int) {
    SPIN_DING(R.raw.spin_ding),
    YESNO_CHIME(R.raw.yesno_chime),
    ELIMINATION_DRUM(R.raw.elimination_drum),
    WINNER_CHEER(R.raw.winner_cheer),
    WHEEL_TICK(R.raw.wheel_tick),
    DICE_ROLL(R.raw.dice_roll),
    COIN_BUTTON(R.raw.coin_button),
}

class AudioHapticManager private constructor(
    private val context: Context,
    private val hapticEngine: HapticEngine,
) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: AudioHapticManager? = null

        fun getInstance(context: Context): AudioHapticManager {
            return instance ?: synchronized(this) {
                instance ?: AudioHapticManager(
                    context.applicationContext,
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        AudioHapticEntryPoint::class.java
                    ).hapticEngine()
                ).also { instance = it }
            }
        }
    }

    private val dataStore = context.dataStore
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Main)

    private val _soundEnabled = MutableStateFlow(true)

    private val _hapticEnabled = MutableStateFlow(true)

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

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        scope.launch {
            dataStore.data.collect { prefs: Preferences ->
                _soundEnabled.value = prefs[DataStoreKeys.SOUND_ENABLED]?.toBooleanStrictOrNull() ?: true
            }
        }
        scope.launch {
            dataStore.data.collect { prefs: Preferences ->
                _hapticEnabled.value = prefs[DataStoreKeys.HAPTIC_ENABLED]?.toBooleanStrictOrNull() ?: true
            }
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
     * Sound-only playback — no haptic. For when haptic is handled separately.
     */
    fun playSound(effect: SoundEffect) {
        val soundReady = _soundEnabled.value && audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT
        if (soundReady) {
            try {
                if (!loaded) loadSounds()
                val soundId = soundMap[effect]
                if (soundId != null && soundId != 0) {
                    soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Haptic-only tick — no sound. For syncing haptic to existing audio.
     */
    fun playHapticTick() {
        if (_hapticEnabled.value) {
            try { hapticEngine.playTick() } catch (_: Exception) {}
        }
    }

    /**
     * Haptic-only effect — no sound. For syncing haptic to existing audio.
     */
    fun playHapticEffect(effect: HapticEffect) {
        if (_hapticEnabled.value) {
            try { hapticEngine.playEffect(effect) } catch (_: Exception) {}
        }
    }

    /**
     * Coin spin haptic — simulates coin spinning on table surface.
     * Starts with a sharp flick, then rapid tapping that decelerates,
     * ending with a thud when the coin settles.
     */
    fun playCoinSpinHaptic(durationMs: Int) {
        if (!_hapticEnabled.value) return
        scope.launch {
            // 1. Initial flick impact
            try { hapticEngine.playEffect(HapticEffect.CLICK) } catch (_: Exception) {}

            // 2. Spinning phase — ticks that decelerate over the animation duration
            val spinStart = 30L      // fastest interval (ms)
            val spinEnd = 250L       // slowest interval (ms)
            val startTime = System.currentTimeMillis()
            val endTime = startTime + durationMs - 300L  // stop 300ms before end

            while (System.currentTimeMillis() < endTime) {
                // EaseOutQuart curve: fast at start, slow at end
                val progress = ((System.currentTimeMillis() - startTime).toFloat() / (durationMs - 300)).coerceIn(0f, 1f)
                val interval = spinStart + (spinEnd - spinStart) * progress.pow(3)
                delay(interval.toLong().milliseconds)
                try { hapticEngine.playTick() } catch (_: Exception) {}
            }

            // 3. Coin settles — heavy thud
            delay(100.milliseconds)
            try { hapticEngine.playEffect(HapticEffect.THUD) } catch (_: Exception) {}
        }
    }

    /**
     * Unified feedback: plays sound and haptic simultaneously on the same frame.
     */
    fun playFeedback(effect: SoundEffect) {
        // WHEEL_TICK: haptic + sound synchronized on boundary crossing
        if (effect == SoundEffect.WHEEL_TICK) {
            val soundReady = _soundEnabled.value && audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT
            if (soundReady) {
                try {
                    if (!loaded) loadSounds()
                    val soundId = soundMap[effect]
                    if (soundId != null && soundId != 0) {
                        soundPool.play(soundId, 0.6f, 0.6f, 1, 0, 1f)
                    }
                } catch (_: Exception) {}
            }
            if (_hapticEnabled.value) {
                try { hapticEngine.playTick() } catch (_: Exception) {}
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
                    soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                }
            } catch (_: Exception) {}
        }

        if (hapticReady) {
            val hapticEffect = mapToHapticEffect(effect)
            if (hapticEffect != null) {
                try { hapticEngine.playEffect(hapticEffect) } catch (_: Exception) {}
            }
        }
    }

    private fun mapToHapticEffect(effect: SoundEffect): HapticEffect? = when (effect) {
        SoundEffect.WHEEL_TICK -> HapticEffect.TICK
        SoundEffect.SPIN_DING -> HapticEffect.CLICK
        SoundEffect.YESNO_CHIME -> HapticEffect.RISE
        SoundEffect.ELIMINATION_DRUM -> HapticEffect.CLICK
        SoundEffect.WINNER_CHEER -> HapticEffect.CELEBRATION
        SoundEffect.DICE_ROLL -> null  // Haptic driven by waveform-synced ticks in Dice3DComposable
        SoundEffect.COIN_BUTTON -> null  // Haptic driven by playCoinSpinHaptic in Coin3DComposable
    }

}
