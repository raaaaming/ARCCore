package cc.arccore.api.di

import kotlin.reflect.KClass

interface ResolutionContext {
    fun <T : Any> resolve(type: KClass<T>): T
    val moduleId: String
}

interface InstanceProvider<T : Any> {
    val scope: Scope
    fun provide(context: ResolutionContext): T
    fun dispose(instance: T) {}
}
