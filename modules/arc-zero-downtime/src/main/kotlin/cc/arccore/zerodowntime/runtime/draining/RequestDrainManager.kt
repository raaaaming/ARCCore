package cc.arccore.zerodowntime.runtime.draining

import cc.arccore.zerodowntime.runtime.model.DrainRecord

data class DrainConfig(
    val maxWaitMs: Long = 10_000L,
    val pollIntervalMs: Long = 50L,
    val forceAfterMs: Long = 8_000L,
    val strategy: DrainStrategy = DrainStrategy.GRACEFUL_THEN_FORCE
)

enum class DrainStrategy {
    GRACEFUL,
    GRACEFUL_THEN_FORCE,
    IMMEDIATE_FORCE
}

sealed class DrainResult {
    data class Completed(val remainingTasks: Int, val durationMs: Long) : DrainResult()
    data class ForceDrained(val abortedTasks: Int, val durationMs: Long) : DrainResult()
    data class TimedOut(val remainingTasks: Int, val durationMs: Long) : DrainResult()
}

internal class RequestDrainManager(
    private val config: DrainConfig = DrainConfig()
) {
    fun drain(
        moduleId: String,
        counter: InflightCounter,
        record: DrainRecord
    ): DrainResult {
        record.drainStartMs = System.currentTimeMillis()
        record.inflightTasksAtStart = counter.currentCount()

        if (config.strategy == DrainStrategy.IMMEDIATE_FORCE) {
            record.forceDrained = true
            record.drainEndMs = System.currentTimeMillis()
            return DrainResult.ForceDrained(record.inflightTasksAtStart, record.drainDurationMs)
        }

        val deadline = record.drainStartMs + config.maxWaitMs
        val forceDeadline = record.drainStartMs + config.forceAfterMs

        while (System.currentTimeMillis() < deadline) {
            val current = counter.currentCount()
            if (current == 0) {
                record.drainEndMs = System.currentTimeMillis()
                return DrainResult.Completed(0, record.drainDurationMs)
            }

            if (config.strategy == DrainStrategy.GRACEFUL_THEN_FORCE &&
                System.currentTimeMillis() > forceDeadline) {
                record.forceDrained = true
                record.drainEndMs = System.currentTimeMillis()
                return DrainResult.ForceDrained(current, record.drainDurationMs)
            }

            Thread.sleep(config.pollIntervalMs)
        }

        record.drainEndMs = System.currentTimeMillis()
        return DrainResult.TimedOut(counter.currentCount(), record.drainDurationMs)
    }
}
