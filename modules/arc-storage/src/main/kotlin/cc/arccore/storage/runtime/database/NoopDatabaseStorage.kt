package cc.arccore.storage.runtime.database

import cc.arccore.storage.runtime.exception.StorageRuntimeException
import cc.arccore.storage.runtime.storage.AbstractStorageHandle
import cc.arccore.storage.runtime.storage.StorageType

/**
 * Placeholder [DatabaseStorage] that throws on every data-access call.
 *
 * Used as a stand-in until a real database provider (HikariCP, SQLite, etc.)
 * is wired into the runtime. Allows the rest of the storage system to compile
 * and initialise without a live database dependency.
 */
class NoopDatabaseStorage(
    override val moduleId: String,
    @Suppress("unused") private val name: String
) : AbstractStorageHandle(moduleId = moduleId, storageType = StorageType.DATABASE), DatabaseStorage {

    private fun notImplemented(): Nothing =
        throw StorageRuntimeException(
            "NoopDatabaseStorage: no database provider has been configured for module '$moduleId'. " +
                "Wire a concrete DatabaseStorage implementation via PersistentStorageRuntime."
        )

    override suspend fun query(query: String, vararg params: Any?): QueryResult = notImplemented()

    override suspend fun execute(sql: String, vararg params: Any?): Int = notImplemented()

    override suspend fun <R> transaction(block: suspend DatabaseStorage.() -> R): R = notImplemented()

    override suspend fun isConnected(): Boolean = false
}
