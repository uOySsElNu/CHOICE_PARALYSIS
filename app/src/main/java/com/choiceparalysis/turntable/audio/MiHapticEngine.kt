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

    // Cached method references
    private var startComposeMethod: java.lang.reflect.Method? = null
    private var addPrimitiveMethod: java.lang.reflect.Method? = null
    private var createTransientMethod: java.lang.reflect.Method? = null
    private var createContinuousMethod: java.lang.reflect.Method? = null
    private var hapticPlayerConstructor: java.lang.reflect.Constructor<*>? = null
    private var hapticPlayerStartMethod: java.lang.reflect.Method? = null

    // PrimitiveEffect interface (inner class of DynamicEffect)
    private var primitiveEffectClass: Class<*>? = null

    fun isAvailable(context: Context): Boolean {
        if (checked) return available
        checked = true
        return try {
            dynamicEffectClass = Class.forName("miui.os.DynamicEffect")
            hapticPlayerClass = Class.forName("miui.os.HapticPlayer")

            // Find PrimitiveEffect inner interface
            primitiveEffectClass = dynamicEffectClass!!.classes.find {
                it.simpleName == "PrimitiveEffect"
            }

            // Log all available methods for debugging
            Log.d(TAG, "DynamicEffect methods:")
            dynamicEffectClass!!.methods.forEach { m ->
                Log.d(TAG, "  ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})")
            }
            Log.d(TAG, "DynamicEffect inner classes:")
            dynamicEffectClass!!.classes.forEach { c ->
                Log.d(TAG, "  ${c.simpleName}")
            }
            Log.d(TAG, "HapticPlayer constructors:")
            hapticPlayerClass!!.constructors.forEach { c ->
                Log.d(TAG, "  (${c.parameterTypes.joinToString { it.simpleName }})")
            }

            // Cache methods
            startComposeMethod = dynamicEffectClass!!.getMethod("startCompose")

            // Find createTransient - could be (float, float) or (Float, Float)
            createTransientMethod = dynamicEffectClass!!.methods.find {
                it.name == "createTransient" && it.parameterTypes.size == 2
            }

            // Find createContinuous
            createContinuousMethod = dynamicEffectClass!!.methods.find {
                it.name == "createContinuous" && it.parameterTypes.size == 3
            }

            // Find addPrimitive - match by name and parameter count
            addPrimitiveMethod = dynamicEffectClass!!.methods.find {
                it.name == "addPrimitive" && it.parameterTypes.size == 2
            }

            // HapticPlayer constructor
            hapticPlayerConstructor = hapticPlayerClass!!.constructors.firstOrNull()
            hapticPlayerStartMethod = hapticPlayerClass!!.getMethod("start")

            available = startComposeMethod != null &&
                    createTransientMethod != null &&
                    createContinuousMethod != null &&
                    addPrimitiveMethod != null &&
                    hapticPlayerConstructor != null

            Log.d(TAG, "MiHaptic available: $available")
            if (available) {
                Log.d(TAG, "  addPrimitive params: ${addPrimitiveMethod!!.parameterTypes.map { it.simpleName }}")
                Log.d(TAG, "  PrimitiveEffect class: ${primitiveEffectClass?.name}")
            }

            available
        } catch (e: Exception) {
            Log.w(TAG, "MiHaptic init failed", e)
            available = false
            false
        }
    }

    /**
     * Play a composed effect with multiple primitives.
     */
    fun playComposed(primitives: List<HapticPrimitive>) {
        if (!available) return
        try {
            // Create DynamicEffect via startCompose()
            val effect = startComposeMethod!!.invoke(null)!!

            for (p in primitives) {
                val primitive = when (p.type) {
                    PrimitiveType.TRANSIENT -> createTransientMethod!!.invoke(
                        null, p.intensity / 100f, p.frequency / 100f
                    )!!
                    PrimitiveType.CONTINUOUS -> createContinuousMethod!!.invoke(
                        null, p.intensity / 100.0, p.frequency / 100.0, p.durationMs / 1000.0
                    )!!
                }
                addPrimitiveMethod!!.invoke(effect, p.startTimeMs / 1000.0, primitive)
            }

            // Play via HapticPlayer
            val player = hapticPlayerConstructor!!.newInstance(effect)
            hapticPlayerStartMethod!!.invoke(player)

            Log.d(TAG, "playComposed: ${primitives.size} primitives played")
        } catch (e: Exception) {
            Log.w(TAG, "playComposed failed", e)
        }
    }

    data class HapticPrimitive(
        val type: PrimitiveType,
        val intensity: Int,    // 0-100
        val frequency: Int,    // 0-100
        val startTimeMs: Long = 0,
        val durationMs: Long = 0,  // only for CONTINUOUS
    )

    enum class PrimitiveType {
        TRANSIENT,
        CONTINUOUS,
    }
}
