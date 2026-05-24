package cc.arccore.storage.runtime.database

/**
 * Abstraction over a pool of database connections.
 *
 * The contract is intentionally minimal; concrete implementations will
 * wrap HikariCP, SQLite, or custom connection factories.
 */
interface DatabaseConnectionPool : AutoCloseable {

    /** Returns `true` if the pool has at least one available connection. */
    fun isAvailable(): Boolean

    /** Returns the number of connections currently active (in use). */
    fun activeConnections(): Int

    /** Returns the maximum pool size configured for this pool. */
    fun maxPoolSize(): Int

    /**
     * Closes all connections and shuts down the pool.
     * After this call [isAvailable] must return `false`.
     */
    override fun close()
}
