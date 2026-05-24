package cc.arccore.snapshot.runtime.serialization

import cc.arccore.snapshot.runtime.model.RuntimeSnapshot
import cc.arccore.snapshot.runtime.exception.SnapshotSerializationException

object NoOpSerializer : RuntimeStateSerializer {
    override val formatName = "noop"
    override fun serialize(snapshot: RuntimeSnapshot): ByteArray = ByteArray(0)
    override fun deserialize(data: ByteArray): RuntimeSnapshot =
        throw SnapshotSerializationException("noop", "NoOpSerializer cannot deserialize")
}
