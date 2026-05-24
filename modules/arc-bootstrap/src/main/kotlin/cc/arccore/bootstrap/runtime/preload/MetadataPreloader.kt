package cc.arccore.bootstrap.runtime.preload

import cc.arccore.api.module.ModuleDescription
import cc.arccore.bootstrap.runtime.BootstrapContext
import cc.arccore.bootstrap.runtime.BootstrapContextKey
import cc.arccore.bootstrap.runtime.BootstrapPhase
import cc.arccore.bootstrap.runtime.scheduling.BootstrapPhaseHandler
import cc.arccore.bootstrap.runtime.state.BootstrapPhaseResult

/**
 * Handles the METADATA_PRELOAD phase.
 *
 * Loads:
 * 1. META-INF/arc/module.json  — module descriptor
 * 2. META-INF/arc/arc-annotation-metadata.json — KSP-generated annotation index
 * 3. cc.arccore.generated.ArcBootstrap class presence check — generated artifact manifest
 * 4. META-INF/arc/injectors.list — generated injector list
 */
class MetadataPreloader : BootstrapPhaseHandler {

    override val phase: BootstrapPhase = BootstrapPhase.METADATA_PRELOAD

    companion object {
        private const val MODULE_JSON_PATH = "META-INF/arc/module.json"
        private const val INJECTORS_LIST_PATH = "META-INF/arc/injectors.list"
        private const val ARC_BOOTSTRAP_CLASS = "cc.arccore.generated.ArcBootstrap"
    }

    private val annotationMetadataLoader = AnnotationMetadataLoader()

    override fun handle(context: BootstrapContext): BootstrapPhaseResult {
        val startNanos = System.nanoTime()

        return try {
            val classLoader = context.classLoader
            val notes = mutableListOf<String>()

            // 1. Load module.json
            val rawModuleJson = loadModuleJson(classLoader)
            if (rawModuleJson != null) notes.add("module.json loaded")

            // 2. Load annotation metadata
            val annotationCache = annotationMetadataLoader.load(classLoader)
            if (!annotationCache.isEmpty()) {
                notes.add("annotation metadata loaded: ${annotationCache.totalAnnotatedClassCount()} annotated classes")
            }

            // 3. Check for ArcBootstrap class
            val hasArcBootstrap = hasClass(classLoader, ARC_BOOTSTRAP_CLASS)
            if (hasArcBootstrap) notes.add("ArcBootstrap class found")

            // 4. Check for injectors.list
            val injectorClassNames = loadInjectorsList(classLoader)
            val hasInjectorsList = injectorClassNames.isNotEmpty()
            if (hasInjectorsList) notes.add("injectors.list found: ${injectorClassNames.size} injectors")

            val hasAnnotationMetadata = !annotationCache.isEmpty()

            val manifest = GeneratedArtifactManifest(
                hasArcBootstrapClass = hasArcBootstrap,
                hasInjectorsList = hasInjectorsList,
                hasAnnotationMetadata = hasAnnotationMetadata,
                arcBootstrapClassName = if (hasArcBootstrap) ARC_BOOTSTRAP_CLASS else null,
                injectorClassNames = injectorClassNames,
                metadataVersion = 1,
                generatedAt = null
            )

            val description = resolveDescription(context, rawModuleJson)

            val preloaded = PreloadedModuleMetadata(
                moduleId = context.moduleId,
                description = description,
                artifactManifest = manifest,
                annotationCache = annotationCache,
                rawModuleJson = rawModuleJson
            )

            context.put(BootstrapContextKey.PRELOADED_METADATA, preloaded)

            BootstrapPhaseResult.Success(
                phase = phase,
                durationNanos = System.nanoTime() - startNanos,
                notes = notes
            )
        } catch (e: Exception) {
            BootstrapPhaseResult.Failure(
                phase = phase,
                durationNanos = System.nanoTime() - startNanos,
                cause = e
            )
        }
    }

    private fun loadModuleJson(classLoader: ClassLoader): String? =
        classLoader.getResourceAsStream(MODULE_JSON_PATH)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText().takeIf { it.isNotBlank() }
        }

    private fun loadInjectorsList(classLoader: ClassLoader): List<String> {
        val stream = classLoader.getResourceAsStream(INJECTORS_LIST_PATH) ?: return emptyList()
        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readLines().map { it.trim() }.filter { it.isNotBlank() }
        }
    }

    private fun hasClass(classLoader: ClassLoader, className: String): Boolean =
        try {
            classLoader.loadClass(className)
            true
        } catch (_: ClassNotFoundException) {
            false
        }

    private fun resolveDescription(context: BootstrapContext, rawJson: String?): ModuleDescription {
        // Use context's existing description, or parse from JSON if available
        if (rawJson != null) {
            return try {
                ModuleDescription.fromJson(rawJson)
            } catch (_: Exception) {
                context.description
            }
        }
        return context.description
    }
}
