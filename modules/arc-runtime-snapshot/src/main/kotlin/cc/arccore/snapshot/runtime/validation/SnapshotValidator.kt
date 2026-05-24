package cc.arccore.snapshot.runtime.validation

import cc.arccore.snapshot.runtime.exception.InvalidSnapshotException
import cc.arccore.snapshot.runtime.model.RuntimeSnapshot

class SnapshotValidator {
    fun validate(snapshot: RuntimeSnapshot): Exception? {
        if (snapshot.runtimeId.isBlank()) {
            return InvalidSnapshotException(snapshot.id.value, "runtimeId is blank")
        }
        if (snapshot.state.isEmpty() && snapshot.ownershipState.isEmpty()) {
            // empty snapshot은 경고이지만 오류는 아님
            return null
        }
        return null
    }

    fun validateCompatibility(snapshot: RuntimeSnapshot, currentSchemaVersion: Int): Exception? {
        if (!snapshot.metadata.isCompatibleWith(currentSchemaVersion)) {
            return InvalidSnapshotException(
                snapshot.id.value,
                "Schema version mismatch: snapshot=${snapshot.metadata.schemaVersion}, current=$currentSchemaVersion"
            )
        }
        return null
    }
}
