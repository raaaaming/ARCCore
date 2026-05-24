package cc.arccore.zerodowntime.runtime.state

import cc.arccore.zerodowntime.runtime.model.TransitionContext
import java.util.concurrent.ConcurrentHashMap

internal class TransitionStateRegistry {
    private val activeTransitions = ConcurrentHashMap<String, TransitionContext>()

    fun begin(context: TransitionContext): Boolean {
        return activeTransitions.putIfAbsent(context.targetModuleId, context) == null
    }

    fun complete(moduleId: String) {
        activeTransitions.remove(moduleId)
    }

    fun getContext(moduleId: String): TransitionContext? = activeTransitions[moduleId]

    fun isTransitioning(moduleId: String): Boolean = activeTransitions.containsKey(moduleId)

    fun snapshot(moduleId: String): TransitionState? {
        val ctx = activeTransitions[moduleId] ?: return null
        return TransitionState(
            moduleId = ctx.targetModuleId,
            phase = ctx.phase,
            oldGeneration = ctx.oldHandle.generation,
            newGeneration = ctx.newHandle?.generation,
            elapsedMs = ctx.elapsedMs,
            canAbort = ctx.rollbackAvailable
        )
    }

    fun allSnapshots(): List<TransitionState> {
        return activeTransitions.values.mapNotNull { snapshot(it.targetModuleId) }
    }

    fun allActiveModuleIds(): Set<String> = activeTransitions.keys.toSet()
}
