package cc.arccore.eventbus.runtime.subscription

import cc.arccore.eventbus.runtime.event.InternalEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * Thread-safe registry for all active event subscriptions.
 *
 * Uses [ConcurrentHashMap] keyed by event [KClass] with [CopyOnWriteArrayList] values
 * to allow lock-free iteration during dispatch while supporting concurrent registration.
 */
class SubscriptionRegistry {

    private val subscriptions =
        ConcurrentHashMap<KClass<*>, CopyOnWriteArrayList<EventSubscription<*>>>()

    /**
     * Registers a subscription and returns a [SubscriptionHandle] for cancellation.
     */
    fun <T : InternalEvent> register(subscription: EventSubscription<T>): SubscriptionHandle {
        subscriptions
            .computeIfAbsent(subscription.eventType) { CopyOnWriteArrayList() }
            .add(subscription)
        return DefaultSubscriptionHandle(subscription, this)
    }

    /**
     * Returns all active subscriptions for the given event type, sorted by phase then priority.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : InternalEvent> getActiveSubscriptions(eventType: KClass<T>): List<EventSubscription<T>> {
        return (subscriptions[eventType] ?: return emptyList())
            .filter { it.active }
            .sortedWith(compareBy({ it.phase.order }, { it.priority.order }))
            as List<EventSubscription<T>>
    }

    /**
     * Deactivates the subscription with the given [subscriptionId].
     *
     * @return true if the subscription was found and deactivated.
     */
    fun cancel(subscriptionId: UUID): Boolean {
        for (list in subscriptions.values) {
            val sub = list.find { it.subscriptionId == subscriptionId } ?: continue
            return sub.deactivate()
        }
        return false
    }

    /**
     * Deactivates all subscriptions owned by [moduleId].
     *
     * @return The number of subscriptions that were deactivated.
     */
    fun cancelAllForModule(moduleId: String): Int {
        var count = 0
        for (list in subscriptions.values) {
            for (sub in list) {
                if (sub.moduleId == moduleId && sub.deactivate()) {
                    count++
                }
            }
        }
        return count
    }

    /**
     * Removes all inactive subscriptions from the registry to free memory.
     * Safe to call at any time.
     */
    fun purgeInactive() {
        for ((key, list) in subscriptions) {
            list.removeIf { !it.active }
            if (list.isEmpty()) {
                subscriptions.remove(key, list)
            }
        }
    }

    /**
     * Returns a snapshot of all registered subscriptions (including inactive ones).
     */
    fun allSubscriptions(): List<EventSubscription<*>> =
        subscriptions.values.flatten()

    /**
     * Returns the total number of active subscriptions across all event types.
     */
    fun activeCount(): Int =
        subscriptions.values.sumOf { list -> list.count { it.active } }

    /**
     * Returns the number of distinct event types with at least one subscription (including inactive).
     */
    fun eventTypeCount(): Int = subscriptions.size
}

/**
 * Default [SubscriptionHandle] implementation backed by [EventSubscription] and [SubscriptionRegistry].
 */
internal class DefaultSubscriptionHandle(
    private val subscription: EventSubscription<*>,
    private val registry: SubscriptionRegistry
) : SubscriptionHandle {

    override val subscriptionId: UUID get() = subscription.subscriptionId
    override val moduleId: String get() = subscription.moduleId
    override val isActive: Boolean get() = subscription.active

    override fun cancel() {
        registry.cancel(subscriptionId)
    }
}
