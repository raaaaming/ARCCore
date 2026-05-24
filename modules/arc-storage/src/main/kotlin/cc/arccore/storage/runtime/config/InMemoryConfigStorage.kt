package cc.arccore.storage.runtime.config

import cc.arccore.storage.runtime.storage.AbstractStorageHandle
import cc.arccore.storage.runtime.storage.StorageType
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of [ConfigStorage] backed by a [ConcurrentHashMap].
 *
 * Persistence (reload/save) is intentionally a no-op; file-backed variants
 * extend this class and override those methods.
 */
class InMemoryConfigStorage(
    override val moduleId: String,
    private val name: String,
    initialEntries: Map<String, String> = emptyMap()
) : AbstractStorageHandle(moduleId = moduleId, storageType = StorageType.CONFIG), ConfigStorage {

    private val data = ConcurrentHashMap<String, String>(initialEntries)

    @Volatile
    private var isDirty = false

    override fun get(key: String): String? {
        checkOpen()
        return data[key]
    }

    override fun set(key: String, value: String) {
        checkOpen()
        data[key] = value
        isDirty = true
    }

    override fun getOrDefault(key: String, default: String): String {
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
        // Placeholder — file-backed subclasses re-read the backing file here.
    }

    override fun save() {
        checkOpen()
        isDirty = false
        // Placeholder — file-backed subclasses flush dirty data here.
    }

    override fun onClose() {
        if (isDirty) save()
        data.clear()
    }
}
