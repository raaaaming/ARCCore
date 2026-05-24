package cc.arccore.ksp.generated.injection

import cc.arccore.ksp.model.InjectableEntry
import cc.arccore.ksp.output.ResourceFileWriter
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile

/**
 * Generates `META-INF/arc/generated/injectors.list` — the index file read by
 * [cc.arccore.di.generated.injector.InjectorLoader] at module startup.
 *
 * Format: one factory class FQCN per line, comment lines start with `#`.
 * Using a flat text file (not JSON) avoids a JSON-parsing dependency in arc-di
 * and keeps the format trivially human-readable for debugging.
 *
 * The file is aggregating (changes in ANY @ArcComponent class regenerate it),
 * which is correct: removing an @ArcComponent class must remove its entry.
 *
 * WHY store class names rather than the object graph itself:
 * - The graph topology is encoded in the generated factories (each factory
 *   calls `context.resolve()` for its dependencies). Duplicating that topology
 *   in a JSON graph would be fragile and add a separate parsing step.
 * - [InjectorLoader] just needs to know which factory classes to instantiate;
 *   the runtime graph is built by registering those factories into
 *   [cc.arccore.di.generated.injector.GeneratedObjectGraph].
 */
object GraphGenerator {

    private const val LIST_RESOURCE = "META-INF/arc/generated/injectors"

    fun generate(
        codeGenerator: CodeGenerator,
        entries: List<InjectableEntry>,
        originatingFiles: List<KSFile>
    ) {
        if (entries.isEmpty()) return

        val content = buildString {
            appendLine("# ARCCore Generated Injectors — regenerated on every build, do not edit.")
            entries.forEach { entry ->
                appendLine(FactoryGenerator.factoryFqcn(entry))
            }
        }

        ResourceFileWriter.write(
            codeGenerator,
            LIST_RESOURCE,
            "list",
            content,
            Dependencies(aggregating = true, *originatingFiles.toTypedArray())
        )
    }
}
