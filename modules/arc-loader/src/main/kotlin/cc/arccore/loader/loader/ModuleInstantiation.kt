package cc.arccore.loader.loader

import cc.arccore.api.module.ArcModuleAPI
import cc.arccore.api.module.ModuleDescription
import cc.arccore.loader.ModuleClassLoader
import cc.arccore.loader.loader.exception.ModuleInstantiationException

class ModuleInstantiation {

    fun loadMainClass(
        classLoader: ModuleClassLoader,
        description: ModuleDescription
    ): Class<out ArcModuleAPI> {
        val mainClass: Class<*> = try {
            classLoader.loadClass(description.mainClass)
        } catch (e: ClassNotFoundException) {
            throw ModuleInstantiationException(
                "Main class '${description.mainClass}' not found in module '${description.id}'",
                e
            )
        } catch (e: Exception) {
            throw ModuleInstantiationException(
                "Failed to load main class '${description.mainClass}' for module '${description.id}'",
                e
            )
        }

        if (!ArcModuleAPI::class.java.isAssignableFrom(mainClass)) {
            throw ModuleInstantiationException(
                "Main class '${description.mainClass}' in module '${description.id}' " +
                    "does not implement ArcModuleAPI"
            )
        }

        @Suppress("UNCHECKED_CAST")
        return mainClass as Class<out ArcModuleAPI>
    }

    fun createInstance(
        mainClass: Class<out ArcModuleAPI>,
        description: ModuleDescription
    ): ArcModuleAPI {
        val constructor = try {
            mainClass.getDeclaredConstructor()
        } catch (e: NoSuchMethodException) {
            throw ModuleInstantiationException(
                "Module '${description.id}' main class '${description.mainClass}' " +
                    "has no no-arg constructor"
            )
        }

        return try {
            constructor.isAccessible = true
            constructor.newInstance()
        } catch (e: Exception) {
            throw ModuleInstantiationException(
                "Failed to instantiate module '${description.id}' " +
                    "from class '${description.mainClass}'",
                e
            )
        }
    }
}
