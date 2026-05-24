package cc.arccore.migration.runtime.synchronization

import java.util.concurrent.atomic.AtomicBoolean

internal class MigrationReadWriteBarrier(
    private val sourceGate: AtomicBoolean,
    private val targetGate: AtomicBoolean
) {
    fun commitSwitch() {
        targetGate.set(true)
    }

    fun rollbackSwitch() {
        targetGate.set(false)
        sourceGate.set(true)
    }

    fun isSourceClosed(): Boolean = !sourceGate.get()

    fun isTargetOpen(): Boolean = targetGate.get()
}
