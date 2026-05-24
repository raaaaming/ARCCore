package cc.arccore.storage.runtime

import cc.arccore.api.module.StorageRuntime
import cc.arccore.storage.runtime.cache.CachePolicy
import cc.arccore.storage.runtime.cache.CacheStorage
import cc.arccore.storage.runtime.config.ConfigStorage
import cc.arccore.storage.runtime.config.TypedConfigStorage
import cc.arccore.storage.runtime.database.DatabaseStorage
import cc.arccore.storage.runtime.file.FileStorage
import cc.arccore.storage.runtime.file.FileStorageOptions
import kotlin.reflect.KClass

/**
 * Primary storage API exposed to modules.
 *
 * Each factory method opens a new [cc.arccore.storage.runtime.storage.StorageHandle],
 * registers it under the calling module's ownership, and returns it to the caller.
 * All handles are automatically closed when [unloadAll] is called for the owning module.
 *
 * Callers must not use returned handles after calling [unloadAll] or [shutdown].
 */
interface PersistentStorageRuntime : StorageRuntime {

    /**
     * Opens a string-keyed configuration storage named [name].
     */
    fun config(name: String): ConfigStorage

    /**
     * Opens a typed configuration storage named [name] for type [T].
     */
    fun <T : Any> config(name: String, type: KClass<T>): TypedConfigStorage<T>

    /**
     * Opens a file storage for [path] (relative to the module's data folder).
     *
     * @param path    Relative path. Must not contain `..` or other traversal sequences.
     * @param options Controls creation and access mode; defaults to [FileStorageOptions.DEFAULT].
     */
    fun file(path: String, options: FileStorageOptions = FileStorageOptions.DEFAULT): FileStorage

    /**
     * Opens a typed in-memory cache named [name] with the given [policy].
     *
     * @param K Cache key type.
     * @param V Cache value type.
     */
    fun <K : Any, V : Any> cache(name: String, policy: CachePolicy = CachePolicy.DEFAULT): CacheStorage<K, V>

    /**
     * Opens a database storage handle named [name].
     *
     * The default implementation returns a [cc.arccore.storage.runtime.database.NoopDatabaseStorage]
     * until a real provider is configured.
     */
    fun database(name: String): DatabaseStorage

    /**
     * Closes all storage handles owned by [moduleId].
     * Called by the unload pipeline when a module is being removed.
     */
    fun unloadAll(moduleId: String)

    /**
     * Shuts down the entire storage runtime, closing every open handle.
     * Should only be called during server shutdown.
     */
    fun shutdown()
}
