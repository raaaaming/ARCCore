package cc.arccore.api.module

/**
 * Implemented by [ModuleContext] impls that hold a module-specific ClassLoader.
 * Uses base [ClassLoader] to avoid introducing arc-loader dependency in arc-api.
 */
interface ClassLoaderHolder {
    fun provideClassLoader(): ClassLoader?
}
