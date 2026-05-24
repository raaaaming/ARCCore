package cc.arccore.storage.runtime.async

import java.util.concurrent.CompletableFuture

/**
 * Port for dispatching storage operations to an async execution context.
 *
 * Keeping this interface in arc-storage (rather than depending on
 * kotlinx.coroutines directly) preserves the no-coroutines compile-time
 * boundary for the storage module.
 */
interface AsyncStorageExecutor {

    /**
     * Submits [block] for async execution and returns a [CompletableFuture]
     * that completes with the result (or an exception).
     */
    fun <T> submit(block: () -> T): CompletableFuture<T>

    /**
     * Returns `true` if there is an active executor capable of accepting tasks.
     */
    fun isActive(): Boolean
}
