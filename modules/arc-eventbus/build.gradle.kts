plugins {
    id("arc.module")
}

dependencies {
    // arc-api is provided by the arc.module convention plugin

    // arc-diagnostics: optional integration port
    compileOnly(project(":arc-diagnostics"))

    // kotlinx-coroutines: optional async dispatch (CoroutineEventDispatcher)
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

description = "ARCCore Internal Event Bus — lifecycle-aware module messaging runtime"
