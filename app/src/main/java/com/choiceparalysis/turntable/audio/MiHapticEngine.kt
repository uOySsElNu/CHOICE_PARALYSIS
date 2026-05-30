package com.choiceparalysis.turntable.audio

import android.util.Log

/**
 * MiHaptic engine wrapper for Xiaomi devices.
 * Uses reflection to avoid compile-time dependency on Xiaomi-specific framework classes.
 *
 * Based on official Xiaomi MiHaptic documentation.
 * Two package variants exist:
 *   - miui.os.DynamicEffect / miui.os.HapticPlayer  (older MIUI / MiHaptic SDK)
 *   - android.os.DynamicEffect / android.os.HapticPlayer (newer HyperOS)
 *
 * API signatures:
 *   HapticPlayer.isAvailable() -> boolean           (static)
 *   DynamicEffect.startCompose() -> DynamicEffect    (static factory)
 *   DynamicEffect.createTransient(float, float) -> PrimitiveEffect  (static)
 *   DynamicEffect.createContinuous(float, float, float) -> PrimitiveEffect  (static)
 *   effect.addPrimitive(float, PrimitiveEffect) -> DynamicEffect
 *   HapticPlayer() or HapticPlayer(DynamicEffect)
 *   player.start(DynamicEffect) — immediate playback
 *   player.stop() — stop and release
 */
object MiHapticEngine {

    private const val TAG = "MiHaptic"

    // Package candidates: miui.os first (original MiHaptic SDK), then android.os (HyperOS)
    private val PACKAGE_CANDIDATES = listOf("miui.os", "android.os")

    // Cached method references
    private var startComposeMethod: java.lang.reflect.Method? = null
    private var createTransientMethod: java.lang.reflect.Method? = null
    private var createContinuousMethod: java.lang.reflect.Method? = null
    private var addPrimitiveMethod: java.lang.reflect.Method? = null
    private var hapticPlayerConstructor: java.lang.reflect.Constructor<*>? = null
    private var hapticPlayerStartEffectMethod: java.lang.reflect.Method? = null
    private var isAvailableMethod: java.lang.reflect.Method? = null
    private var initFailed = false
    private var initDone = false
    private var foundPkg: String? = null

    /**
     * Check if MiHaptic is available on this device.
     * First tries HapticPlayer.isAvailable(), then falls back to method discovery.
     */
    fun isAvailable(): Boolean {
        if (initFailed) return false
        if (!initDone) initMethods()
        if (initFailed) return false

        // Try HapticPlayer.isAvailable() first (official way)
        if (isAvailableMethod != null) {
            return try {
                val result = isAvailableMethod!!.invoke(null) as Boolean
                Log.d(TAG, "HapticPlayer.isAvailable() = $result (pkg=$foundPkg)")
                result
            } catch (e: Exception) {
                Log.w(TAG, "isAvailable() call failed, falling back to method check", e)
                startComposeMethod != null
            }
        }

        // No isAvailable method found — assume available if we found the compose methods
        return startComposeMethod != null
    }

    private fun initMethods() {
        initDone = true
        try {
            // Find the first package where ALL required methods exist
            for (pkg in PACKAGE_CANDIDATES) {
                if (tryInitFromPackage(pkg)) {
                    foundPkg = pkg
                    Log.d(TAG, "MiHaptic init successful from $pkg")
                    return
                }
            }

            Log.w(TAG, "MiHaptic: no compatible package found among $PACKAGE_CANDIDATES")
            initFailed = true
        } catch (e: Exception) {
            Log.w(TAG, "MiHaptic init exception", e)
            initFailed = true
        }
    }

