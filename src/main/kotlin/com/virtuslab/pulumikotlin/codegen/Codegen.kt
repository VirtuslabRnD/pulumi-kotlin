package com.virtuslab.pulumikotlin.codegen

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Decoder
import com.virtuslab.pulumikotlin.codegen.step2intermediate.getResourceSpecs
import com.virtuslab.pulumikotlin.codegen.step2intermediate.getTypeSpecs
import com.virtuslab.pulumikotlin.codegen.step3codegen.Generate
import java.io.File
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory

object Codegen {

    // output - directory path
    fun codegen(schema: File): File {
        if (!schema.exists()) {
            error("does not exist")
        }
        val parsedSchemas = Decoder.decode(schema.inputStream())
        val autonomousTypes = getTypeSpecs(parsedSchemas)
        val resourceTypes = getResourceSpecs(parsedSchemas)
        val generatedFiles = Generate.generate(autonomousTypes, resourceTypes)

        val tempDirectory = createTempDirectory()

        generatedFiles.forEach {
            it.writeTo(tempDirectory.absolutePathString())
        }

        return tempDirectory.absolute().toFile()
    }
}