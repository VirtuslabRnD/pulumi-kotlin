package com.virtuslab.pulumikotlin.codegen

import com.squareup.kotlinpoet.*
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Decoder
import com.virtuslab.pulumikotlin.codegen.step2intermediate.*
import com.virtuslab.pulumikotlin.codegen.step3codegen.Generate

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString


fun main(args: Array<String>) {
    val loadedSchemaClassic = { }::class.java.getResourceAsStream("/schema-aws-classic.json")!!

    val parsedSchemas = Decoder.decode(loadedSchemaClassic)
    val autonomousTypes = getTypeSpecs(parsedSchemas)
    val generatedFiles = Generate.generate(autonomousTypes)

    generatedFiles.forEach {
        it.writeTo("/Users/mfudala/workspace/pulumi-fun/calendar-ninja/infra-pulumi/app/src/main/java/")
    }


}

fun generateAndSaveVersionAndPluginFile(baseResourcesPath: String, packageName: String) {
    val path = Path(baseResourcesPath, packageName.replace(".", "/")).absolutePathString()
    File(path).mkdirs()
    File(path, "plugin.json").writeText(
        """
        {
            "resource": true,
            "name": "aws",
            "version": "5.4.0"
        }
    """.trimIndent()
    )

    File(path, "version.txt").writeText(
        "5.4.0"
    )
}

