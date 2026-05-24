package cc.arccore.scheduler.runtime.cancellation

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class CancellationRecord(
    val taskId: String,
    val moduleId: String,
    val reason: CancellationReason,
    val cancelledAt: Instant = Instant.now()
)

internal class CancellationTracker(private val maxHistory: Int = 200) {
    private val records = ConcurrentHashMap<String, CopyOnWriteArrayList<CancellationRecord>>()

    fun record(taskId: String, moduleId: String, reason: CancellationReason) {
        val moduleRecords = records.computeIfAbsent(moduleId) { CopyOnWriteArrayList() }
        moduleRecords.add(CancellationRecord(taskId, moduleId, reason))
        while (moduleRecords.size > maxHistory) moduleRecords.removeAt(0)
    }

    fun getHistory(moduleId: String): List<CancellationRecord> =
        records[moduleId]?.toList() ?: emptyList()

    fun countByReason(moduleId: String, reason: CancellationReason): Int =
        getHistory(moduleId).count { it.reason == reason }

    fun totalCancelled(moduleId: String): Int = getHistory(moduleId).size
}
