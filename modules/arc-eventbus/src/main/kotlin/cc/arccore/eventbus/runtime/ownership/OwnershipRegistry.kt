package cc.arccore.eventbus.runtime.ownership

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry mapping module IDs to their owned subscription IDs.
 *
 * Used by [DefaultInternalEventBus] to cancel all subscriptions for a module
 * when it is unloaded (stale listener detection).
 */
class OwnershipRegistry {

    private val ownershipMap = ConcurrentHashMap<String, SubscriptionOwnership>()

    /**
     * Registers a new subscription under the given module.
     */
    fun register(moduleId: String, subscriptionId: UUID) {
        ownershipMap
            .computeIfAbsent(moduleId) { SubscriptionOwnership(moduleId) }
            .add(subscriptionId)
    }

    /**
     * Removes a specific subscription from its owning module's record.
     */
    fun unregister(moduleId: String, subscriptionId: UUID) {
        ownershipMap[moduleId]?.remove(subscriptionId)
    }

    /**
     * Returns all subscription IDs owned by [moduleId], or an empty set if none.
     */
    fun getSubscriptionIds(moduleId: String): Set<UUID> =
        ownershipMap[moduleId]?.snapshot() ?: emptySet()

    /**
     * Cancels all ownership records for [moduleId] and returns the set of subscription IDs
     * that were owned by that module.
     *
     * The caller is responsible for actually deactivating those subscriptions
     * in [SubscriptionRegistry].
     *
     * @return Set of subscription UUIDs that were registered under [moduleId].
     */
    fun cancelAll(moduleId: String): Set<UUID> {
        val ownership = ownershipMap.remove(moduleId) ?: return emptySet()
        return ownership.snapshot()
    }

    /**
     * Returns the number of subscriptions owned by [moduleId].
     */
    fun subscriptionCount(moduleId: String): Int =
        ownershipMap[moduleId]?.size() ?: 0

    /**
     * Returns a snapshot of all module IDs with registered subscriptions.
     */
    fun registeredModules(): Set<String> = ownershipMap.keys.toSet()

    /**
     * Returns true if [moduleId] has any active ownership records.
     */
    fun hasOwnership(moduleId: String): Boolean =
        (ownershipMap[moduleId]?.size() ?: 0) > 0
}
