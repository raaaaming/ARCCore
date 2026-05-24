package cc.arccore.reflection

object ReflectionUtil {

    fun findAnnotatedClasses(
        packageName: String,
        annotation: Class<out Annotation>
    ): List<Class<*>> {
        TODO("Implement package scanning with annotation filtering")
    }

    fun <T> instantiate(clazz: Class<T>): T {
        return clazz.getDeclaredConstructor().apply {
            trySetAccessible()
        }.newInstance()
    }

    private fun <T : java.lang.reflect.AccessibleObject> T.trySetAccessible(): T {
        try {
            isAccessible = true
        } catch (_: Exception) {
        }
        return this
    }
}
