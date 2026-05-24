package cc.arccore.runtime.annotation.generated

import cc.arccore.runtime.annotation.generated.exception.RegistrarLoadException

object RuntimeRegistrarLoader {

    private const val BOOTSTRAP_CLASS = "cc.arccore.generated.ArcBootstrap"

    fun load(classLoader: ClassLoader): GeneratedRegistrar? {
        return try {
            val clazz = classLoader.loadClass(BOOTSTRAP_CLASS)
            @Suppress("UNCHECKED_CAST")
            clazz.getDeclaredConstructor().newInstance() as GeneratedRegistrar
        } catch (_: ClassNotFoundException) {
            null
        } catch (e: ClassCastException) {
            throw RegistrarLoadException(
                "'$BOOTSTRAP_CLASS' does not implement GeneratedRegistrar — stale or mismatched generated class", e
            )
        } catch (e: Exception) {
            throw RegistrarLoadException("Failed to instantiate generated bootstrap '$BOOTSTRAP_CLASS'", e)
        }
    }
}
