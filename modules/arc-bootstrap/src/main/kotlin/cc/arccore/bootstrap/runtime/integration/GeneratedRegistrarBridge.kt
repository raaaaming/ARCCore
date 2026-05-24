package cc.arccore.bootstrap.runtime.integration

import cc.arccore.bootstrap.runtime.BootstrapContext
import cc.arccore.bootstrap.runtime.BootstrapContextKey

/**
 * Bridge to the KSP-generated ArcBootstrap class.
 *
 * Attempts to load `cc.arccore.generated.ArcBootstrap` via the module's ClassLoader.
 * The generated class must implement `cc.arccore.runtime.annotation.generated.GeneratedRegistrar`
 * and expose a no-arg constructor.
 *
 * Bukkit/Paper is fully isolated inside this class — bootstrap core never imports Plugin.
 * The [PluginHolder] interface provides the necessary Plugin reference without a direct import.
 */
class GeneratedRegistrarBridge(
    private val pluginHolder: PluginHolder
) {

    companion object {
        private const val BOOTSTRAP_CLASS = "cc.arccore.generated.ArcBootstrap"
        private const val REGISTRAR_INTERFACE = "cc.arccore.runtime.annotation.generated.GeneratedRegistrar"
    }

    /**
     * Attempts to load and invoke the generated ArcBootstrap registrar.
     * Returns the result indicating success, not found, or failure.
     */
    fun executeRegistrar(context: BootstrapContext): BootstrapRegistrarResult {
        val classLoader = context.classLoader

        val clazz = try {
            classLoader.loadClass(BOOTSTRAP_CLASS)
        } catch (_: ClassNotFoundException) {
            return BootstrapRegistrarResult.NotFound(
                reason = "Generated class '$BOOTSTRAP_CLASS' not found in classloader for '${context.moduleId}'"
            )
        }

        val instance = try {
            clazz.getDeclaredConstructor().newInstance()
        } catch (e: Exception) {
            return BootstrapRegistrarResult.Failed(
                cause = e,
                message = "Failed to instantiate '$BOOTSTRAP_CLASS': ${e.message}"
            )
        }

        return try {
            // Invoke via reflection — avoids compile-time arc-runtime dependency
            val registerMethod = clazz.getDeclaredMethod(
                "register",
                classLoader.loadClass("cc.arccore.runtime.context.RuntimeModuleContext"),
                classLoader.loadClass("org.bukkit.plugin.Plugin")
            )
            val runtimeContext = context.runtimeContext
                ?: return BootstrapRegistrarResult.Failed(
                    cause = null,
                    message = "RuntimeModuleContext not available in BootstrapContext for '${context.moduleId}'"
                )

            registerMethod.invoke(instance, runtimeContext, pluginHolder.providePlugin())
            val result = BootstrapRegistrarResult.Success(
                registeredClassName = BOOTSTRAP_CLASS,
                moduleId = context.moduleId
            )
            context.put(BootstrapContextKey.REGISTRAR_RESULT, result)
            result
        } catch (e: NoSuchMethodException) {
            BootstrapRegistrarResult.Failed(
                cause = e,
                message = "'$BOOTSTRAP_CLASS' does not expose the expected register(RuntimeModuleContext, Plugin) method"
            )
        } catch (e: Exception) {
            BootstrapRegistrarResult.Failed(
                cause = e,
                message = "register() invocation failed for '${context.moduleId}': ${e.message}"
            )
        }
    }

    sealed class BootstrapRegistrarResult {
        data class Success(
            val registeredClassName: String,
            val moduleId: String
        ) : BootstrapRegistrarResult()

        data class NotFound(val reason: String) : BootstrapRegistrarResult()

        data class Failed(
            val cause: Throwable?,
            val message: String
        ) : BootstrapRegistrarResult()

        val isSuccess: Boolean get() = this is Success
        val isNotFound: Boolean get() = this is NotFound
        val isFailed: Boolean get() = this is Failed
    }

    /**
     * Provides access to the Bukkit Plugin instance without introducing a direct
     * arc-bootstrap -> Paper dependency. Implemented by arc-core.
     */
    interface PluginHolder {
        fun providePlugin(): Any
    }
}
