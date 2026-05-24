package cc.arccore.eventbus.runtime.exception

/**
 * Thrown when a subscription registration is rejected due to invalid parameters.
 *
 * @param moduleId The ID of the module that attempted the invalid subscription.
 * @param reason A human-readable explanation of why the subscription is invalid.
 */
class InvalidSubscriptionException(
    val moduleId: String,
    val reason: String,
    cause: Throwable? = null
) : EventBusException(
    "Invalid subscription from module '$moduleId': $reason",
    cause
)
