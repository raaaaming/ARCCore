package cc.arccore.ksp

import cc.arccore.ksp.generator.MetadataJsonGenerator
import cc.arccore.ksp.output.ResourceFileWriter
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

class ArcModuleProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    companion object {
        private const val ARC_MODULE_ANNOTATION = "cc.arccore.api.module.ModuleSpec"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ARC_MODULE_ANNOTATION)
        val deferred = mutableListOf<KSAnnotated>()

        symbols.filterIsInstance<KSClassDeclaration>().forEach { decl ->
            try {
                processModuleAnnotation(decl)
            } catch (e: Exception) {
                logger.error("Failed to process ${decl.qualifiedName?.asString()}: ${e.message}")
                deferred.add(decl)
            }
        }

        return deferred
    }

    private fun processModuleAnnotation(decl: KSClassDeclaration) {
        val packageName = decl.packageName.asString()
        val simpleName = decl.simpleName.asString()
        val qualifiedName = decl.qualifiedName?.asString()
            ?: error("Unresolvable qualified name for $simpleName")
        val className = ClassName(packageName, simpleName)

        val annotation = decl.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == ARC_MODULE_ANNOTATION
        }
        val args = annotation?.arguments ?: emptyList()
        fun arg(name: String) = args.find { it.name?.asString() == name }?.value as? String ?: ""

        val id = arg("id")
        val name = arg("name").ifBlank { id }
        val version = arg("version").ifBlank { "1.0.0" }
        val description = arg("description")

        val manifestJson = MetadataJsonGenerator.generateModuleManifest(
            id = id,
            name = name,
            version = version,
            mainClass = qualifiedName,
            description = description
        )

        ResourceFileWriter.write(
            codeGenerator = codeGenerator,
            path = "META-INF/arc-module",
            extensionName = "json",
            content = manifestJson,
            dependencies = Dependencies(aggregating = false, decl.containingFile!!)
        )

        val spec = FileSpec.builder(packageName, "${simpleName}Meta")
            .addType(
                TypeSpec.objectBuilder("${simpleName}Meta")
                    .addFunction(
                        FunSpec.builder("createInstance")
                            .returns(className)
                            .addStatement("return %T()", className)
                            .build()
                    )
                    .build()
            )
            .build()

        spec.writeTo(codeGenerator, aggregating = false)
    }
}

class ArcModuleProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ArcModuleProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}
