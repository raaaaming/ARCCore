package cc.arccore.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class ArcModuleConvention : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("arc.common")
        project.dependencies.add("api", project.project(":arc-api"))
    }
}
