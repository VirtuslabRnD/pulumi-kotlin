package com.virtuslab.pulumikotlin.codegen

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Decoder
import com.virtuslab.pulumikotlin.codegen.step2intermediate.IntermediateRepresentationGenerator
import com.virtuslab.pulumikotlin.codegen.step3codegen.CodeGenerator
import com.virtuslab.pulumikotlin.codegen.step3codegen.GeneratorArguments
import com.virtuslab.pulumikotlin.codegen.utils.Paths
import java.io.File

class PulumiKotlin : CliktCommand() {
    private val schemaPath: String by option().required()
    private val outputDirectoryPath: String by option().required()
    private val sdkFilesPath: String by option().default(Paths.filesToCopyToSdkPath)
    override fun run() {
        val loadedSchemaClassic = File(schemaPath).inputStream()

        val parsedSchema = Decoder.decode(loadedSchemaClassic)
        val intermediateRepresentation = IntermediateRepresentationGenerator.getIntermediateRepresentation(parsedSchema)
        val generatedFiles = CodeGenerator.run(
            GeneratorArguments(
                types = intermediateRepresentation.types,
                resources = intermediateRepresentation.resources,
                functions = intermediateRepresentation.functions,
                sdkFilesToCopyPath = sdkFilesPath,
            ),
        )

        generatedFiles.forEach {
            it.writeTo(outputDirectoryPath)
        }
    }
}

fun main(args: Array<String>) {
    PulumiKotlin().main(args)
}
