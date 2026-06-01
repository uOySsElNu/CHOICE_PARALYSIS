package com.choiceparalysis.turntable.audio

/**
 * MiHaptic engine for Xiaomi devices.
 * Uses reflection to access miui.os.DynamicEffect and miui.os.HapticPlayer
 * without compile-time dependency on the Xiaomi SDK.
 *
 * API signatures (from device):
 *   DynamicEffect.startCompose() -> DynamicEffect
 *   DynamicEffect.createTransient(float intensity, float sharpness) -> PrimitiveEffect
 *   DynamicEffect.createContinuous(float intensity, float sharpness, float duration) -> PrimitiveEffect
 *   effect.addPrimitive(float timeSec, PrimitiveEffect pe)
 *   HapticPlayer() + player.start(DynamicEffect)
 */
class MiHapticEngineImpl : HapticEngine {

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

            if (!ok) initFailed = true
        } catch (_: Exception) {
            initFailed = true
        }
    }

    override fun playTick() {
        if (!isAvailable()) return
        try {
            val effect = startComposeMethod!!.invoke(null)!!
            val primitive = createTransientMethod!!.invoke(null, 1.0f, 0.9f)!!
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
            HapticPrimitive(PrimitiveType.TRANSIENT, 80, 70)
        )
        HapticEffect.CLICK -> listOf(
            HapticPrimitive(PrimitiveType.TRANSIENT, 100, 50),
            HapticPrimitive(PrimitiveType.CONTINUOUS, 40, 30, 80, 200)
        )
        HapticEffect.THUD -> listOf(
            HapticPrimitive(PrimitiveType.TRANSIENT, 100, 20),
            HapticPrimitive(PrimitiveType.TRANSIENT, 80, 25, 180),
            HapticPrimitive(PrimitiveType.TRANSIENT, 60, 30, 350),
            HapticPrimitive(PrimitiveType.TRANSIENT, 40, 35, 500),
            HapticPrimitive(PrimitiveType.TRANSIENT, 25, 40, 630)
        )
        HapticEffect.RISE -> listOf(
            HapticPrimitive(PrimitiveType.CONTINUOUS, 50, 60, 0, 600),
            HapticPrimitive(PrimitiveType.TRANSIENT, 100, 50, 350),
            HapticPrimitive(PrimitiveType.TRANSIENT, 60, 40, 500)
        )
        HapticEffect.CELEBRATION -> listOf(
            HapticPrimitive(PrimitiveType.CONTINUOUS, 40, 50, 0, 800),
            HapticPrimitive(PrimitiveType.TRANSIENT, 100, 60, 200),
            HapticPrimitive(PrimitiveType.TRANSIENT, 80, 50, 350),
            HapticPrimitive(PrimitiveType.TRANSIENT, 100, 60, 500),
            HapticPrimitive(PrimitiveType.TRANSIENT, 50, 40, 700)
        )
    }

    private fun playComposed(primitives: List<HapticPrimitive>) {
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
        val intensity: Int,    // 0-100
        val frequency: Int,    // 0-100
        val startTimeMs: Long = 0,
        val durationMs: Long = 0,
    )

    private enum class PrimitiveType {
        TRANSIENT,
        CONTINUOUS,
    }
}
