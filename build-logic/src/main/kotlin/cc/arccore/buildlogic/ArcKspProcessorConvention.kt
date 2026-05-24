package cc.arccore.buildlogic

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project

class ArcKspProcessorConvention : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("arc.common")
        project.pluginManager.apply("com.gradleup.shadow")

        project.dependencies.add("compileOnly", "com.google.devtools.ksp:symbol-processing-api:2.1.21-2.0.1")
        project.dependencies.add("implementation", "com.squareup:kotlinpoet:2.1.0")
        project.dependencies.add("implementation", "com.squareup:kotlinpoet-ksp:2.1.0")

        project.tasks.named("shadowJar", ShadowJar::class.java) {
            archiveClassifier.set("")
            mergeServiceFiles()
        }

        project.tasks.named("jar") {
            enabled = false
        }

        project.artifacts.add("archives", project.tasks.named("shadowJar"))
    }
}
