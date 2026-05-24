package cc.arccore.storage.runtime.async

import java.util.concurrent.CompletableFuture

/**
 * Configures how the storage runtime dispatches asynchronous operations.
 *
 * The [executorFactory] lambda is invoked lazily to obtain an [AsyncStorageExecutor].
 * This indirection allows callers (e.g. arc-coroutine bridge) to inject a
 * coroutine-backed executor without creating a compile-time dependency on
 * kotlinx.coroutines inside arc-storage.
 *
 * @property executorFactory Factory that produces the [AsyncStorageExecutor] on first use.
 */
data class StorageAsyncContext(
    val executorFactory: () -> AsyncStorageExecutor = { NoopAsyncStorageExecutor }
) {
    private val executor: AsyncStorageExecutor by lazy { executorFactory() }

    /**
     * Submits [block] to the configured executor and returns a [CompletableFuture].
     */
    fun <T> submit(block: () -> T): CompletableFuture<T> = executor.submit(block)

    /** Returns `true` if the executor is currently active. */
    fun isActive(): Boolean = executor.isActive()
}
