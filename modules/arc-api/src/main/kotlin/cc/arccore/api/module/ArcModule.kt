package cc.arccore.api.module

/**
 * Declares a class as an ARC module and supplies its metadata.
 *
 * KSP processes this at compile time to generate `META-INF/arc-module.json`.
 *
 * @property dependencies Dependencies on other ARC modules **and** external Bukkit plugins,
 *   declared together in one list:
 *   - ARC module: `"economy-module"` or `"economy-module:>=1.2.0"` (with version range),
 *     `"discount-module?"` (optional), `"other-module@before"` / `"@after"` (load order).
 *   - Bukkit plugin: append the `@plugin` suffix, e.g. `"Vault@plugin"`. ARCCore wires the
 *     plugin's classloader into this module so its classes are directly accessible.
 * @property libraries Maven coordinates (`groupId:artifactId:version`) auto-downloaded from
 *   Maven Central on load, e.g. `"org.xerial:sqlite-jdbc:3.43.0"`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ModuleSpec(
    val id: String,
    val name: String = "",
    val version: String = "1.0.0",
    val description: String = "",
    val authors: Array<String> = [],
    val dependencies: Array<String> = [],
    val libraries: Array<String> = []
)
