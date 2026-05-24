package com.choiceparalysis.turntable.audio

import android.content.Context
import android.util.Log

/**
 * MiHaptic engine wrapper for Xiaomi devices.
 * Uses reflection to avoid compile-time dependency on miui.os.
 * Falls back gracefully on non-Xiaomi devices.
 */
object MiHapticEngine {

    private const val TAG = "MiHaptic"
    private var available = false
    private var dynamicEffectClass: Class<*>? = null
    private var hapticPlayerClass: Class<*>? = null
    private var checked = false

    fun isAvailable(context: Context): Boolean {
        if (checked) return available
        checked = true
        return try {
            dynamicEffectClass = Class.forName("miui.os.DynamicEffect")
            hapticPlayerClass = Class.forName("miui.os.HapticPlayer")
            available = true
            Log.d(TAG, "MiHaptic available")
            true
        } catch (_: Exception) {
            available = false
            Log.d(TAG, "MiHaptic not available")
            false
        }
    }

    /**
     * Play a transient (sharp click) effect.
     * @param intensity 0-100
     * @param frequency 0-100 (sharpness)
     */
    fun playTransient(intensity: Int, frequency: Int) {
        if (!available) return
        try {
            val effect = createEffect {
                val transient = createTransient(intensity, frequency)
                invokeAddPrimitive(it, 0.0, transient)
            }
            playEffect(effect)
        } catch (e: Exception) {
            Log.w(TAG, "playTransient failed", e)
        }
    }

    /**
     * Play a continuous (sustained) effect.
     * @param intensity 0-100
     * @param frequency 0-100
     * @param durationMs duration in milliseconds
     */
    fun playContinuous(intensity: Int, frequency: Int, durationMs: Int) {
        if (!available) return
        try {
            val effect = createEffect {
                val continuous = createContinuous(intensity, frequency, durationMs / 1000.0)
                invokeAddPrimitive(it, 0.0, continuous)
            }
            playEffect(effect)
        } catch (e: Exception) {
            Log.w(TAG, "playContinuous failed", e)
        }
    }

    /**
     * Play a composed effect with multiple primitives.
     */
    fun playComposed(primitives: List<HapticPrimitive>) {
        if (!available) return
        try {
            val effect = createEffect { effectObj ->
                for (p in primitives) {
                    val primitive = when (p.type) {
                        PrimitiveType.TRANSIENT -> createTransient(p.intensity, p.frequency)
                        PrimitiveType.CONTINUOUS -> createContinuous(p.intensity, p.frequency, p.durationMs / 1000.0)
                    }
                    invokeAddPrimitive(effectObj, p.startTimeMs / 1000.0, primitive)
                }
            }
            playEffect(effect)
        } catch (e: Exception) {
            Log.w(TAG, "playComposed failed", e)
        }
    }

    private fun createEffect(build: (Any) -> Unit): Any {
        val startCompose = dynamicEffectClass!!.getMethod("startCompose")
        val effect = startCompose.invoke(null)!!
        build(effect)
        return effect
    }

    private fun createTransient(intensity: Int, frequency: Int): Any {
        val method = dynamicEffectClass!!.getMethod(
            "createTransient", Float::class.java, Float::class.java
        )
        return method.invoke(null, intensity / 100f, frequency / 100f)!!
    }

    private fun createContinuous(intensity: Int, frequency: Int, durationSec: Double): Any {
        val method = dynamicEffectClass!!.getMethod(
            "createContinuous", Double::class.java, Double::class.java, Double::class.java
        )
        return method.invoke(null, intensity / 100.0, frequency / 100.0, durationSec)!!
    }

    private fun invokeAddPrimitive(effect: Any, timeSec: Double, primitive: Any) {
        val method = dynamicEffectClass!!.getMethod(
            "addPrimitive", Double::class.java, primitive.javaClass.interfaces.firstOrNull() ?: Any::class.java
        )
        method.invoke(effect, timeSec, primitive)
    }

    private fun playEffect(effect: Any) {
        val constructor = hapticPlayerClass!!.getConstructor(effect.javaClass)
        val player = constructor.newInstance(effect)
        val startMethod = hapticPlayerClass!!.getMethod("start")
        startMethod.invoke(player)
    }

    data class HapticPrimitive(
        val type: PrimitiveType,
        val intensity: Int,    // 0-100
        val frequency: Int,    // 0-100
        val startTimeMs: Long = 0,
        val durationMs: Long = 0,  // only for CONTINUOUS
    )

    enum class PrimitiveType {
        TRANSIENT,   // sharp click
        CONTINUOUS,  // sustained vibration
    }
}
