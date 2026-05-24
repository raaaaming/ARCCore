package cc.arccore.zerodowntime.runtime.coordination

import java.util.concurrent.ConcurrentHashMap

internal class ShadowModuleRegistry {
    // 타입 안전성을 위해 Any로 보관 (실제 프로젝트에서는 ModuleContainer 타입 사용)
    private val shadowContainers = ConcurrentHashMap<String, Any>()

    fun register(moduleId: String, container: Any) {
        shadowContainers[moduleId] = container
    }

    fun get(moduleId: String): Any? = shadowContainers[moduleId]

    fun promote(moduleId: String): Any? = shadowContainers.remove(moduleId)

    fun discard(moduleId: String) {
        shadowContainers.remove(moduleId)
    }

    fun activeShadows(): Map<String, Any> = shadowContainers.toMap()

    fun hasShadow(moduleId: String): Boolean = shadowContainers.containsKey(moduleId)
}
