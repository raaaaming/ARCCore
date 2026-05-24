package cc.arccore.snapshot.runtime.model

enum class RecoveryPhase {
    IDLE,
    PREPARE_RECOVERY,
    LOAD_SNAPSHOT,
    VALIDATE_SNAPSHOT,
    RESTORE_OWNERSHIP,
    RESTORE_RUNTIME_STATE,
    RESUME_EXECUTION,
    FINALIZE_RECOVERY,
    COMPLETED,
    ROLLING_BACK,
    FAILED;

    fun isTerminal(): Boolean = this == COMPLETED || this == FAILED
    fun isActive(): Boolean = !isTerminal() && this != IDLE
}
