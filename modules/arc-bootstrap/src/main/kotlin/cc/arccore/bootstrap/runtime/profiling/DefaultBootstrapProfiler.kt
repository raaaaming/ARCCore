package cc.arccore.bootstrap.runtime.profiling

import cc.arccore.bootstrap.runtime.BootstrapPhase
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class DefaultBootstrapProfiler : BootstrapProfiler {

    private data class ActiveEntry(
        val phase: BootstrapPhase,
        val startNanos: Long
    )

    private class ModuleSession(
        val moduleId: String,
        val bootstrapStartNanos: Long,
        val completedEntries: CopyOnWriteArrayList<PhaseTimingEntry> = CopyOnWriteArrayList(),
        val activeEntry: AtomicReference<ActiveEntry?> = AtomicReference(null)
    )

    private val sessions: ConcurrentHashMap<String, ModuleSession> = ConcurrentHashMap()
    private val completed: ConcurrentHashMap<String, BootstrapProfilingData> = ConcurrentHashMap()

    override fun startBootstrap(moduleId: String) {
        sessions[moduleId] = ModuleSession(
            moduleId = moduleId,
            bootstrapStartNanos = System.nanoTime()
        )
    }

    override fun startPhase(moduleId: String, phase: BootstrapPhase) {
        val session = sessions[moduleId] ?: return
        session.activeEntry.set(ActiveEntry(phase = phase, startNanos = System.nanoTime()))
    }

    override fun endPhase(moduleId: String, phase: BootstrapPhase, success: Boolean, notes: List<String>) {
        val session = sessions[moduleId] ?: return
        val active = session.activeEntry.get() ?: return
        if (active.phase != phase) return

        val endNanos = System.nanoTime()
        val entry = PhaseTimingEntry(
            phase = phase,
            startNanos = active.startNanos,
            endNanos = endNanos,
            success = success,
            notes = notes
        )
        session.completedEntries.add(entry)
        session.activeEntry.set(null)
    }

    override fun endBootstrap(moduleId: String): BootstrapProfilingData {
        val session = sessions.remove(moduleId)
            ?: return BootstrapProfilingData(
                moduleId = moduleId,
                entries = emptyList(),
                totalStartNanos = System.nanoTime(),
                totalEndNanos = System.nanoTime()
            )

        val data = BootstrapProfilingData(
            moduleId = moduleId,
            entries = session.completedEntries.toList(),
            totalStartNanos = session.bootstrapStartNanos,
            totalEndNanos = System.nanoTime()
        )
        completed[moduleId] = data
        return data
    }

    override fun getProfilingData(moduleId: String): BootstrapProfilingData? = completed[moduleId]
}
