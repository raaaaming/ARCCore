package cc.arccore.di

import cc.arccore.api.di.Container

class ContainerImpl : Container {

    private val singletons = mutableMapOf<Class<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(type: Class<T>): T {
        return singletons[type] as? T
            ?: throw IllegalStateException("No registered instance for ${type.name}")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getOrNull(type: Class<T>): T? {
        return singletons[type] as? T
    }

    override fun <T : Any> register(type: Class<T>, instance: T) {
        singletons[type] = instance
    }

    override fun <T : Any> registerSingleton(type: Class<T>, instance: T) {
        singletons[type] = instance
    }

    override fun has(type: Class<*>): Boolean = type in singletons

    override fun clear() {
        singletons.clear()
    }
}
