package cc.arccore.api.service

import cc.arccore.api.module.ModuleContainerView
import cc.arccore.api.service.exception.ServiceNotFoundException
import kotlin.reflect.KClass

interface ServiceRegistry {

    fun <T : Any> register(
        type: KClass<T>,
        provider: T,
        owner: ModuleContainerView,
        override: Boolean = false
    )

    fun <T : Any> get(type: KClass<T>): T?

    @Throws(ServiceNotFoundException::class)
    fun <T : Any> require(type: KClass<T>): T

    fun <T : Any> unregister(type: KClass<T>)

    fun unregisterAll(owner: ModuleContainerView): Int

    /** 모듈 ID 문자열로 직접 해제합니다 (ClassLoader가 이미 닫힌 이후에도 안전). */
    fun unregisterAllById(ownerId: String): Int

    fun <T : Any> isRegistered(type: KClass<T>): Boolean

    fun registeredTypes(): Set<KClass<*>>

    fun registeredBy(owner: ModuleContainerView): Set<KClass<*>>

    fun registeredById(ownerId: String): Set<KClass<*>>
}
