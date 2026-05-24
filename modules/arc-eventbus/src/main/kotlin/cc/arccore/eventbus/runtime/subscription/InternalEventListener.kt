package cc.arccore.eventbus.runtime.subscription

import cc.arccore.eventbus.runtime.event.InternalEvent

/**
 * Listener for events dispatched through [InternalEventBus].
 *
 * Declared as a `fun interface` to enable lambda syntax:
 * ```kotlin
 * eventBus.subscribe<MyEvent>(moduleId) { event ->
 *     // handle event
 * }
 * ```
 *
 * The [handle] function is a `suspend` function, enabling coroutine-based listeners.
 * When used with [SyncEventDispatcher], it is invoked via `runBlocking`.
 */
fun interface InternalEventListener<T : InternalEvent> {

    /**
     * Handles the received event.
     *
     * @param event The event that was dispatched.
     */
    suspend fun handle(event: T)
}
