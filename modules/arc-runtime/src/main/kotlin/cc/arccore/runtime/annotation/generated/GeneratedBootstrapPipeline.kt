package cc.arccore.runtime.annotation.generated

import cc.arccore.api.lifecycle.LifecycleEvent
import cc.arccore.api.lifecycle.LifecycleEventType
import cc.arccore.api.lifecycle.LifecycleObserver
import cc.arccore.api.module.ClassLoaderHolder
import cc.arccore.runtime.annotation.generated.exception.RegistrarLoadException
import cc.arccore.runtime.annotation.registration.AnnotationRegistrationPipeline
import cc.arccore.runtime.context.RuntimeModuleContext
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap

class GeneratedBootstrapPipeline(
    private val plugin: Plugin,
    private val fallback: AnnotationRegistrationPipeline? = null
) : LifecycleObserver {

    private val bootstrappedModules: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override fun onLifecycleEvent(event: LifecycleEvent) {
        val moduleId = event.container.module.id
        when (event.type) {
            LifecycleEventType.ENABLED -> {
                if (!bootstrappedModules.add(moduleId)) return
                val ctx = event.container.context ?: return
                val classLoader = (ctx as? ClassLoaderHolder)?.provideClassLoader() ?: return
                val runtimeContext = ctx as? RuntimeModuleContext ?: return
                tryBootstrap(moduleId, classLoader, runtimeContext, event)
            }
            LifecycleEventType.DISABLED,
            LifecycleEventType.UNLOADED,
            LifecycleEventType.FAILED,
            LifecycleEventType.DEPENDENCY_FAILED -> bootstrappedModules.remove(moduleId)
            else -> Unit
        }
    }

    private fun tryBootstrap(
        moduleId: String,
        classLoader: ClassLoader,
        context: RuntimeModuleContext,
        event: LifecycleEvent
    ) {
        val registrar = try {
            RuntimeRegistrarLoader.load(classLoader)
        } catch (e: RegistrarLoadException) {
            plugin.logger.warning("[ARCCore] Generated bootstrap load failed for '$moduleId': ${e.message}")
            null
        }

        if (registrar != null) {
            registrar.register(context, plugin)
            plugin.logger.fine("[ARCCore] Generated bootstrap applied for module '$moduleId'")
        } else {
            fallback?.autoRegister(event.container, classLoader)
        }
    }
}
