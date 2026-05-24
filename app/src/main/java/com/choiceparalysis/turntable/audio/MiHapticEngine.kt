package com.choiceparalysis.turntable.audio

import android.content.Context
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
    private var available = false
    private var checked = false

    // Cached references
    private var dynamicEffectClass: Class<*>? = null
    private var hapticPlayerClass: Class<*>? = null
    private var primitiveEffectClass: Class<*>? = null
    private var startComposeMethod: java.lang.reflect.Method? = null
    private var createTransientMethod: java.lang.reflect.Method? = null
    private var createContinuousMethod: java.lang.reflect.Method? = null
    private var addPrimitiveMethod: java.lang.reflect.Method? = null
    private var hapticPlayerConstructor: java.lang.reflect.Constructor<*>? = null
    private var hapticPlayerStartMethod: java.lang.reflect.Method? = null

    fun isAvailable(context: Context): Boolean {
        if (checked) return available
        checked = true
        return try {
            dynamicEffectClass = Class.forName("miui.os.DynamicEffect")
            hapticPlayerClass = Class.forName("miui.os.HapticPlayer")

            primitiveEffectClass = dynamicEffectClass!!.classes.find {
                it.simpleName == "PrimitiveEffect"
            }

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

            available = startComposeMethod != null &&
                    createTransientMethod != null &&
                    createContinuousMethod != null &&
                    addPrimitiveMethod != null &&
                    hapticPlayerConstructor != null

            Log.d(TAG, "MiHaptic available: $available")
            available
        } catch (e: Exception) {
            Log.w(TAG, "MiHaptic init failed", e)
            available = false
            false
        }
    }

    fun playComposed(primitives: List<HapticPrimitive>) {
        if (!available) return
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
                // addPrimitive(float timeSec, PrimitiveEffect pe)
                addPrimitiveMethod!!.invoke(effect, p.startTimeMs / 1000f, primitive)
            }

            val player = hapticPlayerConstructor!!.newInstance(effect)
            hapticPlayerStartMethod!!.invoke(player)
        } catch (e: Exception) {
            Log.w(TAG, "playComposed failed", e)
        }
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
