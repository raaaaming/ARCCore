package cc.arccore.eventbus.runtime.dispatch

import java.util.UUID

/**
 * Result of a single event dispatch operation.
 */
sealed class DispatchResult {

    abstract val dispatchId: UUID
    abstract val invokedCount: Int
    abstract val wasCancelled: Boolean

    /** Whether the dispatch completed with no errors. */
    abstract val isSuccess: Boolean

    /**
     * All listeners were invoked successfully (or no listeners were registered).
     */
    data class Success(
        override val dispatchId: UUID,
        override val invokedCount: Int,
        override val wasCancelled: Boolean = false
    ) : DispatchResult() {
        override val isSuccess: Boolean = true
    }

    /**
     * Some listeners succeeded but at least one threw an exception.
     *
     * @param errors List of (moduleId, exception) pairs for each failed listener invocation.
     */
    data class PartialFailure(
        override val dispatchId: UUID,
        override val invokedCount: Int,
        val successCount: Int,
        val errors: List<Pair<String, Throwable>>,
        override val wasCancelled: Boolean = false
    ) : DispatchResult() {
        override val isSuccess: Boolean = false
        val failureCount: Int get() = errors.size
    }

    /**
     * The entire dispatch failed before any listener was invoked
     * (e.g., bus is shutdown, no strategy available for async).
     */
    data class Failure(
        override val dispatchId: UUID,
        val reason: String,
        val cause: Throwable? = null,
        override val wasCancelled: Boolean = false
    ) : DispatchResult() {
        override val invokedCount: Int = 0
        override val isSuccess: Boolean = false
    }

    companion object {
        fun noListeners(dispatchId: UUID): Success =
            Success(dispatchId = dispatchId, invokedCount = 0, wasCancelled = false)
    }
}
