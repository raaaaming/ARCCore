package cc.arccore.ksp.generated.injection.validation

import cc.arccore.ksp.model.InjectableEntry
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * KSP compile-time validator for @ArcComponent injectable classes.
 *
 * WHY compile-time validation matters:
 * - A circular dependency only surfaces at runtime with reflection DI, on the
 *   first request, in production. With KSP-generated DI, the cycle is caught
 *   during `./gradlew build` before any server starts.
 * - Missing dependencies, inaccessible constructors, and primitive parameters
 *   are caught immediately rather than throwing an obscure exception on the
 *   first player login.
 * - KSP errors include the source file and line number — far more actionable
 *   than a stack trace in server.log.
 */
class CompileTimeValidator(private val logger: KSPLogger) {

    /**
     * Validates a single class declaration before its factory is generated.
     */
    fun validateDeclaration(
        decl: KSClassDeclaration,
        injectConstructors: List<KSFunctionDeclaration>
    ): ValidationResult {
        val fqcn = decl.qualifiedName?.asString()
            ?: return ValidationResult.Error("Cannot determine class name")

        if (Modifier.ABSTRACT in decl.modifiers) {
            return ValidationResult.Error("'$fqcn' is abstract and cannot be instantiated directly.")
        }

        if (injectConstructors.size > 1) {
            return ValidationResult.Error(
                "'$fqcn' has ${injectConstructors.size} constructors annotated with @Inject. " +
                    "Only one @Inject constructor is allowed."
            )
        }

        // Pick constructor: @Inject-annotated one, or the primary constructor, or fail
        val target: KSFunctionDeclaration = when {
            injectConstructors.isNotEmpty() -> injectConstructors.first()
            decl.primaryConstructor != null -> decl.primaryConstructor!!
            else -> return ValidationResult.Error(
                "'$fqcn' has no primary constructor and no @Inject constructor. " +
                    "Add a primary constructor or annotate one with @Inject."
            )
        }

        for ((index, param) in target.parameters.withIndex()) {
            val paramName = param.name?.asString() ?: "param$index"
            val resolved = param.type.resolve()

            if (resolved.isError) {
                return ValidationResult.Error(
                    "'$fqcn': cannot resolve type of parameter '$paramName'. " +
                        "Ensure the dependency is on the compilation classpath."
                )
            }

            val typeFqcn = resolved.declaration.qualifiedName?.asString()
                ?: return ValidationResult.Error(
                    "'$fqcn': parameter '$paramName' has an unresolvable type. " +
                        "Only top-level or nested named classes can be injected."
                )

            if (typeFqcn in FORBIDDEN_PRIMITIVE_TYPES) {
                return ValidationResult.Error(
                    "'$fqcn': parameter '$paramName' is type '$typeFqcn'. " +
                        "Primitives and Strings cannot be injected via DI — wrap them in a config object."
                )
            }
        }

        return ValidationResult.Valid(target)
    }

    /**
     * Cross-entry validation: run after all @ArcComponent entries are collected.
     *
     * Returns true if all checks pass; false on error (errors are emitted via [logger]).
     */
    fun validateAll(entries: List<InjectableEntry>): Boolean {
        var valid = true

        for (cycle in detectCircularDependencies(entries)) {
            logger.error(
                "[ARCCore DI] Circular dependency detected: ${cycle.joinToString(" → ")}. " +
                    "Break the cycle by extracting an interface or using lazy injection."
            )
            valid = false
        }

        return valid
    }

    private fun detectCircularDependencies(entries: List<InjectableEntry>): List<List<String>> {
        val typeMap = entries.associateBy { it.className }
        val cycles = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()
        val stack = mutableListOf<String>()

        fun dfs(className: String) {
            if (className in inStack) {
                val start = stack.indexOf(className)
                if (start >= 0) cycles.add(stack.subList(start, stack.size).toList() + className)
                return
            }
            if (className in visited) return
            visited += className
            inStack += className
            stack += className
            typeMap[className]?.constructorParams?.forEach { param ->
                if (param.typeFqcn in typeMap) dfs(param.typeFqcn)
            }
            stack.removeLast()
            inStack -= className
        }

        entries.forEach { if (it.className !in visited) dfs(it.className) }
        return cycles
    }

    sealed class ValidationResult {
        data class Valid(val constructor: KSFunctionDeclaration) : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    companion object {
        private val FORBIDDEN_PRIMITIVE_TYPES = setOf(
            "kotlin.Byte", "kotlin.Short", "kotlin.Int", "kotlin.Long",
            "kotlin.Float", "kotlin.Double", "kotlin.Boolean", "kotlin.Char",
            "kotlin.String"
        )
    }
}
