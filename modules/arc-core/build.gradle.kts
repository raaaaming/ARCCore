import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

plugins {
    id("arc.core.module")
}

description = "ARCCore Core — Paper plugin entrypoint"

dependencies {
    implementation(project(":arc-coroutine"))
    implementation(project(":arc-diagnostics"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
}

abstract class WritePluginYmlTask : DefaultTask() {
    @get:Input abstract val pluginVersion: Property<String>
    @get:InputFile abstract val templateFile: RegularFileProperty
    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun write() {
        val content = templateFile.get().asFile.readText()
            .replace("\${version}", pluginVersion.get())
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(content)
    }
}

val writePluginYml = tasks.register<WritePluginYmlTask>("writePluginYml") {
    pluginVersion.set(project.version.toString())
    templateFile.set(layout.projectDirectory.file("src/main/resources/plugin.yml"))
    outputFile.set(layout.buildDirectory.file("generated-resources/plugin.yml"))
}

tasks.processResources {
    exclude("plugin.yml")
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    relocate("com.fasterxml.jackson", "cc.arccore.libs.jackson")
    from(writePluginYml.map { it.outputFile.get().asFile.parentFile }) {
        into("")
    }
    dependsOn(writePluginYml)
}

tasks.jar {
    archiveClassifier.set("original")
    from(writePluginYml.map { it.outputFile.get().asFile.parentFile }) {
        into("")
    }
    dependsOn(writePluginYml)
}
