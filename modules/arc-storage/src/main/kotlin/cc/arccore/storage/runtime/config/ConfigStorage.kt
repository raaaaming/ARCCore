package cc.arccore.storage.runtime.config

import cc.arccore.storage.runtime.storage.StorageHandle

/**
 * String-keyed configuration storage handle.
 *
 * Provides basic CRUD operations over a flat key-value store.
 * Implementations may be backed by an in-memory map, a YAML/TOML file,
 * or a remote config service.
 */
interface ConfigStorage : StorageHandle {

    /**
     * Returns the value associated with [key], or `null` if absent.
     */
    fun get(key: String): String?

    /**
     * Associates [key] with [value] and marks the storage dirty.
     */
    fun set(key: String, value: String)

    /**
     * Returns the value for [key], falling back to [default] if absent.
     */
    fun getOrDefault(key: String, default: String): String

    /**
     * Returns `true` if [key] exists in this storage.
     */
    fun contains(key: String): Boolean

    /**
     * Returns an immutable snapshot of all keys present in this storage.
     */
    fun keys(): Set<String>

    /**
     * Re-reads the backing source (file, remote, etc.).
     * No-op for in-memory-only implementations.
     */
    fun reload()

    /**
     * Flushes pending changes to the backing source.
     * No-op for in-memory-only implementations.
     */
    fun save()
}
