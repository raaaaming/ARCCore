package cc.arccore.migration.runtime.model

enum class MigrationPhase {
    IDLE,
    PREPARE_MIGRATION,
    VALIDATE_TARGET,
    BEGIN_DRAINING,
    SNAPSHOT_STATE,
    TRANSFER_OWNERSHIP,
    BOOTSTRAP_TARGET,
    RESTORE_STATE,
    SWITCH_ROUTING,
    FINALIZE_MIGRATION,
    CLEANUP_SOURCE,
    COMPLETED,
    ROLLING_BACK,
    ABORTED,
    FAILED;

    fun isTerminal(): Boolean = this == COMPLETED || this == FAILED || this == ABORTED

    fun isActive(): Boolean = !isTerminal() && this != IDLE

    fun canRollback(): Boolean = when (this) {
        SWITCH_ROUTING, FINALIZE_MIGRATION, CLEANUP_SOURCE, COMPLETED -> false
        else -> !isTerminal()
    }
}
