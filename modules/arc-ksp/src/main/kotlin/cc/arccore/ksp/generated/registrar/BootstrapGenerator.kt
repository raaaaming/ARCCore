package cc.arccore.ksp.generated.registrar

import cc.arccore.ksp.exception.BootstrapGenerationException
import cc.arccore.ksp.model.CommandEntry
import cc.arccore.ksp.model.ConstructorParam
import cc.arccore.ksp.model.ListenerEntry
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

object BootstrapGenerator {

    private const val GENERATED_PACKAGE = "cc.arccore.generated"
    private const val BOOTSTRAP_CLASS = "ArcBootstrap"

    private val registrarInterface = ClassName("cc.arccore.runtime.annotation.generated", "GeneratedRegistrar")
    private val contextType = ClassName("cc.arccore.runtime.context", "RuntimeModuleContext")
    private val pluginType = ClassName("org.bukkit.plugin", "Plugin")
    private val arcCommandType = ClassName("cc.arccore.api.command", "ARCCommand")
    private val commandMetadataType = ClassName("cc.arccore.api.command", "CommandMetadata")

    fun generate(
        codeGenerator: CodeGenerator,
        logger: KSPLogger,
        commands: List<CommandEntry>,
        listeners: List<ListenerEntry>,
        originatingFiles: List<KSFile>
    ) {
        try {
            val registerFn = buildRegisterFunction(commands, listeners)
            val bootstrapClass = TypeSpec.classBuilder(BOOTSTRAP_CLASS)
                .addSuperinterface(registrarInterface)
                .addFunction(registerFn)
                .build()

            FileSpec.builder(GENERATED_PACKAGE, BOOTSTRAP_CLASS)
                .addType(bootstrapClass)
                .build()
                .writeTo(codeGenerator, aggregating = true, originatingKSFiles = originatingFiles)

            logger.info("Generated $GENERATED_PACKAGE.$BOOTSTRAP_CLASS (${commands.size} commands, ${listeners.size} listeners)")
        } catch (e: Exception) {
            throw BootstrapGenerationException("Failed to generate $BOOTSTRAP_CLASS", e)
        }
    }

    private fun buildRegisterFunction(commands: List<CommandEntry>, listeners: List<ListenerEntry>): FunSpec {
        return FunSpec.builder("register")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("context", contextType)
            .addParameter("plugin", pluginType)
            .apply {
                commands.forEach { entry ->
                    addCode(buildCommandRegister(entry))
                }
                listeners.forEach { entry ->
                    val (pkg, simple) = splitClassName(entry.className)
                    val instance = buildInstance(ClassName(pkg, simple), entry.constructorParams)
                    addStatement("context.listeners.register(%L, plugin)", instance)
                }
            }
            .build()
    }

    private fun buildCommandRegister(entry: CommandEntry): CodeBlock {
        val (pkg, simple) = splitClassName(entry.className)
        val cmdClass = ClassName(pkg, simple)
        val delegateExpr = buildInstance(cmdClass, entry.constructorParams)

        val hasSpec = entry.commandName.isNotBlank()

        return if (hasSpec) {
            val aliasesLiteral = if (entry.commandAliases.isEmpty()) {
                "emptyList()"
            } else {
                "listOf(${entry.commandAliases.joinToString(", ") { "\"$it\"" }})"
            }
            val usageLiteral = entry.commandUsage.ifBlank { "/${entry.commandName}" }
            val permissionLiteral = if (entry.commandPermission.isBlank()) "null" else "\"${entry.commandPermission}\""

            CodeBlock.builder().apply {
                add("context.commands.register(object : %T by %L {\n", arcCommandType, delegateExpr)
                indent()
                add("override val metadata = %T(\n", commandMetadataType)
                indent()
                add("name = %S,\n", entry.commandName)
                add("aliases = %L,\n", aliasesLiteral)
                add("permission = %L,\n", permissionLiteral)
                add("description = %S,\n", entry.commandDescription)
                add("usage = %S\n", usageLiteral)
                unindent()
                add(")\n")
                unindent()
                add("})\n")
            }.build()
        } else {
            // @CommandSpec 없음 — delegate만 등록 (runtime에서 metadata 직접 구현 필요)
            CodeBlock.of("context.commands.register(%L)\n", delegateExpr)
        }
    }

    private fun buildInstance(className: ClassName, params: List<ConstructorParam>): CodeBlock {
        return CodeBlock.builder().apply {
            add("%T(", className)
            params.forEachIndexed { i, param ->
                if (i > 0) add(", ")
                val (pkg, simple) = splitClassName(param.typeFqn)
                val paramClass = ClassName(pkg, simple)
                if (param.nullable) {
                    add("context.services.get(%T::class)", paramClass)
                } else {
                    add("context.services.require(%T::class)", paramClass)
                }
            }
            add(")")
        }.build()
    }

    private fun splitClassName(fqn: String): Pair<String, String> {
        val idx = fqn.lastIndexOf('.')
        return if (idx < 0) Pair("", fqn) else Pair(fqn.substring(0, idx), fqn.substring(idx + 1))
    }
}
