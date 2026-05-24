package cc.arccore.runtime.resource.snapshot

import cc.arccore.runtime.resource.ResourceType
import java.time.Instant

data class ResourceSnapshot(
    val snapshotAt: Instant,
    val modules: List<ModuleResourceSnapshot>
) {
    val totalActive: Int get() = modules.sumOf { it.activeCount }
    val totalReleased: Int get() = modules.sumOf { it.releasedCount }
    val totalTracked: Int get() = modules.sumOf { it.totalCount }
    val affectedModules: Int get() = modules.count { it.activeCount > 0 }

    fun activeByType(): Map<ResourceType, Int> {
        val result = mutableMapOf<ResourceType, Int>()
        for (m in modules) {
            for ((type, count) in m.activeByType()) {
                result[type] = (result[type] ?: 0) + count
            }
        }
        return result
    }

    fun getModule(moduleId: String): ModuleResourceSnapshot? =
        modules.find { it.moduleId == moduleId }
}
