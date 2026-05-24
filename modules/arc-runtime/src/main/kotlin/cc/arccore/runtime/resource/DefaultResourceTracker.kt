package cc.arccore.runtime.resource

import cc.arccore.runtime.resource.cleanup.ResourceCleanupError
import cc.arccore.runtime.resource.cleanup.ResourceCleanupResult
import cc.arccore.runtime.resource.integration.ResourceLeakBridge
import cc.arccore.runtime.resource.registration.ResourceRegistration
import cc.arccore.runtime.resource.reporting.ResourceReporter
import cc.arccore.runtime.resource.snapshot.ModuleResourceSnapshot
import cc.arccore.runtime.resource.snapshot.ResourceSnapshot
import cc.arccore.runtime.resource.tracking.ModuleResourceRecord
import cc.arccore.runtime.resource.validation.ResourceValidator
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DefaultResourceTracker : ResourceTracker {

    private val moduleRecords = ConcurrentHashMap<String, ModuleResourceRecord>()
    private val reporter = ResourceReporter()

    @Volatile
    private var leakBridge: ResourceLeakBridge? = null

    fun setLeakBridge(bridge: ResourceLeakBridge) {
        this.leakBridge = bridge
    }

    override fun track(moduleId: String, name: String, type: ResourceType, resource: AutoCloseable): ResourceHandle {
        return register(moduleId, name, type, resource::close)
    }

    override fun track(moduleId: String, name: String, type: ResourceType, cleanup: () -> Unit): ResourceHandle {
        return register(moduleId, name, type, cleanup)
    }

    private fun register(moduleId: String, name: String, type: ResourceType, cleanupFn: () -> Unit): ResourceHandle {
        ResourceValidator.requireValidOwner(moduleId)
        ResourceValidator.requireValidName(name)

        val descriptor = ResourceDescriptor(
            id = UUID.randomUUID(),
            name = name,
            type = type,
            owner = ResourceOwner(moduleId = moduleId),
            state = ResourceState.ACTIVE,
            createdAt = Instant.now()
        )

        val handle = DefaultResourceHandle(
            descriptor = descriptor,
            cleanupFn = cleanupFn,
            onReleased = { desc ->
                moduleRecords[desc.moduleId]?.remove(desc.id)
            }
        )

        val registration = ResourceRegistration(descriptor, handle, cleanupFn)
        moduleRecords.getOrPut(moduleId) { ModuleResourceRecord(moduleId) }.add(registration)
        return handle
    }

    override fun releaseModule(moduleId: String): ResourceCleanupResult {
        val record = moduleRecords.remove(moduleId)
            ?: return ResourceCleanupResult(moduleId, 0, 0, emptyList())

        val result = record.releaseAll()

        val cleanupErrors = result.errors.map { (reg, cause) ->
            ResourceCleanupError(reg.descriptor, cause)
        }

        val cleanupResult = ResourceCleanupResult(
            moduleId = moduleId,
            releasedCount = result.released,
            alreadyReleasedCount = result.alreadyReleased,
            errors = cleanupErrors
        )

        leakBridge?.notifyCleanupResult(cleanupResult)
        return cleanupResult
    }

    override fun releaseAll(): Map<String, ResourceCleanupResult> {
        val results = mutableMapOf<String, ResourceCleanupResult>()
        for (moduleId in moduleRecords.keys.toList()) {
            results[moduleId] = releaseModule(moduleId)
        }
        return results
    }

    override fun takeSnapshot(): ResourceSnapshot {
        val now = Instant.now()
        val moduleSnapshots = moduleRecords.entries.map { (moduleId, record) ->
            ModuleResourceSnapshot(
                moduleId = moduleId,
                snapshotAt = now,
                resources = record.allDescriptors().toList()
            )
        }
        return ResourceSnapshot(snapshotAt = now, modules = moduleSnapshots)
    }

    override fun getModuleSnapshot(moduleId: String): ModuleResourceSnapshot {
        val now = Instant.now()
        val record = moduleRecords[moduleId]
            ?: return ModuleResourceSnapshot(moduleId, now, emptyList())
        return ModuleResourceSnapshot(moduleId, now, record.allDescriptors().toList())
    }

    override fun activeCount(): Int = moduleRecords.values.sumOf { it.activeCount() }

    override fun activeCountForModule(moduleId: String): Int =
        moduleRecords[moduleId]?.activeCount() ?: 0

    override fun getReporter(): ResourceReporter = reporter
}
