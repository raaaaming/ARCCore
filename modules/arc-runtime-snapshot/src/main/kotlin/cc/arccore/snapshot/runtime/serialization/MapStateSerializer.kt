package cc.arccore.snapshot.runtime.serialization

import cc.arccore.snapshot.runtime.model.RuntimeSnapshot
import cc.arccore.snapshot.runtime.model.RuntimeSnapshotMetadata
import cc.arccore.snapshot.runtime.model.SnapshotId
import cc.arccore.snapshot.runtime.exception.SnapshotSerializationException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.Instant

class MapStateSerializer : RuntimeStateSerializer {
    override val formatName = "java-object-stream"

    override fun serialize(snapshot: RuntimeSnapshot): ByteArray {
        return try {
            val baos = ByteArrayOutputStream()
            ObjectOutputStream(baos).use { oos ->
                oos.writeUTF(snapshot.id.value)
                oos.writeUTF(snapshot.runtimeId)
                oos.writeLong(snapshot.capturedAt.toEpochMilli())
                // state: Map<String, Any?> — serialize only Serializable values
                val serializableState = snapshot.state.filterValues { it == null || it is java.io.Serializable }
                oos.writeObject(serializableState)
                val serializableOwnership = snapshot.ownershipState.filterValues { it == null || it is java.io.Serializable }
                oos.writeObject(serializableOwnership)
            }
            baos.toByteArray()
        } catch (e: Exception) {
            throw SnapshotSerializationException(snapshot.runtimeId, "Serialization error", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(data: ByteArray): RuntimeSnapshot {
        return try {
            ObjectInputStream(ByteArrayInputStream(data)).use { ois ->
                val id = SnapshotId(ois.readUTF())
                val runtimeId = ois.readUTF()
                val capturedAt = Instant.ofEpochMilli(ois.readLong())
                val state = ois.readObject() as Map<String, Any?>
                val ownershipState = ois.readObject() as Map<String, Any?>
                RuntimeSnapshot(
                    id = id,
                    runtimeId = runtimeId,
                    capturedAt = capturedAt,
                    state = state,
                    ownershipState = ownershipState
                )
            }
        } catch (e: Exception) {
            throw SnapshotSerializationException("unknown", "Deserialization error", e)
        }
    }
}
