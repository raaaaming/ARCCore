package cc.arccore.eventbus.runtime

import cc.arccore.eventbus.runtime.diagnostics.DefaultEventBusDiagnostics
import cc.arccore.eventbus.runtime.diagnostics.EventBusDiagnostics
import cc.arccore.eventbus.runtime.dispatch.AsyncEventDispatcher
import cc.arccore.eventbus.runtime.dispatch.DispatchResult
import cc.arccore.eventbus.runtime.dispatch.SyncEventDispatcher
import cc.arccore.eventbus.runtime.event.EventEnvelope
import cc.arccore.eventbus.runtime.event.EventPriority
import cc.arccore.eventbus.runtime.event.InternalEvent
import cc.arccore.eventbus.runtime.exception.EventBusException
import cc.arccore.eventbus.runtime.exception.InvalidSubscriptionException
import cc.arccore.eventbus.runtime.integration.DiagnosticsEventBridgePort
import cc.arccore.eventbus.runtime.integration.DistributedEventBusPort
import cc.arccore.eventbus.runtime.integration.NoopDiagnosticsEventBridgePort
import cc.arccore.eventbus.runtime.lifecycle.EventBusLifecycleHook
import cc.arccore.eventbus.runtime.ownership.OwnershipRegistry
import cc.arccore.eventbus.runtime.propagation.PropagationPhase
import cc.arccore.eventbus.runtime.subscription.EventSubscription
import cc.arccore.eventbus.runtime.subscription.InternalEventListener
import cc.arccore.eventbus.runtime.subscription.SubscriptionHandle
import cc.arccore.eventbus.runtime.subscription.SubscriptionRegistry
import cc.arccore.eventbus.runtime.validation.SubscriptionValidator
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

/**
 * Default implementation of [InternalEventBus].
 *
 * ## Thread safety
 * All public methods are thread-safe. Subscription registration and cancellation
 * use [ConcurrentHashMap] + [CopyOnWriteArrayList] for lock-free dispatch iteration.
 *
 * ## Coroutine integration
 * Inject [asyncDispatchStrategy] to enable [publishAsync]. The strategy is a lambda
 * that accepts a `suspend () -> Unit` block and schedules it on a coroutine.
 * Example (using arc-coroutine):
 * ```kotlin
 * DefaultInternalEventBus(
 *     asyncDispatchStrategy = { block -> coroutineRuntime.launch { block() } }
 * )
 * ```
 *
 * ## Diagnostics
 * Inject [diagnosticsBridge] to receive dispatch telemetry in an external system.
 * The internal [EventBusDiagnostics] is always active and accessible via [diagnostics].
 *
 * @param asyncDispatchStrategy Optional coroutine dispatch strategy. Required for [publishAsync].
 * @param diagnosticsBridge Bridge to an external diagnostics system. Defaults to no-op.
 * @param distributedPort Optional distributed event bus integration. Null by default.
 * @param lifecycleHook Optional lifecycle callback hook.
 * @param publisherModuleId Optional default publisher module ID for events without explicit origin.
 */
class DefaultInternalEventBus(
    private val asyncDispatchStrategy: ((suspend () -> Unit) -> Unit)? = null,
    private val diagnosticsBridge: DiagnosticsEventBridgePort = NoopDiagnosticsEventBridgePort(),
    private val distributedPort: DistributedEventBusPort? = null,
    private val lifecycleHook: EventBusLifecycleHook? = null,
    private val publisherModuleId: String? = null
) : InternalEventBus {

    private val registry = SubscriptionRegistry()
    private val ownership = OwnershipRegistry()
    private val syncDispatcher = SyncEventDispatcher()
    private val asyncDispatcher = AsyncEventDispatcher(asyncDispatchStrategy)
    private val isShutdownFlag = AtomicBoolean(false)
    private val totalDispatched = AtomicLong(0L)
    private val totalFailed = AtomicLong(0L)

    /** Internal diagnostics — always active. */
    val diagnostics: EventBusDiagnostics = DefaultEventBusDiagnostics(
        registry = registry,
        ownership = ownership,
        busIsShutdown = { isShutdownFlag.get() },
        totalDispatched = { totalDispatched.get() },
        totalFailed = { totalFailed.get() }
    )

    init {
        lifecycleHook?.onStartup()
    }

    override fun <T : InternalEvent> subscribe(
        eventType: KClass<T>,
        moduleId: String,
        phase: PropagationPhase,
        priority: EventPriority,
        listener: InternalEventListener<T>
    ): SubscriptionHandle {
        checkNotShutdown("subscribe")

        val validationResult = SubscriptionValidator.validate(eventType, moduleId, phase, priority, listener)
        if (!validationResult.isValid) {
            throw InvalidSubscriptionException(
                moduleId,
                validationResult.errors.joinToString("; ")
            )
        }

        val subscription = EventSubscription(
            eventType = eventType,
            moduleId = moduleId,
            phase = phase,
            priority = priority,
            listener = listener
        )

        val handle = registry.register(subscription)
        ownership.register(moduleId, subscription.subscriptionId)
        return handle
    }

    override fun <T : InternalEvent> publish(event: T): DispatchResult {
        checkNotShutdown("publish")

        val envelope = EventEnvelope(
            event = event,
            publisherModuleId = publisherModuleId,
            async = false
        )

        val subscriptions = registry.getActiveSubscriptions(event::class as KClass<T>)
        totalDispatched.incrementAndGet()

        val result = syncDispatcher.dispatch(envelope, subscriptions)

        if (!result.isSuccess) totalFailed.incrementAndGet()

        diagnostics.recordDispatch(envelope, result)
        diagnosticsBridge.onDispatched(envelope, result)
        distributedPort?.onLocalDispatch(event, publisherModuleId)

        return result
    }

    override suspend fun <T : InternalEvent> publishAsync(event: T): DispatchResult {
        checkNotShutdown("publishAsync")

        if (asyncDispatchStrategy == null) {
            throw EventBusException(
                "Async dispatch requires a coroutine strategy. " +
                    "Provide asyncDispatchStrategy when constructing DefaultInternalEventBus."
            )
        }

        val envelope = EventEnvelope(
            event = event,
            publisherModuleId = publisherModuleId,
            async = true
        )

        val subscriptions = registry.getActiveSubscriptions(event::class as KClass<T>)
        totalDispatched.incrementAndGet()

        val result = asyncDispatcher.dispatch(envelope, subscriptions)

        if (!result.isSuccess) totalFailed.incrementAndGet()

        diagnostics.recordDispatch(envelope, result)
        diagnosticsBridge.onDispatched(envelope, result)
        distributedPort?.onLocalDispatch(event, publisherModuleId)

        return result
    }

    override fun unsubscribeAll(moduleId: String): Int {
        checkNotShutdown("unsubscribeAll")
        ownership.cancelAll(moduleId)
        return registry.cancelAllForModule(moduleId)
    }

    override fun isShutdown(): Boolean = isShutdownFlag.get()

    override fun shutdown() {
        if (!isShutdownFlag.compareAndSet(false, true)) return

        lifecycleHook?.onShutdown()

        // Cancel all subscriptions
        for (moduleId in ownership.registeredModules()) {
            registry.cancelAllForModule(moduleId)
        }
        registry.purgeInactive()

        diagnosticsBridge.onShutdown()
        distributedPort?.onShutdown()

        lifecycleHook?.onShutdownComplete()
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private fun checkNotShutdown(operation: String) {
        if (isShutdownFlag.get()) {
            throw EventBusException(
                "Cannot perform '$operation' on a shut-down InternalEventBus. " +
                    "The bus has already been shut down."
            )
        }
    }
}
