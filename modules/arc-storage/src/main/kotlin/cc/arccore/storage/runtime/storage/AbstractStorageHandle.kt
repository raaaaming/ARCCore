package cc.arccore.storage.runtime.storage

import cc.arccore.storage.runtime.exception.InvalidStorageAccessException
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Base implementation of [StorageHandle] providing thread-safe state management.
 *
 * Subclasses must call [checkOpen] before any data access and may override
 * [onClose] to release resources when the handle transitions to CLOSED.
 */
abstract class AbstractStorageHandle(
    override val handleId: UUID = UUID.randomUUID(),
    override val moduleId: String,
    override val storageType: StorageType
) : StorageHandle {

    private val _state = AtomicReference(StorageHandleState.OPEN)
    override val state: StorageHandleState get() = _state.get()

    /**
     * Asserts that this handle is in [StorageHandleState.OPEN].
     * @throws InvalidStorageAccessException if the handle is CLOSED or STALE.
     */
    protected fun checkOpen() {
        if (_state.get() != StorageHandleState.OPEN) {
            throw InvalidStorageAccessException(
                "StorageHandle[$handleId] for module '$moduleId' is ${_state.get().name}. " +
                    "Cannot access closed or stale storage."
            )
        }
    }

    /**
     * Closes the handle. Transitions from OPEN → CLOSED exactly once,
     * then invokes [onClose]. Subsequent calls are no-ops.
     */
    override fun close() {
        if (_state.compareAndSet(StorageHandleState.OPEN, StorageHandleState.CLOSED)) {
            onClose()
        }
    }

    /**
     * Called once when the handle transitions to CLOSED.
     * Subclasses should release internal resources here.
     */
    protected open fun onClose() {}

    /**
     * Marks this handle as [StorageHandleState.STALE] (externally invalidated).
     * Triggers [onClose] regardless of previous state.
     */
    fun markStale() {
        _state.set(StorageHandleState.STALE)
        onClose()
    }
}
