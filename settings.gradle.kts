rootProject.name = "ARCCore"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

includeBuild("build-logic")

// ──────────────────────────────────────────────
// Framework Modules (grouped under modules/)
// ──────────────────────────────────────────────
include(":arc-api")
project(":arc-api").projectDir = file("modules/arc-api")

include(":arc-core")
project(":arc-core").projectDir = file("modules/arc-core")

include(":arc-loader")
project(":arc-loader").projectDir = file("modules/arc-loader")

include(":arc-runtime")
project(":arc-runtime").projectDir = file("modules/arc-runtime")

include(":arc-coroutine")
project(":arc-coroutine").projectDir = file("modules/arc-coroutine")

include(":arc-diagnostics")
project(":arc-diagnostics").projectDir = file("modules/arc-diagnostics")

include(":arc-command")
project(":arc-command").projectDir = file("modules/arc-command")

include(":arc-event")
project(":arc-event").projectDir = file("modules/arc-event")

include(":arc-di")
project(":arc-di").projectDir = file("modules/arc-di")

include(":arc-bootstrap")
project(":arc-bootstrap").projectDir = file("modules/arc-bootstrap")

include(":arc-eventbus")
project(":arc-eventbus").projectDir = file("modules/arc-eventbus")

include(":arc-storage")
project(":arc-storage").projectDir = file("modules/arc-storage")

include(":arc-config")
project(":arc-config").projectDir = file("modules/arc-config")

include(":arc-scheduler")
project(":arc-scheduler").projectDir = file("modules/arc-scheduler")

include(":arc-zero-downtime")
project(":arc-zero-downtime").projectDir = file("modules/arc-zero-downtime")

include(":arc-runtime-snapshot")
project(":arc-runtime-snapshot").projectDir = file("modules/arc-runtime-snapshot")

include(":arc-runtime-migration")
project(":arc-runtime-migration").projectDir = file("modules/arc-runtime-migration")

include(":arc-reflection")
project(":arc-reflection").projectDir = file("modules/arc-reflection")

include(":arc-ksp")
project(":arc-ksp").projectDir = file("modules/arc-ksp")

include(":arc-utils")
project(":arc-utils").projectDir = file("modules/arc-utils")

// ──────────────────────────────────────────────
// Standalone / External Modules
// ──────────────────────────────────────────────
include(":test-module")
