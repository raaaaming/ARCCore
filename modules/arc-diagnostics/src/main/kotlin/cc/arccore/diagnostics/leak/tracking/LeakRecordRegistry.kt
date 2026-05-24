package cc.arccore.diagnostics.leak.tracking

import cc.arccore.diagnostics.leak.model.LeakReport
import cc.arccore.diagnostics.leak.model.LeakSeverity
import cc.arccore.diagnostics.leak.model.LeakType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class LeakRecordRegistry {

    private val records = ConcurrentHashMap<String, CopyOnWriteArrayList<LeakReport>>()

    fun record(report: LeakReport) {
        records.getOrPut(report.moduleId) { CopyOnWriteArrayList() }.add(report)
    }

    fun getForModule(moduleId: String): List<LeakReport> =
        records[moduleId]?.toList() ?: emptyList()

    fun getAll(): List<LeakReport> = records.values.flatten()

    fun getBySeverity(severity: LeakSeverity): List<LeakReport> =
        getAll().filter { it.severity == severity }

    fun getByType(type: LeakType): List<LeakReport> =
        getAll().filter { it.type == type }

    fun clearModule(moduleId: String) {
        records.remove(moduleId)
    }

    fun clear() {
        records.clear()
    }

    val totalCount: Int get() = records.values.sumOf { it.size }

    val affectedModules: Set<String> get() = records.keys.toSet()

    val hasCritical: Boolean get() = getBySeverity(LeakSeverity.CRITICAL).isNotEmpty()
}
