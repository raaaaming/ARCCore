package cc.arccore.bootstrap.runtime

import cc.arccore.bootstrap.runtime.exception.BootstrapPhaseException
import cc.arccore.bootstrap.runtime.exception.BootstrapValidationException
import cc.arccore.bootstrap.runtime.optimization.BootstrapOptimizationHint
import cc.arccore.bootstrap.runtime.optimization.BootstrapOptimizer
import cc.arccore.bootstrap.runtime.profiling.BootstrapProfiler
import cc.arccore.bootstrap.runtime.scheduling.BootstrapPhaseHandler
import cc.arccore.bootstrap.runtime.state.BootstrapPhaseResult
import cc.arccore.bootstrap.runtime.state.BootstrapResult
import cc.arccore.bootstrap.runtime.state.BootstrapStateRegistry
import cc.arccore.bootstrap.runtime.validation.BootstrapValidator
import java.util.logging.Logger

/**
 * Default pipeline implementation.
 *
 * Execution contract per phase:
 * 1. Check optimization hints — if a SkipPhase hint exists, record Skipped and advance
 * 2. Run [BootstrapValidator.validatePreCondition] — failure aborts the pipeline
 * 3. Run [BootstrapProfiler.startPhase]
 * 4. Invoke [BootstrapPhaseHandler.handle]
 * 5. Run [BootstrapProfiler.endPhase]
 * 6. On handler failure → [BootstrapResult.Failure] immediately
 * 7. Run [BootstrapValidator.validatePostCondition] — failure aborts the pipeline
 * 8. [BootstrapStateRegistry.recordPhaseResult]
 * 9. Advance to next phase; repeat
 *
 * [DefaultBootstrapPipeline] never mutates [ModuleContainer] directly —
 * module state transitions are left to [BootstrapLifecycleCoordinator].
 */
