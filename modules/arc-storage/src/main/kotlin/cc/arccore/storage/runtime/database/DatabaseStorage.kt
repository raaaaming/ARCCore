package cc.arccore.storage.runtime.database

import cc.arccore.storage.runtime.storage.StorageHandle

/**
 * Database storage handle.
 *
 * All data-access operations are declared as `suspend` to allow non-blocking
 * I/O dispatch. The arc-storage module itself does not depend on kotlinx.coroutines;
 * callers dispatch these on their own coroutine context.
 */
interface DatabaseStorage : StorageHandle {

    /**
     * Executes a read-only [query] with optional [params] and returns a [QueryResult].
     *
     * The caller must close the returned [QueryResult] after use.
     */
    suspend fun query(query: String, vararg params: Any?): QueryResult

    /**
     * Executes a DML statement ([sql]) with optional [params].
     *
     * @return The number of rows affected.
     */
    suspend fun execute(sql: String, vararg params: Any?): Int

    /**
     * Executes the given [block] within a single database transaction.
     * The transaction is committed if [block] completes normally, rolled back on exception.
     */
    suspend fun <R> transaction(block: suspend DatabaseStorage.() -> R): R

    /** Returns `true` if the underlying connection is reachable. */
    suspend fun isConnected(): Boolean
}
