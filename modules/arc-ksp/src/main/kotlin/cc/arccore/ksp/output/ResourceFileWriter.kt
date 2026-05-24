package cc.arccore.ksp.output

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

object ResourceFileWriter {

    fun write(
        codeGenerator: CodeGenerator,
        path: String,
        extensionName: String,
        content: String,
        dependencies: Dependencies
    ) {
        codeGenerator.createNewFileByPath(
            dependencies = dependencies,
            path = path,
            extensionName = extensionName
        ).bufferedWriter().use { it.write(content) }
    }
}
