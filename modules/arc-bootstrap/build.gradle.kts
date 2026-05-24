plugins {
    id("arc.module")
}

dependencies {
    // arc-api only — bootstrap core must not pull Paper/Bukkit transitively
    implementation(project(":arc-api"))

    // arc-diagnostics is optional — bridge compiles but runs without it on classpath
    compileOnly(project(":arc-diagnostics"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

description = "ARCCore Bootstrap — module bootstrap pipeline, phase orchestration, and startup profiling"