class DefaultBootstrapPipeline(
    private val handlers: List<BootstrapPhaseHandler>,
    private val profiler: BootstrapProfiler,
    private val validator: BootstrapValidator,
    private val stateRegistry: BootstrapStateRegistry,
    private val optimizer: BootstrapOptimizer = BootstrapOptimizer(),
    private val log: Logger = Logger.getLogger(DefaultBootstrapPipeline::class.java.name)
) : BootstrapPipeline {

    private val handlersByPhase: Map<BootstrapPhase, BootstrapPhaseHandler> =
        handlers.associateBy { it.phase }

    override fun execute(context: BootstrapContext): BootstrapResult {
        profiler.startBootstrap(context.moduleId)

        val completedPhases = mutableListOf<BootstrapPhase>()
        // Initial hints from context state before pipeline runs (e.g. hot-reload flags).
        // Additional hints that depend on PRELOADED_METADATA are re-evaluated per phase
        // so that METADATA_PRELOAD results are visible to later skip decisions.
        var currentContext = context

        // Iterate only active phases (excludes FAILED, SKIPPED terminal states)
        var phase: BootstrapPhase? = BootstrapPhase.DISCOVERY

        while (phase != null) {
            stateRegistry.recordPhaseStart(context.moduleId, phase)
            currentContext = currentContext.withPhase(phase)

            // --- Optimization: re-evaluate hints per phase so PRELOADED_METADATA is visible ---
            val optimizationHints = optimizer.computeHints(currentContext)
            val skipHint = optimizationHints
                .filterIsInstance<BootstrapOptimizationHint.SkipPhase>()
                .firstOrNull { it.phase == phase }

            if (skipHint != null) {
                val skippedResult = BootstrapPhaseResult.Skipped(
                    phase = phase,
                    reason = skipHint.reason
                )
                stateRegistry.recordPhaseResult(context.moduleId, skippedResult)
                log.fine("[ARCCore] Phase $phase SKIPPED for '${context.moduleId}'")
                phase = phase.next()
                continue
            }

            // --- Pre-condition validation ---
            val preCheck = validator.validatePreCondition(currentContext, phase)
            if (preCheck.isFail) {
                val fail = preCheck as cc.arccore.bootstrap.runtime.validation.ValidationResult.Fail
                val exception = BootstrapValidationException(phase = phase, reason = fail.reason, cause = fail.cause)
                val failResult = buildFailureResult(context, phase, completedPhases, exception)
                stateRegistry.recordFinalResult(context.moduleId, failResult)
                val profilingData = profiler.endBootstrap(context.moduleId)
                return failResult.copy(profilingData = profilingData)
            }

            // --- Phase handler lookup ---
            val handler = handlersByPhase[phase]
            if (handler == null) {
                // No handler registered for this phase — treat as no-op success
                val noOpResult = BootstrapPhaseResult.Success(
                    phase = phase,
                    durationNanos = 0L,
                    notes = listOf("no handler registered for phase $phase")
                )
                stateRegistry.recordPhaseResult(context.moduleId, noOpResult)
                completedPhases.add(phase)
                phase = phase.next()
                continue
            }

            // --- Profiler: start ---
            profiler.startPhase(context.moduleId, phase)

            // --- Execute handler ---
            val phaseResult: BootstrapPhaseResult = try {
                handler.handle(currentContext)
            } catch (e: Exception) {
                val wrappedException = BootstrapPhaseException(
                    phase = phase,
                    moduleId = context.moduleId,
                    message = "handler threw uncaught exception: ${e.message}",
                    cause = e
                )
                profiler.endPhase(context.moduleId, phase, success = false, notes = listOf(e.message ?: ""))
                val failResult = buildFailureResult(context, phase, completedPhases, wrappedException)
                stateRegistry.recordPhaseResult(context.moduleId, BootstrapPhaseResult.Failure(phase, 0L, wrappedException))
                stateRegistry.recordFinalResult(context.moduleId, failResult)
                val profilingData = profiler.endBootstrap(context.moduleId)
                return failResult.copy(profilingData = profilingData)
            }

            // --- Profiler: end ---
            profiler.endPhase(
                moduleId = context.moduleId,
                phase = phase,
                success = phaseResult.isSuccess,
                notes = when (phaseResult) {
                    is BootstrapPhaseResult.Success -> phaseResult.notes
                    is BootstrapPhaseResult.Failure -> phaseResult.notes
                    is BootstrapPhaseResult.Skipped -> listOf(phaseResult.reason)
                }
            )

            stateRegistry.recordPhaseResult(context.moduleId, phaseResult)

            // --- Handle phase failure ---
            if (phaseResult is BootstrapPhaseResult.Failure) {
                val exception = BootstrapPhaseException(
                    phase = phase,
                    moduleId = context.moduleId,
                    message = phaseResult.cause.message ?: "phase failed",
                    cause = phaseResult.cause
                )
                val failResult = buildFailureResult(context, phase, completedPhases, exception)
                stateRegistry.recordFinalResult(context.moduleId, failResult)
                val profilingData = profiler.endBootstrap(context.moduleId)
                log.warning(
                    "[ARCCore] Bootstrap FAILED at phase $phase for '${context.moduleId}': ${phaseResult.cause.message}"
                )
                return failResult.copy(profilingData = profilingData)
            }

            // --- Post-condition validation ---
            val postCheck = validator.validatePostCondition(currentContext, phase)
            if (postCheck.isFail) {
                val fail = postCheck as cc.arccore.bootstrap.runtime.validation.ValidationResult.Fail
                val exception = BootstrapValidationException(phase = phase, reason = fail.reason, cause = fail.cause)
                val failResult = buildFailureResult(context, phase, completedPhases, exception)
                stateRegistry.recordFinalResult(context.moduleId, failResult)
                val profilingData = profiler.endBootstrap(context.moduleId)
                return failResult.copy(profilingData = profilingData)
            }

            completedPhases.add(phase)
            log.fine("[ARCCore] Phase $phase OK for '${context.moduleId}'")
            phase = phase.next()
        }

        val profilingData = profiler.endBootstrap(context.moduleId)
        val successResult = BootstrapResult.Success(
            moduleId = context.moduleId,
            completedPhases = completedPhases,
            profilingData = profilingData
        )
        stateRegistry.recordFinalResult(context.moduleId, successResult)
        log.fine(
            "[ARCCore] Bootstrap SUCCEEDED for '${context.moduleId}' — " +
                "phases: $completedPhases, total: ${String.format("%.2f", profilingData.totalDurationMs)}ms"
        )
        return successResult
    }

    private fun buildFailureResult(
        context: BootstrapContext,
        failedPhase: BootstrapPhase,
        completedPhases: List<BootstrapPhase>,
        cause: Throwable
    ): BootstrapResult.Failure = BootstrapResult.Failure(
        moduleId = context.moduleId,
        failedPhase = failedPhase,
        cause = cause,
        completedPhases = completedPhases.toList()
    )
}
