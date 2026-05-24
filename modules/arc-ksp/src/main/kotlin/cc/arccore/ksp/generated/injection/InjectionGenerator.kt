package cc.arccore.ksp.generated.injection

import cc.arccore.ksp.generated.injection.validation.CompileTimeValidator
import cc.arccore.ksp.model.InjectableEntry
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class InjectionGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    private val validator = CompileTimeValidator(logger)

    companion object {
        private const val ARC_COMPONENT = "cc.arccore.api.di.ArcComponent"
        private const val ARC_SINGLETON = "cc.arccore.api.di.ArcSingleton"
        private const val INJECT = "cc.arccore.api.di.Inject"
    }

    fun process(resolver: Resolver): Pair<List<InjectableEntry>, List<KSFile>> {
        val entries = mutableListOf<InjectableEntry>()
        val files = mutableListOf<KSFile>()

        resolver.getSymbolsWithAnnotation(ARC_COMPONENT)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { classDecl ->
                val entry = toEntry(classDecl) ?: return@forEach
                entries += entry
                classDecl.containingFile?.let { files += it }
            }

        return entries to files
    }

    fun generateAll(entries: List<InjectableEntry>, originatingFiles: List<KSFile>) {
        if (entries.isEmpty()) return

        if (!validator.validateAll(entries)) return

        entries.forEach { entry ->
            FactoryGenerator.generate(codeGenerator, logger, entry, originatingFiles)
        }
        GraphGenerator.generate(codeGenerator, entries, originatingFiles)

        logger.info("[ARCCore DI] Generated ${entries.size} injection factories.")
    }

    private fun toEntry(classDecl: KSClassDeclaration): InjectableEntry? {
        val fqcn = classDecl.qualifiedName?.asString() ?: return null

        // Collect constructors: primary + secondary (via declarations)
        val allCtors = mutableListOf<KSFunctionDeclaration>()
        classDecl.primaryConstructor?.let { allCtors.add(it) }
        for (member in classDecl.declarations) {
            if (member is KSFunctionDeclaration && member.simpleName.asString() == "<init>") {
                if (allCtors.none { it === member }) allCtors.add(member)
            }
        }

        val injectCtors = allCtors.filter { ctor ->
            ctor.annotations.any { anno: KSAnnotation ->
                anno.annotationType.resolve().declaration.qualifiedName?.asString() == INJECT
            }
        }

        val validationResult = validator.validateDeclaration(classDecl, injectCtors)
        val constructor = when (validationResult) {
            is CompileTimeValidator.ValidationResult.Error -> {
                logger.error("[ARCCore DI] ${validationResult.message}", classDecl)
                return null
            }
            is CompileTimeValidator.ValidationResult.Valid -> validationResult.constructor
        }

        val hasSingleton = classDecl.annotations.any { anno: KSAnnotation ->
            anno.annotationType.resolve().declaration.qualifiedName?.asString() == ARC_SINGLETON
        }
        val scope = if (hasSingleton) InjectableEntry.InjectableScope.SINGLETON
        else InjectableEntry.InjectableScope.TRANSIENT

        val params = constructor.parameters.mapIndexed { i, param ->
            val resolved = param.type.resolve()
            val typeFqcn = resolved.declaration.qualifiedName?.asString() ?: run {
                logger.error(
                    "[ARCCore DI] Cannot determine FQCN for parameter #$i of '$fqcn'.",
                    param
                )
                return null
            }
            InjectableEntry.ParamEntry(
                name = param.name?.asString() ?: "p$i",
                typeFqcn = typeFqcn
            )
        }

        val pkgName = fqcn.substringBeforeLast('.', missingDelimiterValue = "")
        val simpleName = fqcn.substringAfterLast('.')
        val factoryFqcn = if (pkgName.isEmpty()) "generated.di.Generated${simpleName}Factory"
        else "$pkgName.generated.di.Generated${simpleName}Factory"

        return InjectableEntry(
            className = fqcn,
            factoryClassName = factoryFqcn,
            scope = scope,
            constructorParams = params
        )
    }
}
