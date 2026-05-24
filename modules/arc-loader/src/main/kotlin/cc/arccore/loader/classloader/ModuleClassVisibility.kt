package cc.arccore.loader.classloader

data class ModuleClassVisibility(
    val moduleId: String,
    val exportedPackages: Set<String> = emptySet(),
    val importedModules: Set<String> = emptySet()
) {
    fun isExported(className: String): Boolean {
        if (exportedPackages.isEmpty()) return false
        return exportedPackages.any { className.startsWith(it) }
    }

    fun canAccess(className: String, otherVisibility: ModuleClassVisibility): Boolean {
        if (otherVisibility.moduleId in importedModules) {
            return otherVisibility.isExported(className)
        }
        return false
    }

    companion object {
        fun allExported(moduleId: String): ModuleClassVisibility =
            ModuleClassVisibility(
                moduleId = moduleId,
                exportedPackages = setOf(""),
                importedModules = emptySet()
            )

        fun noneExported(moduleId: String): ModuleClassVisibility =
            ModuleClassVisibility(
                moduleId = moduleId,
                importedModules = emptySet()
            )
    }
}
