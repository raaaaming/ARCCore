package cc.arccore.loader

import cc.arccore.loader.classloader.ClassLoaderStrategy
import cc.arccore.loader.classloader.ModuleClassVisibility
import cc.arccore.loader.classloader.ModuleResourceLoader
import cc.arccore.loader.classloader.SharedClassFilter
import cc.arccore.loader.classloader.delegation.DelegationContext
import cc.arccore.loader.classloader.delegation.DelegationPolicy
import cc.arccore.loader.classloader.delegation.DelegationResult
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.util.Collections
import java.util.Enumeration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

class ModuleClassLoader
@JvmOverloads constructor(
    val moduleId: String,
    urls: Array<URL>,
    parent: ClassLoader,
    private val strategy: ClassLoaderStrategy = ClassLoaderStrategy.HYBRID,
    private val sharedFilter: SharedClassFilter = SharedClassFilter.default(),
    private val visibility: ModuleClassVisibility = ModuleClassVisibility.noneExported(moduleId),
    private val dependencyClassLoaders: List<ModuleClassLoader> = emptyList(),
    private val pluginClassLoaders: List<ClassLoader> = emptyList()
) : URLClassLoader("arc-module:$moduleId", urls, parent) {

    companion object {
        private const val MAX_DELEGATION_DEPTH = 64
        private val DELEGATION_DEPTH = ThreadLocal.withInitial { 0 }
        private val LOG: Logger = Logger.getLogger(ModuleClassLoader::class.java.name)

        init {
            registerAsParallelCapable()
        }
    }

    private val loadedModuleClasses: MutableSet<Class<*>> =
        Collections.newSetFromMap(ConcurrentHashMap<Class<*>, Boolean>())

    private val closed = AtomicBoolean(false)

    private val depLoaders: List<ModuleClassLoader> = dependencyClassLoaders.toList()

    private val pluginLoaders: List<ClassLoader> = pluginClassLoaders.toList()

    private val delegationPolicy: DelegationPolicy = when (strategy) {
        ClassLoaderStrategy.PARENT_FIRST -> DelegationPolicy.parentFirst()
        ClassLoaderStrategy.CHILD_FIRST -> DelegationPolicy.childFirst()
        ClassLoaderStrategy.HYBRID -> DelegationPolicy.hybrid { className ->
            sharedFilter.isShared(className)
        }
    }

    private val resourceLoader = ModuleResourceLoader(
        classLoader = this,
        parent = parent,
        sharedFilter = sharedFilter,
        strategy = strategy,
        dependencyClassLoaders = depLoaders
    )

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        val depth = DELEGATION_DEPTH.get()
        if (depth >= MAX_DELEGATION_DEPTH) {
            throw ClassNotFoundException(
                "Max delegation depth exceeded for class '$name' in ModuleClassLoader[$moduleId]"
            )
        }
        DELEGATION_DEPTH.set(depth + 1)
        try {
            return loadClassInternal(name, resolve)
        } finally {
            DELEGATION_DEPTH.set(depth)
        }
    }

    private fun loadClassInternal(name: String, resolve: Boolean): Class<*> {
        if (closed.get()) {
            throw ClassNotFoundException("ModuleClassLoader[$moduleId] is closed: $name")
        }

        synchronized(getClassLoadingLock(name)) {
            var c = findLoadedClass(name)
            if (c != null) {
                if (resolve) resolveClass(c)
                return c
            }

            c = resolveFromDelegationPolicy(name)

            if (c == null) {
                throw ClassNotFoundException("$name not found in ModuleClassLoader[$moduleId]")
            }

            loadedModuleClasses.add(c)

            if (resolve) resolveClass(c)
            return c
        }
    }

    private fun resolveFromDelegationPolicy(name: String): Class<*>? {
        val context = DelegationContext(
            className = name,
            selfLoader = this,
            parentLoader = parent,
            dependencyLoaders = depLoaders,
            pluginClassLoaders = pluginLoaders,
            findClassFromSelf = { findClass(it) },
            findClassFromParent = { parent.loadClass(it) }
        )

        return when (val result = delegationPolicy.resolveClass(context)) {
            is DelegationResult.Found -> {
                val clazz = result.clazz
                if (!isClassAccessible(name, clazz)) {
                    throw ClassNotFoundException(
                        "$name not accessible from ModuleClassLoader[$moduleId]: " +
                            "class is hidden by module visibility policy"
                    )
                }
                clazz
            }
            is DelegationResult.NotFound -> null
        }
    }

    private fun isClassAccessible(name: String, clazz: Class<*>): Boolean {
        val definingLoader = clazz.classLoader
        if (definingLoader == null) return true
        if (definingLoader === this) return true
        if (definingLoader === parent) return true
        if (sharedFilter.isShared(name)) return true

        if (definingLoader is ModuleClassLoader) {
            val depVisibility = definingLoader.visibility
            return depVisibility.isExported(name)
        }

        if (definingLoader in pluginLoaders) return true

        return false
    }

    override fun getResource(name: String): URL? {
        synchronized(getClassLoadingLock("__resource__$name")) {
            if (closed.get()) return null
            return resourceLoader.getResource(name)
        }
    }

    override fun getResources(name: String): Enumeration<URL> {
        synchronized(getClassLoadingLock("__resources__$name")) {
            if (closed.get()) return Collections.emptyEnumeration()
            return resourceLoader.getResources(name)
        }
    }

    override fun addURL(url: URL) {
        if (closed.get()) return
        synchronized(getClassLoadingLock("__addurl__${url.hashCode()}")) {
            super.addURL(url)
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        loadedModuleClasses.clear()
        try {
            super.close()
        } catch (e: IOException) {
            LOG.warning("IOException while closing ModuleClassLoader[$moduleId]: ${e.message}")
        }
    }

    fun isClosed(): Boolean = closed.get()

    fun getLoadedModuleClasses(): Set<Class<*>> = loadedModuleClasses.toSet()

    fun <T> withThreadContext(action: () -> T): T {
        val previous = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = this
        try {
            return action()
        } finally {
            Thread.currentThread().contextClassLoader = previous
        }
    }

    fun getDependencyLoaders(): List<ModuleClassLoader> = depLoaders

    override fun toString(): String {
        return "ModuleClassLoader{id='$moduleId', strategy=$strategy, " +
            "closed=${closed.get()}, deps=${depLoaders.size}}"
    }
}
