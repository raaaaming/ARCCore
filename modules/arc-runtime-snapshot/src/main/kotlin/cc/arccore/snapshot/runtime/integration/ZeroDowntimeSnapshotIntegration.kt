package cc.arccore.snapshot.runtime.integration

import cc.arccore.snapshot.runtime.RuntimeSnapshotSystem
import cc.arccore.snapshot.runtime.model.RecoveryResult
import cc.arccore.snapshot.runtime.model.SnapshotCaptureResult

// arc-zero-downtime과의 통합 포인트
// ZeroDowntimeReloadRuntime의 PREPARE 단계에서 snapshot 캡처,
// BOOTSTRAP_NEW 이후 OWNERSHIP_TRANSFER 전에 recovery 수행하는 구조

class ZeroDowntimeSnapshotIntegration(
    private val snapshotSystem: RuntimeSnapshotSystem
) {
    fun snapshotBeforeReload(moduleId: String): SnapshotCaptureResult {
        return snapshotSystem.createSnapshot(moduleId)
    }

    fun recoverAfterReload(moduleId: String): RecoveryResult {
        return snapshotSystem.recoverLatest(moduleId)
    }

    fun isSnapshotAvailable(moduleId: String): Boolean {
        return snapshotSystem.getLatestSnapshot(moduleId) != null
    }
}
