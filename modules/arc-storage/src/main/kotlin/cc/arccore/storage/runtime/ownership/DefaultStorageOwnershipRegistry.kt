package cc.arccore.storage.runtime.ownership

import cc.arccore.storage.runtime.storage.AbstractStorageHandle
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Default thread-safe implementation of [StorageOwnershipRegistry].
 *
 * Uses a [ConcurrentHashMap] keyed by module ID, each mapping to a
 * [CopyOnWriteArraySet] of open handles for that module. This design
 * favours read-heavy workloads (diagnostics snapshots) while keeping
 * registration and bulk-close lock-free.
 */
class DefaultStorageOwnershipRegistry : StorageOwnershipRegistry {

    private val registry = ConcurrentHashMap<String, CopyOnWriteArraySet<AbstractStorageHandle>>()

    override fun register(handle: AbstractStorageHandle) {
        registry.computeIfAbsent(handle.moduleId) { CopyOnWriteArraySet() }.add(handle)
    }

    override fun closeAll(moduleId: String) {
        registry.remove(moduleId)?.forEach { handle ->
            runCatching { handle.close() }
        }
    }

    override fun closeAllHandles() {
        val allModules = registry.keys.toList()
        allModules.forEach { closeAll(it) }
    }

    override fun snapshot(): List<StorageOwnership> =
        registry.flatMap { (moduleId, handles) ->
            handles.map { handle ->
                StorageOwnership(
                    moduleId = moduleId,
                    handleId = handle.handleId,
                    storageType = handle.storageType
                )
            }
        }

    override fun countFor(moduleId: String): Int =
        registry[moduleId]?.size ?: 0
}
