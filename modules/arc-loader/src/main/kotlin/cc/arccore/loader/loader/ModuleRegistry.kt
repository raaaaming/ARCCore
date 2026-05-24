package cc.arccore.loader.loader

import cc.arccore.api.module.ModuleContainer
import java.util.concurrent.ConcurrentHashMap

interface ModuleRegistry {

    fun register(container: ModuleContainer)

    fun unregister(id: String)

    fun get(id: String): ModuleContainer?

    fun getAll(): Collection<ModuleContainer>

    fun contains(id: String): Boolean

    fun size(): Int

    fun clear()
}

class DefaultModuleRegistry : ModuleRegistry {

    private val containers = ConcurrentHashMap<String, ModuleContainer>()

    override fun register(container: ModuleContainer) {
        val previous = containers.put(container.module.id, container)
        if (previous != null) {
            containers.put(container.module.id, previous)
            throw IllegalStateException(
                "Module '${container.module.id}' is already registered. " +
                    "Unregister it first before replacing."
            )
        }
    }

    override fun unregister(id: String) {
        containers.remove(id)
    }

    override fun get(id: String): ModuleContainer? = containers[id]

    override fun getAll(): Collection<ModuleContainer> = containers.values.toList()

    override fun contains(id: String): Boolean = containers.containsKey(id)

    override fun size(): Int = containers.size

    override fun clear() {
        containers.clear()
    }
}
