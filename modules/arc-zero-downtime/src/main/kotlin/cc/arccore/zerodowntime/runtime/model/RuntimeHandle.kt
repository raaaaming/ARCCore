package cc.arccore.zerodowntime.runtime.model

import java.time.Instant

data class RuntimeHandle(
    val moduleId: String,
    val generation: Int,
    val role: RuntimeRole,
    val createdAt: Instant = Instant.now()
)

enum class RuntimeRole { OLD, NEW, PRIMARY }
