package cc.arccore.storage.runtime

import cc.arccore.storage.runtime.async.StorageAsyncContext
import cc.arccore.storage.runtime.cache.CachePolicy
import cc.arccore.storage.runtime.cache.CacheStorage
import cc.arccore.storage.runtime.cache.InMemoryCacheStorage
import cc.arccore.storage.runtime.config.ConfigStorage
import cc.arccore.storage.runtime.config.InMemoryConfigStorage
import cc.arccore.storage.runtime.config.InMemoryTypedConfigStorage
import cc.arccore.storage.runtime.config.TypedConfigStorage
import cc.arccore.storage.runtime.database.DatabaseStorage
import cc.arccore.storage.runtime.database.NoopDatabaseStorage
import cc.arccore.storage.runtime.exception.StorageRuntimeException
import cc.arccore.storage.runtime.file.DefaultFileStorage
import cc.arccore.storage.runtime.file.FileStorage
import cc.arccore.storage.runtime.file.FileStorageOptions
import cc.arccore.storage.runtime.integration.DiagnosticsStorageBridgePort
import cc.arccore.storage.runtime.integration.NoopDiagnosticsStorageBridgePort
import cc.arccore.storage.runtime.ownership.DefaultStorageOwnershipRegistry
import cc.arccore.storage.runtime.ownership.StorageOwnershipRegistry
import cc.arccore.storage.runtime.storage.AbstractStorageHandle
import cc.arccore.storage.runtime.validation.StorageAccessValidator
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

/**
 * Production implementation of [PersistentStorageRuntime].
 *
 * All handle factory methods are thread-safe. The runtime tracks every opened
 * handle via [StorageOwnershipRegistry] so that [unloadAll] and [shutdown]
 * can perform a deterministic, leak-free teardown.
 *
 * @param moduleId          Identifier of the module that owns this runtime instance.
 * @param baseDataPath      Root directory for all file-backed storage for this module.
 * @param ownershipRegistry Registry that tracks handle ownership. Defaults to [DefaultStorageOwnershipRegistry].
 * @param asyncContext      Async dispatch strategy. Defaults to a synchronous no-op executor.
 * @param diagnosticsPort   Bridge port for arc-diagnostics notifications. Defaults to [NoopDiagnosticsStorageBridgePort].
 */
class DefaultPersistentStorageRuntime(
    private val moduleId: String,
    private val baseDataPath: Path,
    private val ownershipRegistry: StorageOwnershipRegistry = DefaultStorageOwnershipRegistry(),
    @Suppress("unused") private val asyncContext: StorageAsyncContext = StorageAsyncContext(),
    private val diagnosticsPort: DiagnosticsStorageBridgePort = NoopDiagnosticsStorageBridgePort
) : PersistentStorageRuntime {

    private val isShutdown = AtomicBoolean(false)

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    override fun config(name: String): ConfigStorage {
        checkNotShutdown()
        val handle = InMemoryConfigStorage(moduleId = moduleId, name = name)
        registerHandle(handle)
        return handle
    }

    override fun <T : Any> config(name: String, type: KClass<T>): TypedConfigStorage<T> {
        checkNotShutdown()
        val handle = InMemoryTypedConfigStorage<T>(moduleId = moduleId, name = name, type = type)
        registerHandle(handle)
        return handle
    }

    override fun file(path: String, options: FileStorageOptions): FileStorage {
        checkNotShutdown()
        StorageAccessValidator.validatePath(path)
        val handle = DefaultFileStorage(
            moduleId = moduleId,
            basePath = baseDataPath,
            relativePath = path,
            options = options
        )
        registerHandle(handle)
        return handle
    }

    override fun <K : Any, V : Any> cache(name: String, policy: CachePolicy): CacheStorage<K, V> {
        checkNotShutdown()
        val handle = InMemoryCacheStorage<K, V>(moduleId = moduleId, name = name, policy = policy)
        registerHandle(handle)
        return handle
    }

    override fun database(name: String): DatabaseStorage {
        checkNotShutdown()
        val handle = NoopDatabaseStorage(moduleId = moduleId, name = name)
        registerHandle(handle)
        return handle
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun unloadAll(moduleId: String) {
        ownershipRegistry.closeAll(moduleId)
    }

    override fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            ownershipRegistry.closeAllHandles()
        }
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private fun registerHandle(handle: AbstractStorageHandle) {
        ownershipRegistry.register(handle)
        diagnosticsPort.onHandleOpened(handle.moduleId, handle.handleId, handle.storageType.name)
    }

    private fun checkNotShutdown() {
        if (isShutdown.get()) {
            throw StorageRuntimeException(
                "PersistentStorageRuntime for module '$moduleId' is already shut down."
            )
        }
    }
}
