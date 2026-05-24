package cc.arccore.runtime.resource.tracking

import cc.arccore.runtime.resource.ResourceHandle
import cc.arccore.runtime.resource.ResourceState
import cc.arccore.runtime.resource.registration.ResourceRegistration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class ModuleResourceRecord(val moduleId: String) {

    private val registrations = ConcurrentHashMap<UUID, ResourceRegistration>()

    fun add(registration: ResourceRegistration) {
        registrations[registration.descriptor.id] = registration
    }

    fun remove(id: UUID) {
        registrations.remove(id)
    }

    fun releaseAll(): ReleaseAllResult {
        var released = 0
        var alreadyReleased = 0
        val errors = mutableListOf<Pair<ResourceRegistration, Throwable>>()

        for ((_, reg) in registrations) {
            if (reg.descriptor.isReleased) {
                alreadyReleased++
                continue
            }
            reg.descriptor.state = ResourceState.CLEANING
            try {
                reg.cleanupFn()
                reg.descriptor.state = ResourceState.RELEASED
                released++
            } catch (e: Exception) {
                reg.descriptor.state = ResourceState.RELEASED
                errors += reg to e
            }
        }
        registrations.clear()
        return ReleaseAllResult(released, alreadyReleased, errors)
    }

    fun activeHandles(): List<ResourceHandle> =
        registrations.values.filter { !it.descriptor.isReleased }.map { it.handle }

    fun allDescriptors() = registrations.values.map { it.descriptor }

    fun activeCount(): Int = registrations.values.count { !it.descriptor.isReleased }

    data class ReleaseAllResult(
        val released: Int,
        val alreadyReleased: Int,
        val errors: List<Pair<ResourceRegistration, Throwable>>
    )
}
