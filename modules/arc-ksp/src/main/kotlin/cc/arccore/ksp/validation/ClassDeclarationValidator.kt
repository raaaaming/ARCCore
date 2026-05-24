package cc.arccore.ksp.validation

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier

class ClassDeclarationValidator(private val logger: KSPLogger) {

    fun validate(
        decl: KSClassDeclaration,
        annotationLabel: String,
        requiredSupertype: String?
    ): Boolean {
        val fqn = decl.qualifiedName?.asString() ?: run {
            logger.error("@$annotationLabel: anonymous or unnamed class cannot be auto-registered", decl)
            return false
        }

        if (decl.classKind == ClassKind.INTERFACE) {
            logger.error("@$annotationLabel: '$fqn' must not be an interface", decl)
            return false
        }

        if (decl.classKind == ClassKind.OBJECT) {
            logger.error("@$annotationLabel: '$fqn' must not be a Kotlin object (singleton). Use a class.", decl)
            return false
        }

        if (Modifier.ABSTRACT in decl.modifiers) {
            logger.error("@$annotationLabel: '$fqn' must not be abstract", decl)
            return false
        }

        if (requiredSupertype != null && !hasDirectSupertype(decl, requiredSupertype)) {
            logger.error(
                "@$annotationLabel: '$fqn' must implement or extend '$requiredSupertype'",
                decl
            )
            return false
        }

        return true
    }

    private fun hasDirectSupertype(decl: KSClassDeclaration, targetFqn: String): Boolean {
        return decl.superTypes.any { ref ->
            ref.resolve().declaration.qualifiedName?.asString() == targetFqn
        }
    }
}
