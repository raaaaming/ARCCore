package cc.arccore.config.runtime.ownership

import cc.arccore.config.runtime.ConfigHandle

interface ConfigOwnershipRegistry {
    fun register(moduleId: String, handle: ConfigHandle<*>)
    fun closeAll(moduleId: String): Int
    fun closeAllHandles(): Int
    fun registeredModules(): Set<String>
}
