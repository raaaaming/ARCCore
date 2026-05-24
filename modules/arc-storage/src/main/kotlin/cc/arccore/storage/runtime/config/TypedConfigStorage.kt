package cc.arccore.storage.runtime.config

import cc.arccore.storage.runtime.storage.StorageHandle

/**
 * Typed variant of [ConfigStorage] that serializes/deserializes values to [T].
 *
 * @param T The domain type used for typed get/set operations.
 */
interface TypedConfigStorage<T : Any> : StorageHandle {

    /**
     * Returns the deserialized value for [key], or `null` if absent.
     */
    fun get(key: String): T?

    /**
     * Serializes [value] and stores it under [key].
     */
    fun set(key: String, value: T)

    /**
     * Returns the deserialized value for [key], falling back to [default].
     */
    fun getOrDefault(key: String, default: T): T

    /**
     * Returns `true` if [key] exists.
     */
    fun contains(key: String): Boolean

    /**
     * Returns an immutable snapshot of all keys.
     */
    fun keys(): Set<String>

    /**
     * Re-reads the backing source.
     */
    fun reload()

    /**
     * Flushes pending changes.
     */
    fun save()
}
