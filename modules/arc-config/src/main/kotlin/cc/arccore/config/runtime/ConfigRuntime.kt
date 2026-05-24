package cc.arccore.config.runtime

import cc.arccore.config.runtime.exception.ConfigRuntimeException
import cc.arccore.config.runtime.reload.ReloadResult
import kotlin.reflect.KClass

interface ConfigRuntime : cc.arccore.api.module.ConfigRuntimeMarker {

    fun <T : Any> load(clazz: KClass<T>, path: String): ConfigHandle<T>

    fun <T : Any> reload(handle: ConfigHandle<T>): ReloadResult

    fun unloadAll(moduleId: String)

    fun shutdown()

    companion object {
        val NOOP: ConfigRuntime = object : ConfigRuntime {
            override fun <T : Any> load(clazz: KClass<T>, path: String): ConfigHandle<T> =
                throw ConfigRuntimeException("No ConfigRuntime configured")

            override fun <T : Any> reload(handle: ConfigHandle<T>): ReloadResult =
                ReloadResult.NoChange("noop")

            override fun unloadAll(moduleId: String) {}
            override fun shutdown() {}
        }
    }
}
