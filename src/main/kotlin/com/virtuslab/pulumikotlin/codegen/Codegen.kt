package com.virtuslab.pulumikotlin.codegen

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Decoder
import com.virtuslab.pulumikotlin.codegen.step2intermediate.getFunctionSpecs
import com.virtuslab.pulumikotlin.codegen.step2intermediate.getResourceSpecs
import com.virtuslab.pulumikotlin.codegen.step2intermediate.getTypeSpecs
import com.virtuslab.pulumikotlin.codegen.step3codegen.CodeGenerator
import com.virtuslab.pulumikotlin.codegen.step3codegen.GeneratorArguments
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
        val parsedSchemas = Decoder.decode(inputStreamWithSchema)
        val autonomousTypes = getTypeSpecs(parsedSchemas)
        val resourceTypes = getResourceSpecs(parsedSchemas)
        val functionTypes = getFunctionSpecs(parsedSchemas)
        val generatedFiles = CodeGenerator.run(
            GeneratorArguments(
                types = autonomousTypes,
                resources = resourceTypes,
                functions = functionTypes
            )
        )

        val tempDirectory = createTempDirectory()

        generatedFiles.forEach {
            it.writeTo(tempDirectory.absolutePathString())
        }

        return tempDirectory.absolute().toFile()
    }
}
