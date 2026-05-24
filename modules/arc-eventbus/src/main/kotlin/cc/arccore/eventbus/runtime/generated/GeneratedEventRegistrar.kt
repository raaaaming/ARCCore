package cc.arccore.eventbus.runtime.generated

import cc.arccore.eventbus.runtime.InternalEventBus

/**
 * Interface implemented by KSP-generated event registrar classes.
 *
 * The generated class (typically `cc.arccore.generated.ArcEventRegistrar`)
 * is loaded at runtime via [EventRegistrarLoader] to perform reflection-less
 * event subscription registration.
 *
 * ```kotlin
 * // KSP generates:
 * class ArcEventRegistrar : GeneratedEventRegistrar {
 *     override fun register(bus: InternalEventBus, moduleId: String) {
 *         bus.subscribe<MyEvent>(moduleId, PropagationPhase.NORMAL, EventPriority.NORMAL) {
 *             MyHandler().onMyEvent(it)
 *         }
 *     }
 * }
 * ```
 */
interface GeneratedEventRegistrar {

    /**
     * Registers all KSP-discovered event listeners on [bus] for the given [moduleId].
     *
     * @param bus The [InternalEventBus] to register subscriptions on.
     * @param moduleId The owning module's ID.
     */
    fun register(bus: InternalEventBus, moduleId: String)
}
