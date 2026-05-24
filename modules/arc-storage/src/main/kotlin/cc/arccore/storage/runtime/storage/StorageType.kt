package cc.arccore.storage.runtime.storage

/**
 * Discriminates the kind of resource a [StorageHandle] represents.
 */
enum class StorageType {
    /** Key-value configuration data. */
    CONFIG,
    /** Raw file I/O. */
    FILE,
    /** In-memory or distributed cache. */
    CACHE,
    /** Relational or NoSQL database connection. */
    DATABASE
}
