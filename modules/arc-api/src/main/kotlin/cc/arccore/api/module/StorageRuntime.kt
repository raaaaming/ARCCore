package cc.arccore.api.module

/**
 * Marker interface for the storage runtime provided to a module via [ModuleContext.storage].
 *
 * The concrete implementation ([cc.arccore.storage.runtime.PersistentStorageRuntime])
 * lives in arc-storage. arc-api depends only on this marker so that the API layer
 * remains free of storage implementation details.
 *
 * When no storage provider is active, [ModuleContext.storage] returns [NOOP].
 */
interface StorageRuntime {
    companion object {
        /** Inert instance returned when no storage provider is configured. */
        val NOOP: StorageRuntime = object : StorageRuntime {}
    }
}
