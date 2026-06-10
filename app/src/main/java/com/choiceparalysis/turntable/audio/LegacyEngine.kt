package com.choiceparalysis.turntable.audio

import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Legacy haptic engine using basic Vibrator API.
 * Waveform patterns with max amplitude for small motors (0809 etc.).
 * All amplitudes are 200-255 to ensure the motor responses.
 */
class LegacyEngine(private val vibrator: Vibrator) : HapticEngine {

    override fun isAvailable(): Boolean = true

    /**
     * Picker/roller detent tick — short, sharp, constant.
     * 12ms at max amplitude for a crisp click feel.
     */
    override fun playTick() {
        vibrator.vibrate(VibrationEffect.createOneShot(12, 255))
    }

    override fun playEffect(effect: HapticEffect) {
        when (effect) {
            HapticEffect.TICK -> {
                vibrator.vibrate(VibrationEffect.createOneShot(15, 255))
            }
            HapticEffect.CLICK -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 40, 60, 25),
                    intArrayOf(255, 0, 220, 0), -1
                ))
            }
            HapticEffect.THUD -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 40, 140, 35, 135, 30, 120, 25, 105),
                    intArrayOf(255, 0, 220, 0, 200, 0, 180, 0, 160), -1
                ))
            }
            HapticEffect.RISE -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 250, 80, 250),
                    intArrayOf(200, 0, 255, 0), -1
                ))
            }
            HapticEffect.CELEBRATION -> {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 180, 60, 40, 40, 40, 40, 40),
                    intArrayOf(200, 0, 255, 0, 220, 0, 255, 0), -1
                ))
            }
        }
    }

    override fun release() {
        vibrator.cancel()
    }
}
