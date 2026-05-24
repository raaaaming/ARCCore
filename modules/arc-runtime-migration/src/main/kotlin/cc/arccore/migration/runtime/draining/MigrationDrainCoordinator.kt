package cc.arccore.migration.runtime.draining

import cc.arccore.migration.runtime.model.MigrationContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class MigrationDrainCoordinator(
    private val gateRegistry: MigrationGateRegistry = MigrationGateRegistry(),
    private val drainTimeoutMs: Long = 30_000L,
    private val drainPollMs: Long = 50L
) {
    private val inflightCounters = ConcurrentHashMap<String, AtomicInteger>()
    private val draining = ConcurrentHashMap.newKeySet<String>()

    fun beginDrain(context: MigrationContext): DrainOutcome {
        val moduleId = context.moduleId
        val gate = gateRegistry.getOrCreate(moduleId)
        gate.set(false)
        draining.add(moduleId)

        context.drainRecord.drainStartMs = System.currentTimeMillis()
        val inflightCounter = inflightCounters.getOrPut(moduleId) { AtomicInteger(0) }
        val inflightAtStart = inflightCounter.get()
        context.drainRecord.inflightAtStart = inflightAtStart

        val deadline = System.currentTimeMillis() + drainTimeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (inflightCounter.get() == 0) {
                context.drainRecord.drainEndMs = System.currentTimeMillis()
                val duration = context.drainRecord.drainEndMs - context.drainRecord.drainStartMs
                return DrainOutcome.Completed(inflightAtStart, duration)
            }
            Thread.sleep(drainPollMs)
        }

        val remaining = inflightCounter.get()
        return DrainOutcome.TimedOut(remaining)
    }

    fun releaseDrain(context: MigrationContext) {
        releaseDrain(context.moduleId)
    }

    fun releaseDrain(moduleId: String) {
        draining.remove(moduleId)
        gateRegistry.getOrCreate(moduleId).set(true)
    }

    fun forceReleaseDrain(moduleId: String) {
        draining.remove(moduleId)
        gateRegistry.getOrCreate(moduleId).set(true)
        inflightCounters.remove(moduleId)
    }

    fun isDraining(moduleId: String): Boolean = draining.contains(moduleId)

    fun getOrCreateGate(moduleId: String): AtomicBoolean = gateRegistry.getOrCreate(moduleId)

    sealed class DrainOutcome {
        data class Completed(val inflightAtStart: Int, val drainDurationMs: Long) : DrainOutcome()
        data class TimedOut(val remainingInflight: Int) : DrainOutcome()
        data class Forced(val remainingInflight: Int) : DrainOutcome()
    }
}
