package cc.arccore.migration.runtime.exception

import cc.arccore.migration.runtime.model.MigrationId
import cc.arccore.migration.runtime.model.MigrationPhase

open class MigrationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class MigrationAlreadyActiveException(val moduleId: String) :
    MigrationException("Migration already active for module '$moduleId'")

class MigrationAbortedException(val migrationId: MigrationId, val phase: MigrationPhase) :
    MigrationException("Migration $migrationId aborted at phase $phase")

class TargetNodeUnreachableException(val targetNodeId: String, message: String, cause: Throwable? = null) :
    MigrationException(message, cause)

class SnapshotTransferException(val migrationId: MigrationId, message: String, cause: Throwable? = null) :
    MigrationException(message, cause)

class TargetBootstrapException(val migrationId: MigrationId, val targetNodeId: String, message: String, cause: Throwable? = null) :
    MigrationException(message, cause)

class StateRestoreException(val migrationId: MigrationId, val targetNodeId: String, message: String, cause: Throwable? = null) :
    MigrationException(message, cause)

class RoutingSwitchException(val migrationId: MigrationId, message: String, cause: Throwable? = null) :
    MigrationException(message, cause)

class MigrationRollbackException(val migrationId: MigrationId, val originalError: Throwable, message: String, cause: Throwable? = null) :
    MigrationException(message, cause)

class StaleMigrationStateException(val migrationId: MigrationId, val moduleId: String) :
    MigrationException("Stale migration state for module '$moduleId' in migration $migrationId")
