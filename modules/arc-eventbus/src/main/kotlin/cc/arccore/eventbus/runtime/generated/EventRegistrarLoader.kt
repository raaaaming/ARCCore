package cc.arccore.eventbus.runtime.generated

import cc.arccore.eventbus.runtime.exception.EventBusException
import cc.arccore.eventbus.runtime.exception.InvalidSubscriptionException

/**
 * Loads the KSP-generated [GeneratedEventRegistrar] at runtime via reflection.
 *
 * The generated class is expected at `cc.arccore.generated.ArcEventRegistrar`.
 * If the class is not found (no KSP generation occurred), this returns null.
 *
 * Usage:
 * ```kotlin
 * val registrar = EventRegistrarLoader.load(module.classLoader)
 * registrar?.register(eventBus, moduleId)
 * ```
 */
object EventRegistrarLoader {

    private const val REGISTRAR_CLASS = "cc.arccore.generated.ArcEventRegistrar"

    /**
     * Attempts to load the generated event registrar from [classLoader].
     *
     * @param classLoader The class loader to use (typically the module's class loader).
     * @return The registrar instance, or null if the generated class was not found.
     * @throws InvalidSubscriptionException if the generated class exists but does not implement
     *   [GeneratedEventRegistrar] (stale or mismatched generated class).
     * @throws EventBusException if the class exists but cannot be instantiated.
     */
    fun load(classLoader: ClassLoader): GeneratedEventRegistrar? {
        return try {
            val clazz = classLoader.loadClass(REGISTRAR_CLASS)
            clazz.getDeclaredConstructor().newInstance() as GeneratedEventRegistrar
        } catch (_: ClassNotFoundException) {
            null
        } catch (e: ClassCastException) {
            throw InvalidSubscriptionException(
                "system",
                "'$REGISTRAR_CLASS' does not implement GeneratedEventRegistrar — " +
                    "stale or mismatched generated class: ${e.message}",
                e
            )
        } catch (e: Exception) {
            throw EventBusException(
                "Failed to load generated event registrar '$REGISTRAR_CLASS': ${e.message}",
                e
            )
        }
    }
}
