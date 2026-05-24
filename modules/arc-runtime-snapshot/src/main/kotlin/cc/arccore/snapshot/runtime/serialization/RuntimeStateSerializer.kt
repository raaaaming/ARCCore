package cc.arccore.snapshot.runtime.serialization

import cc.arccore.snapshot.runtime.model.RuntimeSnapshot

interface RuntimeStateSerializer {
    val formatName: String
    val schemaVersion: Int get() = 1

    fun serialize(snapshot: RuntimeSnapshot): ByteArray
    fun deserialize(data: ByteArray): RuntimeSnapshot

    // 미래 확장 포인트
    fun supportsStreaming(): Boolean = false
    fun supportsIncremental(): Boolean = false
}
