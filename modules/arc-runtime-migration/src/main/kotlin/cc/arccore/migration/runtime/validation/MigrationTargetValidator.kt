package cc.arccore.migration.runtime.validation

import cc.arccore.migration.runtime.coordination.MigrationNodeRegistry
import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.transport.RuntimeRelocationTransport
import cc.arccore.migration.runtime.transport.TransportPingResult

internal class MigrationTargetValidator(
    private val nodeRegistry: MigrationNodeRegistry,
    private val transport: RuntimeRelocationTransport
) {
    fun validate(context: MigrationContext): List<MigrationValidationFailure> {
        val failures = mutableListOf<MigrationValidationFailure>()
        val target = nodeRegistry.get(context.targetNodeId)
        if (target == null) {
            failures.add(MigrationValidationFailure.TargetNodeNotFound(context.targetNodeId))
            return failures
        }
        if (target.capacity.availableModules <= 0) {
            failures.add(MigrationValidationFailure.TargetNodeAtCapacity(context.targetNodeId, target.capacity.maxModules))
        }
        val ping = transport.ping(context.targetNodeId)
        if (ping is TransportPingResult.Unreachable) {
            failures.add(MigrationValidationFailure.TransportUnreachable(context.targetNodeId))
        }
        return failures
    }
}
