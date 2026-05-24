package cc.arccore.migration.runtime.validation

sealed class MigrationValidationFailure {
    data class TargetNodeNotFound(val nodeId: String) : MigrationValidationFailure()
    data class TargetNodeAtCapacity(val nodeId: String, val capacity: Int) : MigrationValidationFailure()
    data class SourceAlreadyMigrating(val moduleId: String) : MigrationValidationFailure()
    data class TransportUnreachable(val targetNodeId: String) : MigrationValidationFailure()
    data object SnapshotUnavailable : MigrationValidationFailure()
    data class VersionIncompatible(val sourceVersion: String, val targetVersion: String) : MigrationValidationFailure()
    data class GenericValidationFailed(val reason: String) : MigrationValidationFailure()
}
