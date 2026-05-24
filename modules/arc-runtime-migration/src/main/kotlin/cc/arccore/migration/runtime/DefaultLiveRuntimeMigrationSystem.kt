package cc.arccore.migration.runtime

import cc.arccore.migration.runtime.cleanup.MigrationCleanupManager
import cc.arccore.migration.runtime.cleanup.MigrationSnapshotCleaner
import cc.arccore.migration.runtime.coordination.MigrationGenerationCounter
import cc.arccore.migration.runtime.coordination.MigrationNodeRegistry
import cc.arccore.migration.runtime.coordination.MigrationRoutingSwitch
import cc.arccore.migration.runtime.diagnostics.MigrationDiagnosticsCollector
import cc.arccore.migration.runtime.draining.MigrationDrainCoordinator
import cc.arccore.migration.runtime.exception.MigrationAlreadyActiveException
import cc.arccore.migration.runtime.integration.SnapshotMigrationIntegration
import cc.arccore.migration.runtime.lifecycle.MigrationLifecycleEvent
import cc.arccore.migration.runtime.lifecycle.MigrationLifecycleObserver
import cc.arccore.migration.runtime.migration.BeginDrainingStage
import cc.arccore.migration.runtime.migration.BootstrapTargetStage
import cc.arccore.migration.runtime.migration.CleanupSourceStage
import cc.arccore.migration.runtime.migration.FinalizeMigrationStage
import cc.arccore.migration.runtime.migration.MigrationPipeline
import cc.arccore.migration.runtime.migration.PrepareMigrationStage
import cc.arccore.migration.runtime.migration.RestoreStateStage
import cc.arccore.migration.runtime.migration.SnapshotStateStage
import cc.arccore.migration.runtime.migration.SwitchRoutingStage
import cc.arccore.migration.runtime.migration.TransferOwnershipStage
import cc.arccore.migration.runtime.migration.ValidateTargetStage
import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationId
import cc.arccore.migration.runtime.model.MigrationMetrics
import cc.arccore.migration.runtime.model.MigrationPhase
import cc.arccore.migration.runtime.model.MigrationResult
import cc.arccore.migration.runtime.model.NodeDescriptor
import cc.arccore.migration.runtime.ownership.RuntimeOwnershipRelocationCoordinator
import cc.arccore.migration.runtime.rollback.MigrationRollbackManager
import cc.arccore.migration.runtime.state.MigrationSession
import cc.arccore.migration.runtime.state.MigrationSessionRegistry
import cc.arccore.migration.runtime.synchronization.MigrationTransitionLock
import cc.arccore.migration.runtime.transfer.TargetNodeBootstrapper
import cc.arccore.migration.runtime.transport.RuntimeRelocationTransport
import cc.arccore.migration.runtime.validation.MigrationTargetValidator
import java.util.concurrent.CopyOnWriteArrayList

