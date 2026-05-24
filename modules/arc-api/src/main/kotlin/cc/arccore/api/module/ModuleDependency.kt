package cc.arccore.api.module

import cc.arccore.api.module.description.ModuleLoadOrder
import cc.arccore.api.module.description.version.VersionRange

data class ModuleDependency(
    val id: String,
    val versionRange: VersionRange? = null,
    val optional: Boolean = false,
    val loadOrder: ModuleLoadOrder = ModuleLoadOrder.NORMAL
) {
    companion object {
        fun parse(value: String): ModuleDependency {
            var working = value

            val loadOrder = when {
                working.endsWith("@before", ignoreCase = true) -> {
                    working = working.dropLast(7)
                    ModuleLoadOrder.BEFORE
                }
                working.endsWith("@after", ignoreCase = true) -> {
                    working = working.dropLast(6)
                    ModuleLoadOrder.AFTER
                }
                working.endsWith("!") -> {
                    working = working.dropLast(1)
                    ModuleLoadOrder.BEFORE
                }
                else -> ModuleLoadOrder.NORMAL
            }

            val optional = working.endsWith("?")
            if (optional) working = working.dropLast(1)

            val parts = working.split(":", limit = 2)
            val id = parts[0]
            val versionRange = parts.getOrNull(1)?.let { versionStr ->
                if (versionStr.isNotBlank()) VersionRange.parse(versionStr) else null
            }

            return ModuleDependency(
                id = id,
                versionRange = versionRange,
                optional = optional,
                loadOrder = loadOrder
            )
        }
    }
}
