package com.virtuslab.pulumikotlin.codegen

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Decoder
import com.virtuslab.pulumikotlin.codegen.step2intermediate.*
import com.virtuslab.pulumikotlin.codegen.step3codegen.CodeGenerator
import com.virtuslab.pulumikotlin.codegen.utils.Paths

import java.io.File

class PulumiKotlin : CliktCommand() {
    private val schemaPath: String by option().required()
    private val outputDirectoryPath: String by option().required()
    private val sdkFilesPath: String by option().default(Paths.filesToCopyToSdkPath)

    override fun run() {
        val loadedSchemaClassic = File(schemaPath).inputStream()
        val parsedSchemas = Decoder.decode(loadedSchemaClassic)

        val autonomousTypes = getTypeSpecs(parsedSchemas)
        val resourceTypes = getResourceSpecs(parsedSchemas)
        val functionTypes = getFunctionSpecs(parsedSchemas)

        val generatedFiles = CodeGenerator.run(
            CodeGenerator.Arguments(
                types = autonomousTypes,
                resources = resourceTypes,
                functions = functionTypes,
                sdkFilesToCopyPath = sdkFilesPath,
            )
        )

        generatedFiles.forEach {
            it.writeTo(outputDirectoryPath)
        }
    }
}

fun main(args: Array<String>) {
    println(File("path").absolutePath)
    PulumiKotlin().main(args)
}
