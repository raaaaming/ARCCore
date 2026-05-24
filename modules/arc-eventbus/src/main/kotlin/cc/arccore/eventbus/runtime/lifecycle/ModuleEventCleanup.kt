package cc.arccore.eventbus.runtime.lifecycle

import cc.arccore.eventbus.runtime.InternalEventBus

/**
 * Utility that performs cleanup of all subscriptions for a specific module
 * when that module is unloaded.
 *
 * Intended to be registered with a module's
 * [CleanupScope][cc.arccore.api.module.CleanupScope] or called
 * by the framework's unload pipeline:
 *
 * ```kotlin
 * context.cleanupScope.onClose {
 *     ModuleEventCleanup(eventBus, moduleId).cleanup()
 * }
 * ```
 *
 * @param eventBus The event bus to clean up subscriptions from.
 * @param moduleId The module ID whose subscriptions should be cancelled.
 */
class ModuleEventCleanup(
    private val eventBus: InternalEventBus,
    private val moduleId: String
) : AutoCloseable {

    /**
     * Cancels all subscriptions owned by [moduleId] on the [eventBus].
     *
     * @return The number of subscriptions cancelled.
     */
    fun cleanup(): Int = eventBus.unsubscribeAll(moduleId)

    /**
     * Implements [AutoCloseable] for use with [CleanupScope].
     */
    override fun close() {
        cleanup()
    }
}
