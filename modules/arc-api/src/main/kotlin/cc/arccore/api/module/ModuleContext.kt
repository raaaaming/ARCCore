package cc.arccore.api.module

import cc.arccore.api.ArcAPI
import java.nio.file.Path

/**
 * Runtime context provided to a module during [ArcModuleAPI.onLoad].
 *
 * This is the primary mechanism for modules to interact with the platform.
 * Modules **must not** access Bukkit/Paper singletons directly; everything
 * is obtained through this context.
 */
interface ModuleContext {

    /** The core ARCCore platform API. */
    val api: ArcAPI

    /** The module instance that owns this context. */
    val module: ArcModuleAPI

    /** Logger scoped to this module. */
    val logger: ModuleLogger

    /** Platform-resolved data directory for this module's persistent storage. */
    val dataFolder: Path

    /** Immutable descriptor with this module's metadata. */
    val description: ModuleDescription

    /** The current lifecycle state of this module. */
    val state: ModuleState

    /**
     * 모듈 언로드 시 자동으로 해제되는 리소스 범위.
     * Bukkit 리스너, 스케줄러 Task 등을 여기에 등록하면 별도 onDisable/onUnload
     * 구현 없이도 안전하게 해제된다.
     *
     * @since 1.0.0-Beta
     */
    val cleanupScope: CleanupScope

    /**
     * Storage runtime for this module.
     *
     * Provides access to config, file, cache, and database storage handles.
     * Defaults to [StorageRuntime.NOOP] when no storage provider is configured.
     *
     * @since 1.0.0-Beta
     */
    val storage: StorageRuntime

    /**
     * Config runtime for this module.
     *
     * Provides typed configuration loading, validation, hot reload, and lifecycle-aware cleanup.
     * Defaults to [ConfigRuntimeMarker.NOOP] when no config provider is configured.
     *
     * @since 1.0.0-Beta
     */
    val config: ConfigRuntimeMarker

    /**
     * Retrieves a platform service by its interface class.
     *
     * @param T The service type.
     * @param type The [Class] object for the service interface.
     * @return The service instance, or `null` if no provider is registered.
     */
    fun <T : Any> getService(type: Class<T>): T?
}
