package com.choiceparalysis.turntable.audio

/**
 * RichTap engine for OEMs using AAC Technologies RichTap SDK.
 *
 * Both Xiaomi (MiHaptic / DynamicEffect) and vivo (VivoHaptic / DynamicEffect)
 * expose the same RichTap API surface under different package names:
 *
 *   Xiaomi: miui.os.DynamicEffect + miui.os.HapticPlayer
 *   vivo:   com.vivo.os.dynamicEffect.DynamicEffect + ...HapticPlayer
 *           (also android.os.DynamicEffect on some ROM versions)
 *
 * This engine auto-discovers whichever package is present at runtime.
 *
 * RichTap API signatures (same across OEMs):
 *   DynamicEffect.startCompose() -> DynamicEffect *   .createTransient(float intensity, float sharpness) -> PrimitiveEffect
 *     - intensity: 0.0~1.0, vibration strength
 *     - sharpness: 0.0~1.0, higher = crisper/shorter, lower = softer/longer
 *   DynamicEffect.createContinuous(float intensity, float sharpness, float duration) -> PrimitiveEffect
 *     - duration in seconds
 *   effect.addPrimitive(float timeSec, PrimitiveEffect pe)
 *   HapticPlayer() + player.start(DynamicEffect)
 *
 * Key tuning principles for linear motor:
 * - Transient (short pulse): high sharpness (0.8-1.0) for crisp click feel
 * - Continuous (sustained): lower sharpness (0.3-0.6) for rumble/buzz feel
 * - Intensity controls amplitude, sharpness controls frequency envelope
 * - X-axis linear motor optimal range: 50-500Hz, resonance ~130Hz
 */
class RichTapEngineImpl : HapticEngine {

    private var startComposeMethod: java.lang.reflect.Method? = null
    private var createTransientMethod: java.lang.reflect.Method? = null
    private var createContinuousMethod: java.lang.reflect.Method? = null
    private var addPrimitiveMethod: java.lang.reflect.Method? = null
    private var hapticPlayerConstructor: java.lang.reflect.Constructor<*>? = null
    private var hapticPlayerStartEffectMethod: java.lang.reflect.Method? = null
    private var initFailed = false
    private var initDone = false

    private var cachedPlayer: Any? = null

    override fun isAvailable(): Boolean {
        if (initFailed) return false
        if (!initDone) initMethods()
        return !initFailed
    }

    @Synchronized
    private fun initMethods() {
        if (initDone) return
        initDone = true
        try {
            // Try known RichTap package paths across OEMs
            val packageCandidates = listOf(
                // Xiaomi (MiHaptic)
                "miui.os" to "DynamicEffect" to "HapticPlayer",
                "android.os" to "DynamicEffect" to "HapticPlayer",
                // vivo (OriginOS)
                "com.vivo.os.dynamicEffect" to "DynamicEffect" to "HapticPlayer",
                "com.vivo.os" to "DynamicEffect" to "HapticPlayer",
            )
            for ((pair, hpName) in packageCandidates) {
                val (pkg, deName) = pair
                if (tryInitFromPackage(pkg, deName, hpName)) return
            }
            initFailed = true
        } catch (_: Exception) {
            initFailed = true
        }
    }

    private fun tryInitFromPackage(pkg: String, deName: String, hpName: String): Boolean {
        return try {
            val deClass = Class.forName("$pkg.$deName")
            val hpClass = Class.forName("$pkg.$hpName")

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

            if (!ok) return false
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /**
     * Picker/roller detent tick — constant, short, sharp.
     *
     * Tuning:
     * - Max sharpness (1.0) for ultra-crisp "擦过" transient attack
     * - Moderate intensity (0.5) — brief enough to not fatigue, strong enough to feel
     * - Each tick is identical; fast spin = more ticks/sec (natural frequency)
     */
    override fun playTick() {
        if (!isAvailable()) return
        try {
            val effect = startComposeMethod!!.invoke(null)!!
            val primitive = createTransientMethod!!.invoke(null, 0.5f, 1.0f)!!
            addPrimitiveMethod!!.invoke(effect, 0f, primitive)
            val player = getOrCreatePlayer()
            hapticPlayerStartEffectMethod!!.invoke(player, effect)
        } catch (_: Exception) {
            cachedPlayer = null
        }
    }

    override fun playEffect(effect: HapticEffect) {
        if (!isAvailable()) return
        val primitives = mapEffect(effect)
        playComposed(primitives)
    }

    private fun mapEffect(effect: HapticEffect): List<HapticPrimitive> = when (effect) {
        HapticEffect.TICK -> listOf(
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 60, sharpness = 95)
        )
        HapticEffect.CLICK -> listOf(
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 85, sharpness = 75)
        )
        HapticEffect.THUD -> listOf(
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 100, sharpness = 40),
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 65, sharpness = 45, startTimeMs = 50),
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 35, sharpness = 50, startTimeMs = 100)
        )
        HapticEffect.RISE -> listOf(
            HapticPrimitive(PrimitiveType.CONTINUOUS, intensity = 30, sharpness = 45, startTimeMs = 0, durationMs = 600),
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 50, sharpness = 60, startTimeMs = 200),
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 70, sharpness = 70, startTimeMs = 400),
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 100, sharpness = 85, startTimeMs = 600)
        )
        HapticEffect.CELEBRATION -> listOf(
            HapticPrimitive(PrimitiveType.CONTINUOUS, intensity = 25, sharpness = 35, startTimeMs = 0, durationMs = 900),
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 100, sharpness = 90, startTimeMs = 50),
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 85, sharpness = 75, startTimeMs = 200),
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 95, sharpness = 80, startTimeMs = 350),
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 75, sharpness = 65, startTimeMs = 500),
            HapticPrimitive(PrimitiveType.TRANSIENT, intensity = 100, sharpness = 85, startTimeMs = 700)
        )
    }

    private fun playComposed(primitives: List<HapticPrimitive>) {
        try {
            val effect = startComposeMethod!!.invoke(null)!!
            for (p in primitives) {
                val primitive = when (p.type) {
                    PrimitiveType.TRANSIENT -> createTransientMethod!!.invoke(
                        null, p.intensity / 100f, p.sharpness / 100f
                    )!!
                    PrimitiveType.CONTINUOUS -> createContinuousMethod!!.invoke(
                        null, p.intensity / 100f, p.sharpness / 100f, p.durationMs / 1000f
                    )!!
                }
                addPrimitiveMethod!!.invoke(effect, p.startTimeMs / 1000f, primitive)
            }
            val player = getOrCreatePlayer()
            hapticPlayerStartEffectMethod!!.invoke(player, effect)
        } catch (_: Exception) {
            cachedPlayer = null
        }
    }

    @Synchronized
    private fun getOrCreatePlayer(): Any {
        return cachedPlayer ?: hapticPlayerConstructor!!.newInstance().also { cachedPlayer = it }
    }

    override fun release() {
        cachedPlayer = null
    }

    private data class HapticPrimitive(
        val type: PrimitiveType,
        val intensity: Int,     // 0-100, maps to 0.0-1.0
        val sharpness: Int,     // 0-100, maps to 0.0-1.0 (higher = crisper)
        val startTimeMs: Long = 0,
        val durationMs: Long = 0,  // only for CONTINUOUS
    )

    private enum class PrimitiveType {
        TRANSIENT,
        CONTINUOUS,
    }
}
