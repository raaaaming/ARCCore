package cc.arccore.api.service

import cc.arccore.api.module.ModuleContainerView
import kotlin.reflect.KClass

/**
 * 서비스 등록 정보를 담는 불변 디스크립터.
 *
 * [owner]는 ClassLoader 누수를 방지하기 위해 보유하지 않습니다.
 * 소유 모듈 식별은 [ownerId] (String) 로만 수행합니다.
 */
data class ServiceDescriptor<T : Any>(
    val type: KClass<T>,
    val ownerId: String,
    val implementation: T,
    val registeredAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun <T : Any> of(
            type: KClass<T>,
            owner: ModuleContainerView,
            implementation: T
        ): ServiceDescriptor<T> = ServiceDescriptor(type, owner.module.id, implementation)
    }
}
