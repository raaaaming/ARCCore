package cc.arccore.api.di

import kotlin.reflect.KClass

interface DIContainer : DependencyResolver {
    fun <T : Any> bindSingleton(type: KClass<T>, provider: InstanceProvider<T>)
    fun <T : Any> bindModule(moduleId: String, type: KClass<T>, provider: InstanceProvider<T>)
    fun <T : Any> bindInstance(type: KClass<T>, instance: T)
    fun <T : Any> bindModuleInstance(moduleId: String, type: KClass<T>, instance: T)
    fun clearModule(moduleId: String)
    fun <T : Any> isBound(type: KClass<T>): Boolean
    fun <T : Any> isModuleBound(moduleId: String, type: KClass<T>): Boolean
}
