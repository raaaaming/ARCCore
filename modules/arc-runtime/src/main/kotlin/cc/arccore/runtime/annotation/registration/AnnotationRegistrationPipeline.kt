package cc.arccore.runtime.annotation.registration

import cc.arccore.api.lifecycle.LifecycleEvent
import cc.arccore.api.lifecycle.LifecycleEventType
import cc.arccore.api.lifecycle.LifecycleObserver
import cc.arccore.api.module.ClassLoaderHolder
import cc.arccore.api.module.ModuleContainerView
import cc.arccore.runtime.annotation.cache.ScanResultCache
import cc.arccore.runtime.annotation.exception.AutoRegistrationException
import cc.arccore.runtime.annotation.scanner.AnnotationScanner
import java.util.concurrent.ConcurrentHashMap

class AnnotationRegistrationPipeline(
    private val scanner: AnnotationScanner,
    private val registrars: List<AutoRegistrar>,
    private val cache: ScanResultCache = ScanResultCache()
) : LifecycleObserver {

    private val targetAnnotations = registrars.map { it.handledAnnotation }.toSet()
    private val registrarMap = registrars.associateBy { it.handledAnnotation }
    private val registeredModules: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override fun onLifecycleEvent(event: LifecycleEvent) {
        val moduleId = event.container.module.id
        when (event.type) {
            LifecycleEventType.ENABLED -> {
                if (!registeredModules.add(moduleId)) return
                val context = event.container.context ?: return
                val classLoader = (context as? ClassLoaderHolder)?.provideClassLoader() ?: return
                autoRegister(event.container, classLoader)
            }
            LifecycleEventType.DISABLED -> {
                registeredModules.remove(moduleId)
            }
            LifecycleEventType.UNLOADED,
            LifecycleEventType.FAILED,
            LifecycleEventType.DEPENDENCY_FAILED -> {
                registeredModules.remove(moduleId)
                cache.invalidate(moduleId)
            }
            else -> Unit
        }
    }

    fun autoRegister(owner: ModuleContainerView, classLoader: ClassLoader) {
        val moduleId = owner.module.id

        val scanResults = cache.get(moduleId)
            ?: scanner.scan(classLoader, moduleId, targetAnnotations).also {
                cache.put(moduleId, it)
            }

        for (result in scanResults) {
            val registrar = registrarMap[result.annotationType] ?: continue
            try {
                registrar.register(result, owner)
            } catch (e: AutoRegistrationException) {
                throw e
            } catch (e: Exception) {
                throw AutoRegistrationException(
                    "Auto-registration failed for '${result.clazz.name}' in module '$moduleId'", e
                )
            }
        }
    }

    fun invalidateCache(moduleId: String) {
        cache.invalidate(moduleId)
    }
}
