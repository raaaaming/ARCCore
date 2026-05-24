package cc.arccore.runtime.annotation.registration

import cc.arccore.api.annotation.ARCListener as ARCListenerAnnotation
import cc.arccore.api.module.ModuleContainerView
import cc.arccore.event.listener.ListenerRegistry
import cc.arccore.runtime.annotation.exception.AutoRegistrationException
import cc.arccore.runtime.annotation.scanner.ScanResult
import cc.arccore.runtime.annotation.validation.AnnotatedClassValidator
import org.bukkit.event.Listener
import kotlin.reflect.KClass

class ListenerAutoRegistrar(
    private val listenerRegistry: ListenerRegistry,
    private val validator: AnnotatedClassValidator = AnnotatedClassValidator()
) : AutoRegistrar {

    override val handledAnnotation: KClass<out Annotation> = ARCListenerAnnotation::class

    override fun register(result: ScanResult, owner: ModuleContainerView) {
        validator.validate(result.clazz, ARCListenerAnnotation::class, Listener::class)

        @Suppress("UNCHECKED_CAST")
        val listenerClass = result.clazz as Class<out Listener>
        val listener = try {
            listenerClass.getDeclaredConstructor().newInstance()
        } catch (e: Exception) {
            throw AutoRegistrationException(
                "Failed to instantiate listener '${result.clazz.name}' for module '${owner.module.id}'", e
            )
        }
        listenerRegistry.register(owner, listener)
    }
}
