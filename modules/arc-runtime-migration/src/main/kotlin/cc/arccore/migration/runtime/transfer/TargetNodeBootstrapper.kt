package cc.arccore.migration.runtime.transfer

import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.transport.RuntimeRelocationTransport
import cc.arccore.migration.runtime.transport.TransportBootstrapResult
import cc.arccore.migration.runtime.transport.TransportReadinessResult

internal class TargetNodeBootstrapper(
    private val transport: RuntimeRelocationTransport,
    private val readinessTimeoutMs: Long = 30_000L
) {
    fun bootstrap(context: MigrationContext): BootstrapOutcome {
        val snapshotId = context.migrationId.value
        val result = transport.triggerBootstrap(context.targetNodeId, context.moduleId, snapshotId)
        return when (result) {
            is TransportBootstrapResult.Success -> BootstrapOutcome.Success
            is TransportBootstrapResult.Failure -> BootstrapOutcome.Failure(result.error)
        }
    }

    fun awaitReady(moduleId: String, targetNodeId: String): ReadinessOutcome {
        val result = transport.awaitRestoreReady(targetNodeId, moduleId, readinessTimeoutMs)
        return when (result) {
            TransportReadinessResult.Ready -> ReadinessOutcome.Ready
            TransportReadinessResult.TimedOut -> ReadinessOutcome.TimedOut
            is TransportReadinessResult.NotReady -> ReadinessOutcome.Error(RuntimeException(result.reason))
        }
    }

    sealed class BootstrapOutcome {
        data object Success : BootstrapOutcome()
        data class Failure(val error: Throwable) : BootstrapOutcome()
    }

    sealed class ReadinessOutcome {
        data object Ready : ReadinessOutcome()
        data object TimedOut : ReadinessOutcome()
        data class Error(val error: Throwable) : ReadinessOutcome()
    }
}
