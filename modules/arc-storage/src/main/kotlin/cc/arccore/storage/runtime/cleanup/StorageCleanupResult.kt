package cc.arccore.storage.runtime.cleanup

/**
 * Outcome of a [StorageCleanupStep] execution.
 */
sealed class StorageCleanupResult {

    /**
     * The step completed without errors.
     *
     * @property closedHandles Number of storage handles that were closed.
     */
    data class Success(val closedHandles: Int = 0) : StorageCleanupResult()

    /**
     * The step completed but encountered non-fatal warnings.
     *
     * @property warnings Human-readable descriptions of each warning.
     * @property closedHandles Number of handles that were closed despite the warnings.
     */
    data class PartialSuccess(
        val warnings: List<String>,
        val closedHandles: Int = 0
    ) : StorageCleanupResult()

    /**
     * The step failed with an unrecoverable error.
     *
     * @property cause The underlying exception.
     * @property message A human-readable failure summary.
     */
    data class Failure(
        val cause: Throwable,
        val message: String = cause.message ?: "Unknown storage cleanup failure"
    ) : StorageCleanupResult()
}
