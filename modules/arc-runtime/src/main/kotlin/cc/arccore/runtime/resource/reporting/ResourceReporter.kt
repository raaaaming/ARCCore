package cc.arccore.runtime.resource.reporting

import cc.arccore.runtime.resource.ResourceState
import cc.arccore.runtime.resource.ResourceType
import cc.arccore.runtime.resource.snapshot.ModuleResourceSnapshot
import cc.arccore.runtime.resource.snapshot.ResourceSnapshot

class ResourceReporter {

    fun formatFull(snapshot: ResourceSnapshot): List<String> {
        val lines = mutableListOf<String>()
        lines += "§7[ARCCore] §fResource Ownership Graph §7━━━━━━━━━━━━━━━━━━━━━"
        lines += "§7 Total tracked: §f${snapshot.totalTracked}§7 | Active: §f${snapshot.totalActive}§7 | Released: §f${snapshot.totalReleased}"
        lines += "§7 Affected modules: §f${snapshot.affectedModules}"

        val byType = snapshot.activeByType()
        if (byType.isNotEmpty()) {
            lines += "§7 Active by type:"
            for ((type, count) in byType.entries.sortedByDescending { it.value }) {
                lines += "§7   ${typeColor(type)}${type.name}§7: §f$count"
            }
        }

        lines += "§7─────────────────────────────────────────────────────────"
        for (m in snapshot.modules.sortedBy { it.moduleId }) {
            if (m.totalCount > 0) {
                lines += formatModuleLine(m)
            }
        }
        lines += "§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        return lines
    }

    fun formatModuleDetail(snapshot: ModuleResourceSnapshot): List<String> {
        val lines = mutableListOf<String>()
        lines += "§7[ARCCore] §fResources for §e${snapshot.moduleId}"
        lines += "§7 Total: §f${snapshot.totalCount}§7 | Active: §f${snapshot.activeCount}§7 | Released: §f${snapshot.releasedCount}"

        for ((type, resources) in snapshot.byType()) {
            val active = resources.filter { !it.isReleased }
            if (active.isEmpty()) continue
            lines += "§7 ${typeColor(type)}${type.name}§7 (${active.size}):"
            for (r in active) {
                lines += "§7   §f${r.name}§7 [${stateColor(r.state)}${r.state.name}§7]"
            }
        }

        if (snapshot.activeCount == 0) {
            lines += "§7 §aNo active resources — clean."
        }
        return lines
    }

    fun formatUnloadCheck(moduleId: String, activeCount: Int, criticalCount: Int): List<String> {
        return if (activeCount == 0) {
            listOf("§7[ARCCore] §aModule '§e$moduleId§a' — no unreleased resources after unload.")
        } else {
            buildList {
                add("§7[ARCCore] §cModule '§e$moduleId§c' — §f$activeCount§c resource(s) NOT released after unload!")
                if (criticalCount > 0) add("§7  §4$criticalCount critical resource(s) (EXECUTOR/COROUTINE_SCOPE/LISTENER/DATABASE) — classloader leak risk!")
            }
        }
    }

    private fun formatModuleLine(m: ModuleResourceSnapshot): String {
        val active = m.activeByType().entries
            .sortedByDescending { it.value }
            .joinToString(" ") { (t, c) -> "${typeColor(t)}${t.name}§7:§f$c" }
        return "§7  §e${m.moduleId}§7 active:§f${m.activeCount}§7 $active"
    }

    private fun typeColor(type: ResourceType) = when (type) {
        ResourceType.EXECUTOR, ResourceType.COROUTINE_SCOPE -> "§c"
        ResourceType.LISTENER, ResourceType.COMMAND, ResourceType.SERVICE -> "§a"
        ResourceType.SCHEDULER_TASK, ResourceType.COROUTINE_JOB -> "§e"
        ResourceType.DATABASE, ResourceType.NETWORK -> "§6"
        else -> "§7"
    }

    private fun stateColor(state: ResourceState) = when (state) {
        ResourceState.ACTIVE, ResourceState.CREATED -> "§a"
        ResourceState.CLEANING -> "§e"
        ResourceState.RELEASED, ResourceState.VERIFIED -> "§8"
    }
}