    /**
     * Try to initialize from a specific package.
     * Returns true if ALL required methods were found.
     */
    private fun tryInitFromPackage(pkg: String): Boolean {
        return try {
            val deClass = Class.forName("$pkg.DynamicEffect")
            val hpClass = Class.forName("$pkg.HapticPlayer")

            // HapticPlayer.isAvailable() — static, no args
            val isAvail = hpClass.methods.find {
                it.name == "isAvailable" && it.parameterTypes.isEmpty()
            }

            // DynamicEffect.startCompose() — static factory, returns DynamicEffect
            val startCompose = deClass.methods.find {
                it.name == "startCompose" && it.parameterTypes.isEmpty()
            }

            // DynamicEffect.createTransient(float intensity, float sharpness)
            val createTransient = deClass.methods.find {
                it.name == "createTransient" &&
                    it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == Float::class.javaPrimitiveType &&
                    it.parameterTypes[1] == Float::class.javaPrimitiveType
            }

            // DynamicEffect.createContinuous(float intensity, float sharpness, float duration)
            val createContinuous = deClass.methods.find {
                it.name == "createContinuous" &&
                    it.parameterTypes.size == 3 &&
                    it.parameterTypes.all { p -> p == Float::class.javaPrimitiveType }
            }

            // DynamicEffect.addPrimitive(float startTime, PrimitiveEffect effect)
            val addPrim = deClass.methods.find {
                it.name == "addPrimitive" &&
                    it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == Float::class.javaPrimitiveType
            }

            // HapticPlayer() no-arg constructor
            val hpCtor = hpClass.constructors.find {
                it.parameterTypes.isEmpty()
            }

            // HapticPlayer.start(DynamicEffect effect) — immediate playback
            val startEffect = hpClass.methods.find {
                it.name == "start" &&
                    it.parameterTypes.size == 1 &&
                    (it.parameterTypes[0].name.contains("DynamicEffect") ||
                     it.parameterTypes[0].name.contains("VibrationEffect"))
            }

            val allFound = startCompose != null &&
                    createTransient != null &&
                    createContinuous != null &&
                    addPrim != null &&
                    hpCtor != null &&
                    startEffect != null

            if (allFound) {
                startComposeMethod = startCompose
                createTransientMethod = createTransient
                createContinuousMethod = createContinuous
                addPrimitiveMethod = addPrim
                hapticPlayerConstructor = hpCtor
                hapticPlayerStartEffectMethod = startEffect
                isAvailableMethod = isAvail
                Log.d(TAG, "All methods found in $pkg (isAvailable=${isAvail != null})")
            } else {
                Log.d(TAG, "Some methods missing in $pkg: " +
                    "startCompose=${startCompose != null}, " +
                    "createTransient=${createTransient != null}, " +
                    "createContinuous=${createContinuous != null}, " +
                    "addPrimitive=${addPrim != null}, " +
                    "playerCtor=${hpCtor != null}, " +
                    "startEffect=${startEffect != null}")
            }

            allFound
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "Classes not found in $pkg: ${e.message}")
            false
        }
    }

    /**
     * Play a composed haptic effect.
     * Uses HapticPlayer(effect).start() or HapticPlayer().start(effect).
     */
    fun playComposed(primitives: List<HapticPrimitive>) {
        if (!isAvailable()) {
            Log.d(TAG, "playComposed: not available")
            return
        }
        try {
            Log.d(TAG, "playComposed: creating effect with ${primitives.size} primitives (pkg=$foundPkg)")
            val effect = startComposeMethod!!.invoke(null)!!

            for (p in primitives) {
                Log.d(TAG, "  primitive: ${p.type} intensity=${p.intensity} freq=${p.frequency} time=${p.startTimeMs}ms dur=${p.durationMs}ms")
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

            // Try HapticPlayer(effect).start() first, fall back to HapticPlayer().start(effect)
            try {
                val player = hapticPlayerConstructor!!.newInstance()
                hapticPlayerStartEffectMethod!!.invoke(player, effect)
            } catch (e: Exception) {
                Log.w(TAG, "start(effect) failed, trying constructor(effect).start()", e)
                // Some versions use HapticPlayer(DynamicEffect) constructor
                val altCtor = hapticPlayerConstructor!!.declaringClass.constructors.find {
                    it.parameterTypes.size == 1
                }
                if (altCtor != null) {
                    val player = altCtor.newInstance(effect)
                    val startMethod = player.javaClass.getMethod("start")
                    startMethod.invoke(player)
                } else {
                    throw e
                }
            }

            Log.d(TAG, "playComposed: started successfully")
        } catch (e: Exception) {
            Log.w(TAG, "playComposed failed: ${e.message}", e)
            resetCache()
        }
    }

    private fun resetCache() {
        startComposeMethod = null
        createTransientMethod = null
        createContinuousMethod = null
        addPrimitiveMethod = null
        hapticPlayerConstructor = null
        hapticPlayerStartEffectMethod = null
        isAvailableMethod = null
        initFailed = false
        initDone = false
        foundPkg = null
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
