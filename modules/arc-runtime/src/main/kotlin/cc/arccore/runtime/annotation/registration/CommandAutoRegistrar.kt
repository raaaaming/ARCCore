package cc.arccore.runtime.annotation.registration

import cc.arccore.api.annotation.ARCCommand as ARCCommandAnnotation
import cc.arccore.api.command.ARCCommand as ARCCommandInterface
import cc.arccore.api.command.CommandRegistry
import cc.arccore.api.module.ModuleContainerView
import cc.arccore.runtime.annotation.exception.AutoRegistrationException
import cc.arccore.runtime.annotation.scanner.ScanResult
import cc.arccore.runtime.annotation.validation.AnnotatedClassValidator
import kotlin.reflect.KClass

class CommandAutoRegistrar(
    private val commandRegistry: CommandRegistry,
    private val validator: AnnotatedClassValidator = AnnotatedClassValidator()
) : AutoRegistrar {

    override val handledAnnotation: KClass<out Annotation> = ARCCommandAnnotation::class

    override fun register(result: ScanResult, owner: ModuleContainerView) {
        validator.validate(result.clazz, ARCCommandAnnotation::class, ARCCommandInterface::class)

        @Suppress("UNCHECKED_CAST")
        val commandClass = result.clazz as Class<out ARCCommandInterface>
        val command = try {
            commandClass.getDeclaredConstructor().newInstance()
        } catch (e: Exception) {
            throw AutoRegistrationException(
                "Failed to instantiate command '${result.clazz.name}' for module '${owner.module.id}'", e
            )
        }
        commandRegistry.register(command, owner)
    }
}
