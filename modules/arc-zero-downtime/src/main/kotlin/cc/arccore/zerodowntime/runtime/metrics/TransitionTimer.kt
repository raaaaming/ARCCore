package cc.arccore.zerodowntime.runtime.metrics

import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase
import java.util.concurrent.ConcurrentHashMap

internal class TransitionTimer {
    private val stageTimes = ConcurrentHashMap<ZeroDowntimePhase, Long>()
    private val stageStartTimes = ConcurrentHashMap<ZeroDowntimePhase, Long>()
    private val totalStartTime = System.currentTimeMillis()

    fun startStage(phase: ZeroDowntimePhase) {
        stageStartTimes[phase] = System.currentTimeMillis()
    }

    fun endStage(phase: ZeroDowntimePhase) {
        val start = stageStartTimes[phase] ?: return
        stageTimes[phase] = System.currentTimeMillis() - start
    }

    fun getStageDuration(phase: ZeroDowntimePhase): Long? = stageTimes[phase]

    fun getTotalDuration(): Long = System.currentTimeMillis() - totalStartTime

    fun toReport(): Map<ZeroDowntimePhase, Long> = stageTimes.toMap()
}
