package com.choiceparalysis.turntable.audio

import android.util.Log

/**
 * MiHaptic engine wrapper for Xiaomi devices.
 * Uses reflection to avoid compile-time dependency on miui.os.
 *
 * Actual API signatures (from device):
 *   DynamicEffect.startCompose() -> DynamicEffect
 *   DynamicEffect.createTransient(float intensity, float sharpness) -> PrimitiveEffect
 *   DynamicEffect.createContinuous(float intensity, float sharpness, float duration) -> PrimitiveEffect
 *   effect.addPrimitive(float timeSec, PrimitiveEffect pe)
 *   HapticPlayer(DynamicEffect) + player.start()
 */
object MiHapticEngine {

    private const val TAG = "MiHaptic"

    // Cached references - lazily initialized, re-initialized on failure
    private var dynamicEffectClass: Class<*>? = null
    private var hapticPlayerClass: Class<*>? = null
    private var startComposeMethod: java.lang.reflect.Method? = null
    private var createTransientMethod: java.lang.reflect.Method? = null
    private var createContinuousMethod: java.lang.reflect.Method? = null
    private var addPrimitiveMethod: java.lang.reflect.Method? = null
    private var hapticPlayerConstructor: java.lang.reflect.Constructor<*>? = null
    private var hapticPlayerStartMethod: java.lang.reflect.Method? = null
    private var initFailed = false

    /**
     * Check if MiHaptic is available. Re-discovers methods if not yet initialized.
     */
    fun isAvailable(): Boolean {
        if (initFailed) return false
        if (startComposeMethod != null) return true
        return initMethods()
    }

    private fun initMethods(): Boolean {
        return try {
            dynamicEffectClass = Class.forName("miui.os.DynamicEffect")
            hapticPlayerClass = Class.forName("miui.os.HapticPlayer")

            startComposeMethod = dynamicEffectClass!!.getMethod("startCompose")
            createTransientMethod = dynamicEffectClass!!.methods.find {
                it.name == "createTransient" && it.parameterTypes.size == 2
            }
            createContinuousMethod = dynamicEffectClass!!.methods.find {
                it.name == "createContinuous" && it.parameterTypes.size == 3
            }
            addPrimitiveMethod = dynamicEffectClass!!.methods.find {
                it.name == "addPrimitive" && it.parameterTypes.size == 2
            }
            hapticPlayerConstructor = hapticPlayerClass!!.constructors.find {
                it.parameterTypes.size == 1
            }
            hapticPlayerStartMethod = hapticPlayerClass!!.getMethod("start")

            val ok = startComposeMethod != null &&
                    createTransientMethod != null &&
                    createContinuousMethod != null &&
                    addPrimitiveMethod != null &&
                    hapticPlayerConstructor != null

            if (!ok) {
                Log.w(TAG, "MiHaptic: some methods not found")
                initFailed = true
            }
            ok
        } catch (e: Exception) {
            Log.w(TAG, "MiHaptic init failed", e)
            initFailed = true
            false
        }
    }

    /**
     * Play a composed haptic effect. Re-initializes if cached methods are stale.
     */
    fun playComposed(primitives: List<HapticPrimitive>) {
        if (!isAvailable()) return
        try {
            val effect = startComposeMethod!!.invoke(null)!!

            for (p in primitives) {
                val primitive = when (p.type) {
                    PrimitiveType.TRANSIENT -> createTransientMethod!!.invoke(
                        null, p.intensity / 100f, p.frequency / 100f
                    )!!
                    PrimitiveType.CONTINUOUS -> createContinuousMethod!!.invoke(
                        null, p.intensity / 100f, p.frequency / 100f, p.durationMs / 1000f
                    )!!
                }
                addPrimitiveMethod!!.invoke(effect, p.startTimeMs / 1000f, primitive)
            }

            val player = hapticPlayerConstructor!!.newInstance(effect)
            hapticPlayerStartMethod!!.invoke(player)
        } catch (e: Exception) {
            Log.w(TAG, "playComposed failed, re-initializing", e)
            // Reset cached methods so next call re-discovers them
            resetCache()
        }
    }

    private fun resetCache() {
        dynamicEffectClass = null
        hapticPlayerClass = null
        startComposeMethod = null
        createTransientMethod = null
        createContinuousMethod = null
        addPrimitiveMethod = null
        hapticPlayerConstructor = null
        hapticPlayerStartMethod = null
        initFailed = false
    }

    data class HapticPrimitive(
        val type: PrimitiveType,
        val intensity: Int,    // 0-100
        val frequency: Int,    // 0-100
        val startTimeMs: Long = 0,
        val durationMs: Long = 0,
    )

    enum class PrimitiveType {
        TRANSIENT,
        CONTINUOUS,
    }
}
