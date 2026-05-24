package cc.arccore.bootstrap.runtime

import cc.arccore.api.module.ModuleDescription
import cc.arccore.bootstrap.runtime.profiling.BootstrapProfilingData
import java.util.concurrent.ConcurrentHashMap

/**
 * Immutable-by-convention context for a single module's bootstrap lifecycle.
 *
 * Holds:
 * - module identity ([moduleId], [description], [classLoader])
 * - current phase tracking ([currentPhase])
 * - type-safe slot map for inter-phase data passing ([BootstrapContextKey] → value)
 * - optional reference to the runtime context object (erased to [Any])
 * - profiling data accumulated across phases
 * - hot-reload flag for optimization hint computation
 *
 * Designed so that no single phase mutates another phase's concern —
 * phases only add new slots or update [currentPhase].
 */
class BootstrapContext private constructor(
    val moduleId: String,
    val classLoader: ClassLoader,
    val description: ModuleDescription,
    val isHotReload: Boolean,
    /** The RuntimeModuleContext instance, type-erased to avoid arc-runtime compile-time dependency. */
    val runtimeContext: Any?,
    private val slots: ConcurrentHashMap<BootstrapContextKey<*>, Any>,
    @Volatile var currentPhase: BootstrapPhase
) {

    /**
     * Stores a typed value under [key] in the slot map.
     * Overwrites any existing value for the same key.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> put(key: BootstrapContextKey<T>, value: T) {
        slots[key] = value
    }

    /**
     * Retrieves the value stored under [key], or null if absent.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: BootstrapContextKey<T>): T? = slots[key] as? T

    /**
     * Retrieves the value stored under [key], throwing if absent.
     */
    fun <T : Any> require(key: BootstrapContextKey<T>): T =
        get(key) ?: throw IllegalStateException(
            "Required context slot '${key.name}' is missing in bootstrap context for module '$moduleId'"
        )

    fun containsKey(key: BootstrapContextKey<*>): Boolean = slots.containsKey(key)

    /**
     * Returns a copy of this context with [currentPhase] updated to [nextPhase].
     * The slot map is shared (slots are additive, never removed mid-pipeline).
     */
    fun withPhase(nextPhase: BootstrapPhase): BootstrapContext = BootstrapContext(
        moduleId = moduleId,
        classLoader = classLoader,
        description = description,
        isHotReload = isHotReload,
        runtimeContext = runtimeContext,
        slots = slots,
        currentPhase = nextPhase
    )

    /**
     * Returns a copy of this context with [runtimeContext] set.
     * Used by lifecycle-aware handlers that resolve the runtime context lazily.
     */
    fun withRuntimeContext(ctx: Any): BootstrapContext = BootstrapContext(
        moduleId = moduleId,
        classLoader = classLoader,
        description = description,
        isHotReload = isHotReload,
        runtimeContext = ctx,
        slots = slots,
        currentPhase = currentPhase
    )

    fun slotSnapshot(): Map<String, Any> = slots.entries.associate { (k, v) -> k.name to v }

    override fun toString(): String =
        "BootstrapContext(moduleId=$moduleId, phase=$currentPhase, hotReload=$isHotReload, slots=${slots.keys.map { it.name }})"

    companion object {

        fun create(
            moduleId: String,
            classLoader: ClassLoader,
            description: ModuleDescription,
            isHotReload: Boolean = false,
            runtimeContext: Any? = null
        ): BootstrapContext = BootstrapContext(
            moduleId = moduleId,
            classLoader = classLoader,
            description = description,
            isHotReload = isHotReload,
            runtimeContext = runtimeContext,
            slots = ConcurrentHashMap(),
            currentPhase = BootstrapPhase.DISCOVERY
        )
    }
}
