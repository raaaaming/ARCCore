plugins {
    id("arc.module")
}

dependencies {
    // arc.module convention plugin already provides api(":arc-api")

    compileOnly(project(":arc-diagnostics"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

description = "ARCCore Storage Runtime — platform-managed config/file/cache/database storage for modules"
