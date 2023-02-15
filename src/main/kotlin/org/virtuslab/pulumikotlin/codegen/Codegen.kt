package org.virtuslab.pulumikotlin.codegen

import org.virtuslab.pulumikotlin.codegen.step1schemaparse.Decoder
import org.virtuslab.pulumikotlin.codegen.step2intermediate.IntermediateRepresentationGenerator
import org.virtuslab.pulumikotlin.codegen.step3codegen.CodeGenerator
import org.virtuslab.pulumikotlin.codegen.step3codegen.GeneratorArguments
import java.io.File
import java.io.InputStream
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory

object Codegen {
    /**
     * Generates Pulumi SDK Kotlin for a particular schema and saves it to temporary directory (returned File)
     */
    fun codegen(inputStreamWithSchema: InputStream): File {
        val parsedSchema = Decoder.decode(inputStreamWithSchema)
        val intermediateRepresentation = IntermediateRepresentationGenerator.getIntermediateRepresentation(parsedSchema)
        val generatedFiles = CodeGenerator.run(
            GeneratorArguments(
                types = intermediateRepresentation.types,
                resources = intermediateRepresentation.resources,
                functions = intermediateRepresentation.functions,
            ),
        )

        val tempDirectory = createTempDirectory()

        generatedFiles.forEach {
            it.writeTo(tempDirectory.absolutePathString())
        }

        return tempDirectory.absolute().toFile()
    }
}
