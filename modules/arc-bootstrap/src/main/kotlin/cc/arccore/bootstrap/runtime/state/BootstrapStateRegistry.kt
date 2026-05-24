package cc.arccore.bootstrap.runtime.state

import cc.arccore.bootstrap.runtime.BootstrapPhase
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe registry tracking the current bootstrap phase and phase results
 * for each module being bootstrapped.
 */
class BootstrapStateRegistry {

    private val currentPhases: ConcurrentHashMap<String, BootstrapPhase> = ConcurrentHashMap()
    private val phaseResults: ConcurrentHashMap<String, CopyOnWriteArrayList<BootstrapPhaseResult>> = ConcurrentHashMap()
    private val finalResults: ConcurrentHashMap<String, BootstrapResult> = ConcurrentHashMap()

    fun recordPhaseStart(moduleId: String, phase: BootstrapPhase) {
        currentPhases[moduleId] = phase
        phaseResults.getOrPut(moduleId) { CopyOnWriteArrayList() }
    }

    fun recordPhaseResult(moduleId: String, result: BootstrapPhaseResult) {
        phaseResults.getOrPut(moduleId) { CopyOnWriteArrayList() }.add(result)
    }

    fun recordFinalResult(moduleId: String, result: BootstrapResult) {
        finalResults[moduleId] = result
        currentPhases.remove(moduleId)
    }

    fun currentPhaseOf(moduleId: String): BootstrapPhase? = currentPhases[moduleId]

    fun phaseResultsOf(moduleId: String): List<BootstrapPhaseResult> =
        phaseResults[moduleId]?.toList() ?: emptyList()

    fun finalResultOf(moduleId: String): BootstrapResult? = finalResults[moduleId]

    fun isBootstrapped(moduleId: String): Boolean = finalResults.containsKey(moduleId)

    fun isBootstrapping(moduleId: String): Boolean = currentPhases.containsKey(moduleId)

    fun allFinalResults(): Map<String, BootstrapResult> = finalResults.toMap()

    fun allModuleIds(): Set<String> =
        (currentPhases.keys + finalResults.keys + phaseResults.keys).toSet()

    fun clearModule(moduleId: String) {
        currentPhases.remove(moduleId)
        phaseResults.remove(moduleId)
        finalResults.remove(moduleId)
    }

    fun clear() {
        currentPhases.clear()
        phaseResults.clear()
        finalResults.clear()
    }

    fun summary(): String = buildString {
        appendLine("BootstrapStateRegistry summary:")
        appendLine("  Bootstrapping: ${currentPhases.keys}")
        appendLine("  Final results: ${finalResults.size} modules")
        finalResults.forEach { (id, result) ->
            appendLine("    $id -> ${result::class.simpleName}")
        }
    }
}
