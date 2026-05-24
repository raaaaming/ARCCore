plugins {
    id("arc.module")
}

dependencies {
    implementation(project(":arc-runtime"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

description = "ARCCore Coroutine Runtime — lifecycle-aware structured concurrency for modules"
