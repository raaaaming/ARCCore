package cc.arccore.migration.runtime.transfer

data class SnapshotPayload(
    val snapshotId: String,
    val moduleId: String,
    val data: ByteArray,
    val sizeBytes: Long,
    val checksum: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnapshotPayload) return false
        if (snapshotId != other.snapshotId) return false
        if (moduleId != other.moduleId) return false
        if (!data.contentEquals(other.data)) return false
        if (sizeBytes != other.sizeBytes) return false
        if (checksum != other.checksum) return false
        return true
    }

    override fun hashCode(): Int {
        var result = snapshotId.hashCode()
        result = 31 * result + moduleId.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + checksum.hashCode()
        return result
    }
}
