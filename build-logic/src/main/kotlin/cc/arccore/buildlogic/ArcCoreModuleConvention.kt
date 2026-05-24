package cc.arccore.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class ArcCoreModuleConvention : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("arc.common")
        project.pluginManager.apply("com.gradleup.shadow")

        project.dependencies.add("api", project.project(":arc-api"))
        project.dependencies.add("implementation", project.project(":arc-loader"))
        project.dependencies.add("implementation", project.project(":arc-runtime"))
        project.dependencies.add("implementation", project.project(":arc-command"))
        project.dependencies.add("implementation", project.project(":arc-event"))
        project.dependencies.add("implementation", project.project(":arc-di"))
        project.dependencies.add("implementation", project.project(":arc-reflection"))
        project.dependencies.add("implementation", project.project(":arc-utils"))
        project.dependencies.add("compileOnly", "io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    }
}
