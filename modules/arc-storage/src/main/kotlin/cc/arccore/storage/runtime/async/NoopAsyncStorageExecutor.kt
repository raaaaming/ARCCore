package cc.arccore.storage.runtime.async

import java.util.concurrent.CompletableFuture

/**
 * No-op [AsyncStorageExecutor] that runs every submitted block synchronously
 * on the calling thread.
 *
 * Suitable as a default when no explicit async context has been provided,
 * or in test environments where threading is undesirable.
 */
object NoopAsyncStorageExecutor : AsyncStorageExecutor {

    override fun <T> submit(block: () -> T): CompletableFuture<T> =
        try {
            CompletableFuture.completedFuture(block())
        } catch (ex: Exception) {
            CompletableFuture.failedFuture(ex)
        }

    override fun isActive(): Boolean = true
}
