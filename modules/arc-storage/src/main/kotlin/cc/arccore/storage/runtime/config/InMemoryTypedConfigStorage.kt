package cc.arccore.storage.runtime.config

import cc.arccore.storage.runtime.storage.AbstractStorageHandle
import cc.arccore.storage.runtime.storage.StorageType
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * In-memory typed configuration storage.
 *
 * Values are stored as their raw [T] instances. Serialization is deferred
 * until a [ConfigSerializer] is wired in (future extension point).
 *
 * @param T The domain type for this configuration namespace.
 */
class InMemoryTypedConfigStorage<T : Any>(
    override val moduleId: String,
    private val name: String,
    @Suppress("unused") private val type: KClass<T>
) : AbstractStorageHandle(moduleId = moduleId, storageType = StorageType.CONFIG), TypedConfigStorage<T> {

    private val data = ConcurrentHashMap<String, T>()

    @Volatile
    private var isDirty = false

    override fun get(key: String): T? {
        checkOpen()
        return data[key]
    }

    override fun set(key: String, value: T) {
        checkOpen()
        data[key] = value
        isDirty = true
    }

    override fun getOrDefault(key: String, default: T): T {
        checkOpen()
        return data.getOrDefault(key, default)
    }

    override fun contains(key: String): Boolean {
        checkOpen()
        return data.containsKey(key)
    }

    override fun keys(): Set<String> {
        checkOpen()
        return data.keys.toSet()
    }

    override fun reload() {
        checkOpen()
        // Placeholder — typed file-backed subclasses re-read here.
    }

    override fun save() {
        checkOpen()
        isDirty = false
        // Placeholder — typed file-backed subclasses flush here.
    }

    override fun onClose() {
        if (isDirty) save()
        data.clear()
    }
}
