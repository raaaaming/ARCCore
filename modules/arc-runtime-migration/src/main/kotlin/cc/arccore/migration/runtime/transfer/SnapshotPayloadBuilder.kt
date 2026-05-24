package cc.arccore.migration.runtime.transfer

internal class SnapshotPayloadBuilder {
    fun build(moduleId: String, snapshotId: String, stateData: Map<String, Any?>): SnapshotPayload {
        val baos = java.io.ByteArrayOutputStream()
        java.io.ObjectOutputStream(baos).use { oos ->
            val serializable = stateData.filterValues { it == null || it is java.io.Serializable }
            oos.writeObject(serializable)
        }
        val data = baos.toByteArray()
        val checksum = java.security.MessageDigest.getInstance("MD5")
            .digest(data).joinToString("") { "%02x".format(it) }
        return SnapshotPayload(snapshotId, moduleId, data, data.size.toLong(), checksum)
    }

    fun estimateSizeBytes(stateData: Map<String, Any?>): Long {
        return stateData.size.toLong() * 64L
    }
}
