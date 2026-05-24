package cc.arccore.ksp

import cc.arccore.ksp.generated.injection.InjectionGenerator
import cc.arccore.ksp.generated.registrar.BootstrapGenerator
import cc.arccore.ksp.generator.MetadataJsonGenerator
import cc.arccore.ksp.model.CommandEntry
import cc.arccore.ksp.model.ConstructorParam
import cc.arccore.ksp.model.InjectableEntry
import cc.arccore.ksp.model.ListenerEntry
import cc.arccore.ksp.model.ServiceEntry
import cc.arccore.ksp.output.ResourceFileWriter
import cc.arccore.ksp.validation.ClassDeclarationValidator
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile



class ARCAnnotationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val validator = ClassDeclarationValidator(logger)

    private val commands = mutableListOf<CommandEntry>()
    private val listeners = mutableListOf<ListenerEntry>()
    private val services = mutableListOf<ServiceEntry>()
    private val collectedFiles = mutableListOf<KSFile>()

    // Generated injection pipeline
    private val injectionGenerator = InjectionGenerator(codeGenerator, logger)
    private val injectables = mutableListOf<InjectableEntry>()
    private val injectableFiles = mutableListOf<KSFile>()

    companion object {
        private const val ARC_COMMAND = "cc.arccore.api.annotation.ARCCommand"
        private const val COMMAND_SPEC = "cc.arccore.api.command.CommandSpec"
        private const val ARC_LISTENER = "cc.arccore.api.annotation.ARCListener"
        private const val ARC_SERVICE = "cc.arccore.api.annotation.ARCService"

        private const val ARC_COMMAND_INTERFACE = "cc.arccore.api.command.ARCCommand"
        private const val BUKKIT_LISTENER = "org.bukkit.event.Listener"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        (resolver.getSymbolsWithAnnotation(ARC_COMMAND) +
                resolver.getSymbolsWithAnnotation(COMMAND_SPEC))
            .filterIsInstance<KSClassDeclaration>()
            .distinctBy { it.qualifiedName?.asString() }
            .forEach { decl ->
                if (validator.validate(decl, "ARCCommand", ARC_COMMAND_INTERFACE)) {
                    val specAnno = decl.annotations.firstOrNull { anno ->
                        anno.annotationType.resolve().declaration.qualifiedName?.asString() == COMMAND_SPEC
                    }
                    fun specArg(name: String) = specAnno?.arguments
                        ?.firstOrNull { it.name?.asString() == name }?.value
                    @Suppress("UNCHECKED_CAST")
                    val aliases = (specArg("aliases") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    commands.add(CommandEntry(
                        className = decl.qualifiedName!!.asString(),
                        constructorParams = extractConstructorParams(decl),
                        commandName = specArg("name") as? String ?: "",
                        commandAliases = aliases,
                        commandPermission = specArg("permission") as? String ?: "",
                        commandDescription = specArg("description") as? String ?: "",
                        commandUsage = specArg("usage") as? String ?: ""
                    ))
                    decl.containingFile?.let { collectedFiles.add(it) }
                }
            }

        resolver.getSymbolsWithAnnotation(ARC_LISTENER)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { decl ->
                if (validator.validate(decl, "ARCListener", BUKKIT_LISTENER)) {
                    listeners.add(ListenerEntry(
                        className = decl.qualifiedName!!.asString(),
                        constructorParams = extractConstructorParams(decl)
                    ))
                    decl.containingFile?.let { collectedFiles.add(it) }
                }
            }

        resolver.getSymbolsWithAnnotation(ARC_SERVICE)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { decl ->
                if (validator.validate(decl, "ARCService", null)) {
                    val annotation = decl.annotations.firstOrNull { anno ->
                        anno.annotationType.resolve().declaration.qualifiedName?.asString() == ARC_SERVICE
                    }
                    val name = annotation?.arguments
                        ?.firstOrNull { it.name?.asString() == "name" }
                        ?.value as? String ?: ""
                    services.add(ServiceEntry(className = decl.qualifiedName!!.asString(), name = name))
                    decl.containingFile?.let { collectedFiles.add(it) }
                }
            }

        // Generated injection: collect @ArcComponent classes across rounds
        val (entries, files) = injectionGenerator.process(resolver)
        injectables.addAll(entries)
        injectableFiles.addAll(files)

        return emptyList()
    }

    override fun finish() {
        // Generated injection factories (independent of commands/listeners/services)
        if (injectables.isNotEmpty()) {
            injectionGenerator.generateAll(injectables, injectableFiles)
        }

        if (commands.isEmpty() && listeners.isEmpty() && services.isEmpty()) return

        val deps = Dependencies(aggregating = true, *collectedFiles.toTypedArray())

        if (commands.isNotEmpty()) {
            ResourceFileWriter.write(
                codeGenerator, "META-INF/arc/commands", "json",
                MetadataJsonGenerator.generateCommands(commands), deps
            )
        }
        if (listeners.isNotEmpty()) {
            ResourceFileWriter.write(
                codeGenerator, "META-INF/arc/listeners", "json",
                MetadataJsonGenerator.generateListeners(listeners), deps
            )
        }
        if (services.isNotEmpty()) {
            ResourceFileWriter.write(
                codeGenerator, "META-INF/arc/services", "json",
                MetadataJsonGenerator.generateServices(services), deps
            )
        }

        if (commands.isNotEmpty() || listeners.isNotEmpty()) {
            BootstrapGenerator.generate(codeGenerator, logger, commands, listeners, collectedFiles)
        }
    }

    private fun extractConstructorParams(decl: KSClassDeclaration): List<ConstructorParam> =
        decl.primaryConstructor?.parameters?.mapNotNull { param ->
            val resolved = param.type.resolve()
            val fqn = resolved.declaration.qualifiedName?.asString() ?: return@mapNotNull null
            ConstructorParam(typeFqn = fqn, nullable = resolved.isMarkedNullable)
        } ?: emptyList()
}

class ARCAnnotationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ARCAnnotationProcessor(environment.codeGenerator, environment.logger)
    }
}
