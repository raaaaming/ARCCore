plugins {
    id("arc.module")
}

dependencies {
    implementation(project(":arc-loader"))
    implementation(project(":arc-event"))
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

description = "ARCCore Runtime — lifecycle management for modules"
