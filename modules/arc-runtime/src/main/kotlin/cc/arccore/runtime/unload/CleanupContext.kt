package cc.arccore.runtime.unload

import cc.arccore.api.module.ModuleContainer
import cc.arccore.loader.ModuleClassLoader

data class CleanupContext(
    val container: ModuleContainer,
    val classLoader: ModuleClassLoader?,
    val moduleId: String,
    /**
     * null = 일반 unload.
     * non-null = reload 대상 moduleId (hot-reload 확장점, 현재는 미사용).
     */
    val reloadTarget: String? = null
) {
    val isReload: Boolean get() = reloadTarget != null

    companion object {
        fun create(container: ModuleContainer, classLoader: ModuleClassLoader?): CleanupContext =
            CleanupContext(
                container = container,
                classLoader = classLoader,
                moduleId = container.module.id
            )

        fun createForReload(
            container: ModuleContainer,
            classLoader: ModuleClassLoader?,
            newModuleId: String
        ): CleanupContext =
            CleanupContext(
                container = container,
                classLoader = classLoader,
                moduleId = container.module.id,
                reloadTarget = newModuleId
            )
    }
}
