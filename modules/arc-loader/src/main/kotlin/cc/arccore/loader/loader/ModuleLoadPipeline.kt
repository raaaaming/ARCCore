package cc.arccore.loader.loader

import cc.arccore.api.ArcAPI
import cc.arccore.api.module.ModuleContainer
import cc.arccore.api.module.ModuleContext
import cc.arccore.api.module.ModuleDescription
import cc.arccore.loader.ModuleClassLoader
import cc.arccore.loader.classloader.ClassLoaderStrategy
import cc.arccore.loader.classloader.SharedClassFilter
import cc.arccore.loader.loader.exception.DuplicateModuleException
import cc.arccore.loader.loader.exception.ModuleInstantiationException
import cc.arccore.loader.metadata.ModuleMetadataReader
import cc.arccore.loader.metadata.exception.MetadataException
import java.net.URL
import java.nio.file.Path
import java.util.logging.Logger

class ModuleLoadPipeline(
    private val arcAPI: ArcAPI,
    private val metadataReader: ModuleMetadataReader,
    private val parentClassLoader: ClassLoader,
    private val modulesDataFolder: Path,
    private val registry: ModuleRegistry,
    private val contextFactory: ModuleContextFactory = SimpleModuleContextFactory(),
    private val pluginClassLoaderResolver: (String) -> ClassLoader? = { null }
) {
    private val log = Logger.getLogger(ModuleLoadPipeline::class.java.name)
    private val discovery = ModuleDiscovery()
    private val instantiation = ModuleInstantiation()

    fun loadAll(directory: Path): List<ModuleLoadResult> {
        discovery.ensureModulesDirectory(directory)
        val jarFiles = discovery.discover(directory)
        if (jarFiles.isEmpty()) {
            log.info("No module jars found in $directory")
            return emptyList()
        }
        log.info("Discovered ${jarFiles.size} module jar(s) in $directory")
        return jarFiles.map { jarPath -> loadSingle(jarPath) }
    }

    fun loadSingle(jarPath: Path): ModuleLoadResult {
        val fileName = jarPath.fileName.toString()
        log.info("Processing module jar: $fileName")

        val description: ModuleDescription = try {
            metadataReader.read(jarPath)
        } catch (e: MetadataException) {
            log.severe("Metadata error in $fileName: ${e.message}")
            return ModuleLoadResult.Failure(jarPath, e)
        } catch (e: Exception) {
            log.severe("Unexpected error reading metadata from $fileName: ${e.message}")
            return ModuleLoadResult.Failure(jarPath, e)
        }

        if (registry.contains(description.id)) {
            val existing = registry.get(description.id)!!
            val error = DuplicateModuleException(
                moduleId = description.id,
                existingJar = existing.description.name,
                duplicateJar = fileName
            )
            log.severe(error.message)
            return ModuleLoadResult.Failure(jarPath, error, description.id)
        }

        val classLoader = createClassLoader(description, jarPath, fileName)
            ?: return ModuleLoadResult.Failure(
                jarPath,
                RuntimeException("Failed to create ModuleClassLoader for '${description.id}'"),
                description.id
            )

        val container = loadAndInstantiate(classLoader, description, jarPath, fileName)
            ?: return ModuleLoadResult.Failure(
                jarPath,
                RuntimeException("Failed to instantiate module '${description.id}'"),
                description.id
            )

        log.info("Successfully loaded module '${description.id}' from $fileName")
        return ModuleLoadResult.Success(jarPath, container)
    }

    private fun createClassLoader(
        description: ModuleDescription,
        jarPath: Path,
        fileName: String
    ): ModuleClassLoader? {
        val urls = try {
            arrayOf(jarPath.toUri().toURL())
        } catch (e: Exception) {
            log.severe("Invalid jar URL for $fileName: ${e.message}")
            return null
        }

        val pluginClassLoaders = description.dependPlugins.mapNotNull { pluginName ->
            val loader = pluginClassLoaderResolver(pluginName)
            if (loader == null) {
                log.warning("Module '${description.id}' declares dependPlugins '$pluginName' but it is not loaded")
            }
            loader
        }

        return try {
            ModuleClassLoader(
                moduleId = description.id,
                urls = urls,
                parent = parentClassLoader,
                strategy = ClassLoaderStrategy.HYBRID,
                sharedFilter = SharedClassFilter.default(),
                pluginClassLoaders = pluginClassLoaders
            )
        } catch (e: Exception) {
            log.severe("Failed to create ClassLoader for module '${description.id}': ${e.message}")
            null
        }
    }

    private fun loadAndInstantiate(
        classLoader: ModuleClassLoader,
        description: ModuleDescription,
        jarPath: Path,
        fileName: String
    ): ModuleContainer? {
        val mainClass = try {
            instantiation.loadMainClass(classLoader, description)
        } catch (e: ModuleInstantiationException) {
            log.severe("Failed to load main class for module '${description.id}': ${e.message}")
            closeSafely(classLoader)
            return null
        }

        val instance = try {
            instantiation.createInstance(mainClass, description)
        } catch (e: ModuleInstantiationException) {
            log.severe("Failed to instantiate module '${description.id}': ${e.message}")
            closeSafely(classLoader)
            return null
        }

        val container = ModuleContainer(instance)

        val moduleDataFolder = modulesDataFolder.resolve(description.id)

        val context: ModuleContext = contextFactory.create(
            api = arcAPI,
            module = instance,
            description = description,
            dataFolder = moduleDataFolder,
            classLoader = classLoader,
            owner = container
        )

        try {
            instance.onLoad(context)
        } catch (e: Exception) {
            log.severe("Module '${description.id}' threw exception during onLoad(): ${e.message}")
            runCatching { context.cleanupScope.close() }
            closeSafely(classLoader)
            container.transitionToFailed(e)
            return null
        }

        container.transitionToLoad(context)
        registry.register(container)
        return container
    }

    private fun closeSafely(classLoader: ModuleClassLoader) {
        try {
            classLoader.close()
        } catch (e: Exception) {
            log.warning("Failed to close ModuleClassLoader: ${e.message}")
        }
    }
}
