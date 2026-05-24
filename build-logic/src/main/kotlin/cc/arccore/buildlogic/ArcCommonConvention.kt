package cc.arccore.buildlogic

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class ArcCommonConvention : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("java-library")

        project.group = project.rootProject.group ?: "kr.raaaaming"
        project.version = project.rootProject.version ?: "1.0.0-Beta"

        project.extensions.getByType(KotlinJvmProjectExtension::class.java).jvmToolchain(21)
        project.extensions.getByType(JavaPluginExtension::class.java).withSourcesJar()

        project.tasks.withType(
            JavaCompile::class.java,
            Action<JavaCompile> {
                options.encoding = "UTF-8"
            }
        )

        project.tasks.withType(
            KotlinJvmCompile::class.java,
            Action<KotlinJvmCompile> {
                compilerOptions.freeCompilerArgs.add("-Xjsr305=strict")
                compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
            }
        )

        project.repositories.mavenCentral()
        project.repositories.maven(
            Action {
                setUrl("https://repo.papermc.io/repository/maven-public/")
            }
        )
    }
}
