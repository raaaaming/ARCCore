package cc.arccore.eventbus.runtime.subscription

import java.util.UUID

/**
 * A handle to an active event subscription.
 *
 * Implements [AutoCloseable] so it can be registered with
 * [CleanupScope][cc.arccore.api.module.CleanupScope] for automatic cleanup on module unload:
 *
 * ```kotlin
 * context.cleanupScope.register(
 *     eventBus.subscribe<MyEvent>(moduleId) { event -> ... }
 * )
 * ```
 *
 * Calling [close] (or [cancel]) deactivates the subscription without affecting
 * other subscriptions for the same event type.
 */
interface SubscriptionHandle : AutoCloseable {

    /** Unique identifier for this subscription. */
    val subscriptionId: UUID

    /** The module that owns this subscription. */
    val moduleId: String

    /** Whether this subscription is still active. */
    val isActive: Boolean

    /**
     * Cancels this subscription.
     *
     * Idempotent — calling this multiple times has no additional effect.
     */
    fun cancel()

    /**
     * Cancels this subscription. Alias for [cancel] to satisfy [AutoCloseable].
     */
    override fun close() = cancel()
}
