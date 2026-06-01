package com.choiceparalysis.turntable.audio

import android.util.Log

/**
 * MiHaptic engine wrapper for Xiaomi devices.
 * Uses reflection to avoid compile-time dependency on miui.os.
 *
 * API signatures (from device):
 *   DynamicEffect.startCompose() -> DynamicEffect
 *   DynamicEffect.createTransient(float intensity, float sharpness) -> PrimitiveEffect
 *   DynamicEffect.createContinuous(float intensity, float sharpness, float duration) -> PrimitiveEffect
 *   effect.addPrimitive(float timeSec, PrimitiveEffect pe)
 *   HapticPlayer() + player.start(DynamicEffect)
 */
object MiHapticEngine {

    private const val TAG = "MiHaptic"

    private var startComposeMethod: java.lang.reflect.Method? = null
    private var createTransientMethod: java.lang.reflect.Method? = null
    private var createContinuousMethod: java.lang.reflect.Method? = null
    private var addPrimitiveMethod: java.lang.reflect.Method? = null
    private var hapticPlayerConstructor: java.lang.reflect.Constructor<*>? = null
    private var hapticPlayerStartEffectMethod: java.lang.reflect.Method? = null
    private var initFailed = false
    private var initDone = false

    // Cached player — reuse across all calls to avoid constructor overhead
    private var cachedPlayer: Any? = null

    fun isAvailable(): Boolean {
        if (initFailed) return false
        if (!initDone) initMethods()
        return !initFailed
    }

    @Synchronized
    private fun initMethods() {
        if (initDone) return
        initDone = true
        try {
            val deClass = Class.forName("miui.os.DynamicEffect")
            val hpClass = Class.forName("miui.os.HapticPlayer")

            startComposeMethod = deClass.getMethod("startCompose")
            createTransientMethod = deClass.methods.find {
                it.name == "createTransient" &&
                    it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == Float::class.javaPrimitiveType &&
                    it.parameterTypes[1] == Float::class.javaPrimitiveType
            }
            createContinuousMethod = deClass.methods.find {
                it.name == "createContinuous" &&
                    it.parameterTypes.size == 3 &&
                    it.parameterTypes.all { p -> p == Float::class.javaPrimitiveType }
            }
            addPrimitiveMethod = deClass.methods.find {
                it.name == "addPrimitive" &&
                    it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == Float::class.javaPrimitiveType
            }
            hapticPlayerConstructor = hpClass.constructors.find { it.parameterTypes.isEmpty() }
            hapticPlayerStartEffectMethod = hpClass.methods.find {
                it.name == "start" && it.parameterTypes.size == 1
            }

            val ok = startComposeMethod != null &&
                    createTransientMethod != null &&
                    createContinuousMethod != null &&
                    addPrimitiveMethod != null &&
                    hapticPlayerConstructor != null &&
                    hapticPlayerStartEffectMethod != null

            Log.d(TAG, "init: ok=$ok startCompose=${startComposeMethod != null} " +
                "createTransient=${createTransientMethod != null} " +
                "createContinuous=${createContinuousMethod != null} " +
                "addPrimitive=${addPrimitiveMethod != null} " +
                "playerCtor=${hapticPlayerConstructor != null} " +
                "start=${hapticPlayerStartEffectMethod != null}")

            if (!ok) initFailed = true
        } catch (e: Exception) {
            Log.w(TAG, "init failed: ${e.message}")
            initFailed = true
        }
    }

    /**
     * Play a composed haptic effect. Creates fresh player each time
     * since start(effect) properly terminates the previous one.
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
            val player = getOrCreatePlayer()
            hapticPlayerStartEffectMethod!!.invoke(player, effect)
        } catch (e: Exception) {
            Log.w(TAG, "playComposed failed: ${e.message}")
            cachedPlayer = null
        }
    }

    /**
     * Play a single crisp tick for spin wheel boundary crossing.
     * Reuses cached player, creates fresh effect each call.
     */
    fun playTick() {
        if (!isAvailable()) return
        try {
            val effect = startComposeMethod!!.invoke(null)!!
            // intensity=100, sharpness=90 — strong crisp click
            val primitive = createTransientMethod!!.invoke(null, 1.0f, 0.9f)!!
            addPrimitiveMethod!!.invoke(effect, 0f, primitive)
            val player = getOrCreatePlayer()
            hapticPlayerStartEffectMethod!!.invoke(player, effect)
        } catch (e: Exception) {
            Log.w(TAG, "playTick failed: ${e.message}")
            cachedPlayer = null
        }
    }

    @Synchronized
    private fun getOrCreatePlayer(): Any {
        return cachedPlayer ?: hapticPlayerConstructor!!.newInstance().also { cachedPlayer = it }
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
