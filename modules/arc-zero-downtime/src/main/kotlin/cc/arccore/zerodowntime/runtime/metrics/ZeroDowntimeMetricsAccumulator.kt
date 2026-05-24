package cc.arccore.zerodowntime.runtime.metrics

import cc.arccore.zerodowntime.runtime.model.ZeroDowntimeMetrics
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal class ZeroDowntimeMetricsAccumulator {
    private val totalTransitions = AtomicLong(0)
    private val successful = AtomicLong(0)
    private val failed = AtomicLong(0)
    private val rolledBack = AtomicLong(0)
    private val totalDrainMs = AtomicLong(0)
    private val totalTransitionMs = AtomicLong(0)
    private val totalOwnershipTransfers = AtomicLong(0)
    private val lastTransitionAt = AtomicReference<Instant?>(null)

    fun recordSuccess(transitionMs: Long, drainMs: Long, ownershipTransferCount: Long = 0) {
        totalTransitions.incrementAndGet()
        successful.incrementAndGet()
        totalTransitionMs.addAndGet(transitionMs)
        totalDrainMs.addAndGet(drainMs)
        totalOwnershipTransfers.addAndGet(ownershipTransferCount)
        lastTransitionAt.set(Instant.now())
    }

    fun recordFailure(wasRolledBack: Boolean) {
        totalTransitions.incrementAndGet()
        failed.incrementAndGet()
        if (wasRolledBack) rolledBack.incrementAndGet()
        lastTransitionAt.set(Instant.now())
    }

    fun snapshot(): ZeroDowntimeMetrics {
        val total = totalTransitions.get()
        val successCount = successful.get()
        val avgTransition = if (successCount > 0) totalTransitionMs.get().toDouble() / successCount else 0.0
        val avgDrain = if (successCount > 0) totalDrainMs.get().toDouble() / successCount else 0.0

        return ZeroDowntimeMetrics(
            totalTransitions = total,
            successfulTransitions = successCount,
            failedTransitions = failed.get(),
            rolledBackTransitions = rolledBack.get(),
            averageTransitionMs = avgTransition,
            averageDrainMs = avgDrain,
            totalOwnershipTransfers = totalOwnershipTransfers.get(),
            lastTransitionAt = lastTransitionAt.get()
        )
    }
}
