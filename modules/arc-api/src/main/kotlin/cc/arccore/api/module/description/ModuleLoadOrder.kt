package cc.arccore.api.module.description

/**
 * Defines how a dependency affects module load ordering.
 *
 * - [NORMAL]: Standard dependency — the dependent module loads after this dependency.
 * - [BEFORE]: The dependency should load _before_ the declaring module
 *   (equivalent to putting the declaring module in the dependency's `loadBefore`).
 * - [AFTER]:  The dependency should load _after_ the declaring module
 *   (the declaring module waits for this dependency to be present).
 */
enum class ModuleLoadOrder {
    NORMAL,
    BEFORE,
    AFTER
}
