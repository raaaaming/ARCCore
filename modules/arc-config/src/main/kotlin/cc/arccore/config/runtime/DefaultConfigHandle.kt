package cc.arccore.config.runtime

import cc.arccore.config.runtime.config.ConfigHandleState
import cc.arccore.config.runtime.exception.ConfigRuntimeException
import cc.arccore.config.runtime.exception.StaleConfigAccessException
import cc.arccore.config.runtime.reload.ReloadGeneration
import java.util.concurrent.atomic.AtomicReference

/**
 * Default implementation of [ConfigHandle].
 *
 * Captures the [generation] counter value at load time and compares it against the
 * runtime [ReloadGeneration] to detect staleness. State transitions are atomic:
 * OPEN → STALE (on reload) and OPEN/STALE → CLOSED (on close).
 *
 * [close] is idempotent — calling it multiple times is safe.
 */
class DefaultConfigHandle<T : Any>(
    override val moduleId: String,
    override val configPath: String,
    private val value: T,
    override val generation: Long,
    private val reloadGeneration: ReloadGeneration
) : ConfigHandle<T> {

    private val state = AtomicReference(ConfigHandleState.OPEN)

    override val isOpen: Boolean get() = state.get() == ConfigHandleState.OPEN

    override fun get(): T {
        return when (val current = state.get()) {
            ConfigHandleState.STALE -> throw StaleConfigAccessException(
                configPath,
                generation,
                reloadGeneration.current()
            )
            ConfigHandleState.CLOSED -> throw ConfigRuntimeException(
                "ConfigHandle for '$configPath' is already closed"
            )
            ConfigHandleState.OPEN -> value
            else -> throw ConfigRuntimeException(
                "ConfigHandle for '$configPath' is in unexpected state: $current"
            )
        }
    }

    override fun isStale(): Boolean = reloadGeneration.isStale(generation)

    /**
     * Transitions state from OPEN to STALE (CAS — safe to call from multiple threads).
     * A CLOSED handle will not be transitioned back to STALE.
     */
    fun markStale() {
        state.compareAndSet(ConfigHandleState.OPEN, ConfigHandleState.STALE)
    }

    /**
     * Closes this handle, freeing any retained reference.
     * Transitions OPEN or STALE to CLOSED. Idempotent.
     */
    override fun close() {
        // Unconditional set — we always want CLOSED to be terminal
        state.set(ConfigHandleState.CLOSED)
    }
}
