package cc.arccore.storage.runtime.config

/**
 * Strategy interface for future typed serialization of configuration values.
 *
 * Implementations convert between raw string representations stored on disk
 * and strongly-typed domain objects.
 *
 * @param T The domain type this serializer handles.
 */
interface ConfigSerializer<T : Any> {

    /**
     * Serializes [value] into a string suitable for storage.
     */
    fun serialize(value: T): String

    /**
     * Deserializes [raw] back into [T].
     * @throws cc.arccore.storage.runtime.exception.ConfigValidationException if [raw] is invalid.
     */
    fun deserialize(raw: String): T
}
