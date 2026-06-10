package com.choiceparalysis.turntable.audio

import android.content.Context
import com.apprichtap.haptic.RichTapUtils
import com.apprichtap.haptic.base.PrebakedEffectId

/**
 * RichTap SDK engine — official AAC Technologies haptic API.
 *
 * Uses the RichTap ASDK library which wraps DynamicEffect/HapticPlayer
 * with a higher-level API. Works on any device with RichTap-compatible
 * linear motor (Xiaomi, vivo, OPPO, etc.).
 *
 * Key advantages over raw DynamicEffect reflection:
 * - 50 prebaked effects with hardware-optimized waveforms
 * - HE files/JSON playback for complex patterns
 * - isSupportedRichTap check for capability detection
 * - Handles OEM differences internally
 */
class RichTapSdkEngineImpl(private val context: Context) : HapticEngine {

    private var initialized = false
    private var supported = false

    override fun isAvailable(): Boolean {
        if (!initialized) {
            initialized = true
            try {
                val utils = RichTapUtils.getInstance()
                utils.init(context)
                supported = utils.isSupportedRichTap
                if (!supported) {
                    utils.quit()
                }
            } catch (_: Exception) {
                supported = false
            }
        }
        return supported
    }

    /**
     * Picker/roller detent tick — constant, short, sharp.
     * Uses RT_TICK prebaked effect at moderate amplitude.
     * Each tick identical; fast spin = more ticks/sec (natural frequency).
     */
    override fun playTick() {
        if (!isAvailable()) return
        try {
            RichTapUtils.getInstance().playExtPrebaked(PrebakedEffectId.RT_KEYBOARD_TAP, 100)
        } catch (_: Exception) {}
    }

    override fun playEffect(effect: HapticEffect) {
        if (!isAvailable()) return
        try {
            val utils = RichTapUtils.getInstance()
            when (effect) {
                HapticEffect.TICK -> utils.playExtPrebaked(PrebakedEffectId.RT_TICK, 80)
                HapticEffect.CLICK -> utils.playExtPrebaked(PrebakedEffectId.RT_CLICK, 100)
                HapticEffect.THUD -> utils.playExtPrebaked(PrebakedEffectId.RT_THUD, 100)
                HapticEffect.RISE -> utils.playExtPrebaked(PrebakedEffectId.RT_RAMP_UP, 90)
                HapticEffect.CELEBRATION -> utils.playExtPrebaked(PrebakedEffectId.RT_SUCCESS, 100)
            }
        } catch (_: Exception) {}
    }

    override fun release() {
        if (initialized && supported) {
            try {
                RichTapUtils.getInstance().quit()
            } catch (_: Exception) {}
        }
        initialized = false
        supported = false
    }
}
