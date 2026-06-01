package com.choiceparalysis.turntable.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.choiceparalysis.turntable.R
import dagger.hilt.android.EntryPointAccessors
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

class AudioHapticManager private constructor(
    private val context: Context,
    private val hapticEngine: HapticEngine,
) {

    companion object {
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
     */
    fun playFeedback(effect: SoundEffect) {
        // WHEEL_TICK: haptic only (no sound), pure boundary click
        if (effect == SoundEffect.WHEEL_TICK) {
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
                    val streamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                    if (streamId == 0) {
                        scope.launch {
                            kotlinx.coroutines.delay(50)
                            try { soundPool.play(soundId, 1f, 1f, 1, 0, 1f) } catch (_: Exception) {}
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (hapticReady) {
            try { hapticEngine.playEffect(mapToHapticEffect(effect)) } catch (_: Exception) {}
        }
    }

    private fun mapToHapticEffect(effect: SoundEffect): HapticEffect = when (effect) {
        SoundEffect.WHEEL_TICK -> HapticEffect.TICK
        SoundEffect.SPIN_DING -> HapticEffect.CLICK
        SoundEffect.YESNO_CHIME -> HapticEffect.RISE
        SoundEffect.ELIMINATION_DRUM -> HapticEffect.CLICK
        SoundEffect.WINNER_CHEER -> HapticEffect.CELEBRATION
        SoundEffect.DICE_ROLL -> HapticEffect.THUD
        SoundEffect.COIN_BUTTON -> HapticEffect.CELEBRATION
        SoundEffect.COIN_DRAG -> HapticEffect.CELEBRATION
    }

    fun release() {
        soundPool.release()
        hapticEngine.release()
    }
}