class DefaultLiveRuntimeMigrationSystem internal constructor(
    private val transport: RuntimeRelocationTransport,
    private val sessionRegistry: MigrationSessionRegistry,
    private val nodeRegistry: MigrationNodeRegistry,
    private val transitionLock: MigrationTransitionLock,
    private val diagnosticsCollector: MigrationDiagnosticsCollector,
    private val snapshotIntegration: SnapshotMigrationIntegration
) : LiveRuntimeMigrationSystem {

    constructor(transport: RuntimeRelocationTransport) : this(
        transport = transport,
        sessionRegistry = MigrationSessionRegistry(),
        nodeRegistry = MigrationNodeRegistry(),
        transitionLock = MigrationTransitionLock(),
        diagnosticsCollector = MigrationDiagnosticsCollector(),
        snapshotIntegration = SnapshotMigrationIntegration()
    )
    private val observers = CopyOnWriteArrayList<MigrationLifecycleObserver>()
    private val generationCounter = MigrationGenerationCounter()
    private val ownershipCoordinator = RuntimeOwnershipRelocationCoordinator()
    private val drainCoordinator = MigrationDrainCoordinator()
    private val routingSwitch = MigrationRoutingSwitch(generationCounter)
    private val cleanupManager = MigrationCleanupManager(MigrationSnapshotCleaner(), drainCoordinator)
    private val targetValidator = MigrationTargetValidator(nodeRegistry, transport)
    private val targetBootstrapper = TargetNodeBootstrapper(transport)
    private val rollbackManager = MigrationRollbackManager(drainCoordinator, ownershipCoordinator)

    override fun migrate(sourceNodeId: String, targetNodeId: String, moduleId: String): MigrationResult {
        if (!transitionLock.tryLock(moduleId)) {
            return MigrationResult.Failure(
                MigrationId.generate(moduleId), moduleId, MigrationPhase.IDLE,
                MigrationAlreadyActiveException(moduleId), false
            )
        }
        val context = MigrationContext(MigrationId.generate(moduleId), moduleId, sourceNodeId, targetNodeId)
        return try {
            if (!sessionRegistry.begin(context)) {
                return MigrationResult.Failure(
                    context.migrationId, moduleId, MigrationPhase.IDLE,
                    MigrationAlreadyActiveException(moduleId), false
                )
            }
            val pipeline = buildPipeline(context)
            notify(MigrationLifecycleEvent.MigrationStarted(context.migrationId, moduleId, sourceNodeId, targetNodeId))
            pipeline.execute(context)
        } finally {
            transitionLock.unlock(moduleId)
        }
    }

    private fun buildPipeline(@Suppress("UNUSED_PARAMETER") context: MigrationContext): MigrationPipeline {
        val pipelineObservers = buildPipelineObservers()
        val stages = listOf(
            PrepareMigrationStage(sessionRegistry, nodeRegistry),
            ValidateTargetStage(targetValidator, transport),
            BeginDrainingStage(drainCoordinator),
            SnapshotStateStage(snapshotIntegration),
            TransferOwnershipStage(ownershipCoordinator),
            BootstrapTargetStage(transport, targetBootstrapper),
            RestoreStateStage(snapshotIntegration),
            SwitchRoutingStage(routingSwitch),
            FinalizeMigrationStage(ownershipCoordinator, sessionRegistry),
            CleanupSourceStage(cleanupManager)
        )
        return MigrationPipeline(stages, rollbackManager, pipelineObservers)
    }

    private fun buildPipelineObservers(): List<cc.arccore.migration.runtime.migration.MigrationLifecycleObserver> {
        val allLifecycleObservers = observers + listOf(diagnosticsCollector as MigrationLifecycleObserver)
        return allLifecycleObservers.map { lifecycleObserver ->
            object : cc.arccore.migration.runtime.migration.MigrationLifecycleObserver {
                override fun onMigrationCompleted(result: MigrationResult) {
                    val event = when (result) {
                        is MigrationResult.Success -> MigrationLifecycleEvent.MigrationCompleted(
                            result.migrationId, result.moduleId, result.totalDurationMs
                        )
                        is MigrationResult.Failure -> MigrationLifecycleEvent.MigrationFailed(
                            result.migrationId, result.moduleId, result.error, result.phase
                        )
                        is MigrationResult.Aborted -> MigrationLifecycleEvent.MigrationAborted(
                            result.migrationId, result.moduleId, result.phase, result.reason
                        )
                    }
                    try { lifecycleObserver.onMigrationEvent(event) } catch (_: Exception) {}
                }

                override fun onRollbackCompleted(context: MigrationContext, success: Boolean) {
                    val event = MigrationLifecycleEvent.MigrationRolledBack(
                        context.migrationId, context.moduleId, context.phase, success
                    )
                    try { lifecycleObserver.onMigrationEvent(event) } catch (_: Exception) {}
                }
            }
        }
    }

    override fun migrateAll(sourceNodeId: String, targetNodeId: String, moduleIds: List<String>): Map<String, MigrationResult> {
        return moduleIds.associateWith { migrate(sourceNodeId, targetNodeId, it) }
    }

    override fun getSession(migrationId: MigrationId): MigrationSession? {
        val ctx = sessionRegistry.getContext(migrationId) ?: return null
        return sessionRegistry.toSession(ctx)
    }

    override fun getActiveSessions(): List<MigrationSession> =
        sessionRegistry.getAllActive().map { sessionRegistry.toSession(it) }

    override fun isModuleMigrating(moduleId: String): Boolean = sessionRegistry.isModuleMigrating(moduleId)

    override fun registerNode(node: NodeDescriptor) { nodeRegistry.register(node) }

    override fun unregisterNode(nodeId: String) { nodeRegistry.unregister(nodeId) }

    override fun getRegisteredNodes(): List<NodeDescriptor> = nodeRegistry.all()

    override fun getCurrentPhase(migrationId: MigrationId): MigrationPhase? =
        sessionRegistry.getContext(migrationId)?.phase

    override fun abortMigration(migrationId: MigrationId): Boolean {
        val ctx = sessionRegistry.getContext(migrationId) ?: return false
        ctx.abortRequested = true
        return true
    }

    override fun getMetrics(): MigrationMetrics = diagnosticsCollector.collectCurrentMetrics()

    override fun addObserver(observer: MigrationLifecycleObserver) { observers.add(observer) }

    override fun removeObserver(observer: MigrationLifecycleObserver) { observers.remove(observer) }

    private fun notify(event: MigrationLifecycleEvent) {
        observers.forEach { try { it.onMigrationEvent(event) } catch (_: Exception) {} }
        try { diagnosticsCollector.onMigrationEvent(event) } catch (_: Exception) {}
    }
}
