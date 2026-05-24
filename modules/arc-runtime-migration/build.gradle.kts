plugins {
    id("arc.module")
}

dependencies {
    implementation(project(":arc-api"))
    implementation(project(":arc-runtime"))
    compileOnly(project(":arc-runtime-snapshot"))
    compileOnly(project(":arc-zero-downtime"))
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

description = "ARCCore Live Runtime Migration — state-preserving distributed runtime relocation"
