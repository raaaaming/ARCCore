package cc.arccore.eventbus.runtime.propagation

import cc.arccore.eventbus.runtime.dispatch.DispatchResult
import cc.arccore.eventbus.runtime.event.CancellableInternalEvent
import cc.arccore.eventbus.runtime.event.InternalEvent
import cc.arccore.eventbus.runtime.exception.EventPropagationException
import cc.arccore.eventbus.runtime.subscription.EventSubscription

/**
 * Executes event propagation across all phases and priorities.
 *
 * Phase execution order: PRE_PROCESS(-200) → NORMAL(0) → POST_PROCESS(200) → MONITOR(MAX_VALUE)
 * Within each phase, listeners are sorted by [EventPriority.order] ascending.
 *
 * Rules:
 * - Cancellation stops further non-MONITOR phase execution.
 * - MONITOR phase always executes (read-only observation).
 * - Calling [CancellableInternalEvent.cancel] or [CancellableInternalEvent.uncancel]
 *   from a MONITOR phase listener throws [EventPropagationException].
 * - Listener exceptions are caught and collected; they do NOT stop propagation.
 *
 * The [listenerExecutor] parameter decouples the pipeline from any specific threading strategy.
 * [SyncEventDispatcher] injects a `runBlocking`-based executor; [CoroutineEventDispatcher]
 * injects a native suspend executor.
 */
class PropagationPipeline(
    /**
     * Executes a single listener invocation synchronously to completion.
     *
     * Receives a `suspend () -> Unit` block and must execute it before returning.
     * The executor is provided by the caller ([SyncEventDispatcher] uses `runBlocking`,
     * [CoroutineEventDispatcher] provides its own coroutine-aware executor).
     *
     * There is intentionally no default — callers must always supply an executor.
     * The [SyncEventDispatcher] and [CoroutineEventDispatcher] do this automatically.
     */
    private val listenerExecutor: (suspend () -> Unit) -> Unit
) {

    fun <T : InternalEvent> execute(context: PropagationContext<T>): DispatchResult {
        for (phase in PropagationPhase.sortedByOrder) {
            context.currentPhase = phase
            val isMonitorPhase = phase == PropagationPhase.MONITOR

            val phaseSubscriptions = context.subscriptions
                .filter { it.phase == phase && it.active }
                .sortedBy { it.priority.order }

            for (subscription in phaseSubscriptions) {
                // Stopped propagation: skip non-MONITOR phases
                if (context.stopped && !isMonitorPhase) break

                // Cancellation: mark stopped but continue to MONITOR
                if (!isMonitorPhase) {
                    val event = context.envelope.event
                    if (event is CancellableInternalEvent && event.isCancelled) {
                        context.stopped = true
                        break
                    }
                }

                context.invokedCount++

                try {
                    invokeListener(subscription, context, isMonitorPhase)
                    context.successCount++
                } catch (e: EventPropagationException) {
                    context.errors.add(subscription.moduleId to e)
                } catch (e: Exception) {
                    context.errors.add(subscription.moduleId to e)
                }
            }
        }

        return buildDispatchResult(context)
    }

    private fun <T : InternalEvent> invokeListener(
        subscription: EventSubscription<T>,
        context: PropagationContext<T>,
        isMonitorPhase: Boolean
    ) {
        val event = context.envelope.event
        val cancelledBefore = if (isMonitorPhase && event is CancellableInternalEvent) {
            event.isCancelled
        } else null

        listenerExecutor { subscription.listener.handle(event) }

        // Detect if MONITOR listener illegally changed cancellation state
        if (isMonitorPhase && event is CancellableInternalEvent && cancelledBefore != null) {
            if (event.isCancelled != cancelledBefore) {
                throw EventPropagationException(
                    "MONITOR phase listener in module '${subscription.moduleId}' " +
                        "illegally modified cancellation state of event '${context.envelope.eventName}'. " +
                        "MONITOR phase is read-only."
                )
            }
        }
    }

    private fun <T : InternalEvent> buildDispatchResult(context: PropagationContext<T>): DispatchResult {
        val wasCancelled = context.envelope.event.let { it is CancellableInternalEvent && it.isCancelled }
        return if (context.errors.isEmpty()) {
            DispatchResult.Success(
                dispatchId = context.envelope.dispatchId,
                invokedCount = context.invokedCount,
                wasCancelled = wasCancelled
            )
        } else {
            DispatchResult.PartialFailure(
                dispatchId = context.envelope.dispatchId,
                invokedCount = context.invokedCount,
                successCount = context.successCount,
                errors = context.errors.toList(),
                wasCancelled = wasCancelled
            )
        }
    }
}
