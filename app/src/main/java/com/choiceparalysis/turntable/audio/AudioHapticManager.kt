package com.choiceparalysis.turntable.audio

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
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
    COIN_CLINK(R.raw.coin_clink),
    DICE_TAP(R.raw.dice_tap),
    YESNO_CHIME(R.raw.yesno_chime),
    ELIMINATION_DRUM(R.raw.elimination_drum),
    WINNER_CHEER(R.raw.winner_cheer),
}

enum class HapticType {
    SPIN_TICK,
    RESULT_HIT,
    COIN_FLIP,
    DICE_BOUNCE,
    ELIMINATION,
    WINNER,
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

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        scope.launch {
            settingsRepository.soundEnabled.collect { _soundEnabled.value = it }
        }
        scope.launch {
            settingsRepository.hapticEnabled.collect { _hapticEnabled.value = it }
        }
    }

    fun loadSounds() {
        if (loaded) return
        SoundEffect.entries.forEach { effect ->
            soundMap[effect] = soundPool.load(context, effect.resId, 1)
        }
        loaded = true
    }

    fun playSound(effect: SoundEffect) {
        if (!_soundEnabled.value) return
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        if (!loaded) loadSounds()
        val soundId = soundMap[effect] ?: return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun performHaptic(type: HapticType, view: View? = null) {
        if (!_hapticEnabled.value) return
        when (type) {
            HapticType.SPIN_TICK -> {
                view?.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                    ?: vibrateTick()
            }
            HapticType.RESULT_HIT -> {
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    ?: vibrateHeavy()
            }
            HapticType.COIN_FLIP -> {
                view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    ?: vibrateMedium()
            }
            HapticType.DICE_BOUNCE -> {
                view?.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                    ?: vibrateTick()
            }
            HapticType.ELIMINATION -> {
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    ?: vibrateHeavy()
            }
            HapticType.WINNER -> {
                vibratePattern(longArrayOf(0, 100, 50, 100, 50, 200))
            }
        }
    }

    private fun vibrateTick() {
        vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateMedium() {
        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateHeavy() {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibratePattern(pattern: LongArray) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    fun release() {
        soundPool.release()
    }
}
