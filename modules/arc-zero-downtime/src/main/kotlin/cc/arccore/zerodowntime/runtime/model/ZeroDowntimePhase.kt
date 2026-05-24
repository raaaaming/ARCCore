package cc.arccore.zerodowntime.runtime.model

enum class ZeroDowntimePhase {
    IDLE,
    PREPARE,
    BOOTSTRAP_NEW,
    VALIDATE,
    OWNERSHIP_TRANSFER,
    REQUEST_DRAIN,
    SWITCH_ROUTING,
    CLEANUP_OLD,
    COMPLETED,
    ROLLING_BACK,
    ABORTED,
    FAILED
}
