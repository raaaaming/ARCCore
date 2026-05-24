package cc.arccore.eventbus.runtime.ownership

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks the subscription IDs owned by a single module.
 *
 * All mutations and reads are thread-safe via [ConcurrentHashMap.newKeySet].
 *
 * @param moduleId The module that owns these subscriptions.
 */
class SubscriptionOwnership(val moduleId: String) {

    private val subscriptionIds: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    fun add(id: UUID) = subscriptionIds.add(id)
    fun remove(id: UUID) = subscriptionIds.remove(id)
    fun size(): Int = subscriptionIds.size
    fun snapshot(): Set<UUID> = subscriptionIds.toSet()
}
