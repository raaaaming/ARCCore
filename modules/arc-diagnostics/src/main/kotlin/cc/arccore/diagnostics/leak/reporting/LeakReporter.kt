package cc.arccore.diagnostics.leak.reporting

import cc.arccore.diagnostics.leak.model.LeakReport
import cc.arccore.diagnostics.leak.model.LeakSeverity
import cc.arccore.diagnostics.leak.model.LeakType
import cc.arccore.diagnostics.leak.model.UnloadVerificationResult

class LeakReporter {

    fun formatSummary(leaks: List<LeakReport>): List<String> {
        if (leaks.isEmpty()) {
            return listOf(
                "§7[ARCCore] §aNo leaks detected. Runtime is clean."
            )
        }

        val lines = mutableListOf<String>()
        lines += "§7[ARCCore] §cLeak Detection Report §7━━━━━━━━━━━━━━━━━━━━━━━━"
        lines += "§7 Total leaks: §c${leaks.size}§7 across §f${leaks.map { it.moduleId }.toSet().size}§7 module(s)"

        val bySeverity = leaks.groupBy { it.severity }
        LeakSeverity.entries.reversed().forEach { sev ->
            val count = bySeverity[sev]?.size ?: 0
            if (count > 0) lines += "§7  ${severityColor(sev)}${sev.name}§7: §f$count"
        }

        lines += "§7─────────────────────────────────────────────────────────"

        val byModule = leaks.groupBy { it.moduleId }
        for ((moduleId, moduleLeaks) in byModule.entries.sortedBy { it.key }) {
            lines += "§7 Module §e$moduleId§7:"
            for (leak in moduleLeaks) {
                lines += "§7  ${severityColor(leak.severity)}[${leak.type.name}]§7 ${leak.description}"
            }
        }

        lines += "§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        return lines
    }

    fun formatDetail(report: LeakReport): List<String> {
        val lines = mutableListOf<String>()
        lines += "§7[ARCCore] §fLeak Detail"
        lines += "§7 Module: §e${report.moduleId}"
        lines += "§7 Type: §f${report.type.name}"
        lines += "§7 Severity: ${severityColor(report.severity)}${report.severity.name}"
        lines += "§7 Description: §f${report.description}"
        if (report.details.isNotEmpty()) {
            lines += "§7 Details:"
            for ((k, v) in report.details) {
                lines += "§7   §f$k §7= §f$v"
            }
        }
        return lines
    }

    fun formatVerificationResult(result: UnloadVerificationResult): List<String> {
        val lines = mutableListOf<String>()
        val statusColor = if (result.isClean) "§a" else "§c"
        val statusText = if (result.isClean) "CLEAN" else "LEAKED"

        lines += "§7[ARCCore] Unload Verification §7— §e${result.moduleId}"
        lines += "§7 Status: ${statusColor}$statusText"
        lines += "§7 GC Candidate: ${boolColor(result.gcCandidate)}${result.gcCandidate}"
        lines += "§7 ClassLoader Released: ${boolColor(result.classLoaderDereferenced)}${result.classLoaderDereferenced}"
        lines += "§7 Registry Clean: ${boolColor(result.registryClean)}${result.registryClean}"
        lines += "§7 Resources Clean: ${boolColor(result.activeResourcesClean)}${result.activeResourcesClean}"
        lines += "§7 Cleanup Complete: ${boolColor(result.cleanupComplete)}${result.cleanupComplete}"

        if (result.leaks.isEmpty()) {
            lines += "§7 Leaks: §anone"
        } else {
            lines += "§7 Leaks §c(${result.leaks.size})§7:"
            for (leak in result.leaks) {
                lines += "§7  ${severityColor(leak.severity)}[${leak.type.name}]§7 ${leak.description}"
            }
        }

        return lines
    }

    fun formatTypeExplanation(type: LeakType): List<String> = when (type) {
        LeakType.CLASSLOADER_LEAK -> listOf(
            "§6ClassLoader Leak§7:",
            "§7 Each module loads with its own URLClassLoader. When a module is unloaded,",
            "§7 all strong references to that classloader must be released so the GC can collect it.",
            "§7 If even ONE strong reference remains (e.g., a static field, a running thread,",
            "§7 a registered Bukkit listener), the entire class hierarchy stays in JVM metaspace.",
            "§7 Over many reloads this accumulates and causes §cOutOfMemoryError (Metaspace)§7."
        )
        LeakType.STALE_LISTENER, LeakType.STALE_SERVICE, LeakType.STALE_COMMAND -> listOf(
            "§6Stale Reference§7:",
            "§7 A stale reference is a reference to an object from a previous module generation.",
            "§7 Example: Module B holds EconomyServiceImpl from Module A's OLD classloader.",
            "§7 After A reloads, B still calls the old impl — this causes §cClassCastException§7",
            "§7 because the NEW EconomyServiceImpl is a different class (different classloader),",
            "§7 even if the source code is identical. The old classloader cannot be GC'd either."
        )
        LeakType.ORPHAN_COROUTINE -> listOf(
            "§6Orphan Coroutine§7:",
            "§7 A coroutine launched inside a module captures its surrounding closure.",
            "§7 If that coroutine outlives the module (scope not cancelled on unload),",
            "§7 it holds references to old services, listeners, and the old classloader.",
            "§7 Always cancel the module's CoroutineScope in the lifecycle onDisable/onUnload."
        )
        LeakType.ORPHAN_EXECUTOR -> listOf(
            "§6Orphan Executor§7:",
            "§7 An ExecutorService manages a thread pool. Threads hold a reference to their",
            "§7 classloader context. An unshutdown executor after module unload keeps threads",
            "§7 alive, pins the old classloader, and wastes system resources."
        )
        LeakType.ORPHAN_SCHEDULER_TASK -> listOf(
            "§6Orphan Scheduler Task§7:",
            "§7 Bukkit's scheduler tasks typically capture `this` or service references.",
            "§7 If the task is not cancelled before module unload, it continues to run",
            "§7 with stale references. The captured closure holds the old classloader."
        )
        else -> listOf("§7 No detailed explanation available for §f${type.name}§7.")
    }

    private fun severityColor(severity: LeakSeverity) = when (severity) {
        LeakSeverity.LOW -> "§e"
        LeakSeverity.MEDIUM -> "§6"
        LeakSeverity.HIGH -> "§c"
        LeakSeverity.CRITICAL -> "§4"
    }

    private fun boolColor(value: Boolean) = if (value) "§a" else "§c"
}
