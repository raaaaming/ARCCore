package cc.arccore.api.di

import kotlin.reflect.KClass

interface DependencyResolver {
    fun <T : Any> resolve(type: KClass<T>, moduleId: String): T
    fun <T : Any> canResolve(type: KClass<T>, moduleId: String): Boolean
}
