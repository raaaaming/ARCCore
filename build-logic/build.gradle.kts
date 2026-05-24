plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.4.1")
}

gradlePlugin {
    plugins {
        register("arc-common") {
            id = "arc.common"
            implementationClass = "cc.arccore.buildlogic.ArcCommonConvention"
        }
        register("arc-module") {
            id = "arc.module"
            implementationClass = "cc.arccore.buildlogic.ArcModuleConvention"
        }
        register("arc-core-module") {
            id = "arc.core.module"
            implementationClass = "cc.arccore.buildlogic.ArcCoreModuleConvention"
        }
        register("arc-ksp-processor") {
            id = "arc.ksp.processor"
            implementationClass = "cc.arccore.buildlogic.ArcKspProcessorConvention"
        }
    }
}
