package com.virtuslab.pulumikotlin.codegen

import com.squareup.kotlinpoet.*
import com.virtuslab.pulumikotlin.codegen.step1_schema_parse.Decoder
import com.virtuslab.pulumikotlin.codegen.step2_intermediate.*
import com.virtuslab.pulumikotlin.codegen.step3_codegen.Generate

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

fun moveSdk(fromPath: String, toPath: String) {
    File(fromPath).copyRecursively(target = File(toPath), overwrite = true)
}

fun packageNameForName(name: String): String {
    return "com.pulumi.kotlin." + name.split("/").first().replace(":", ".").replace("-", "")
}

fun resourcePackageNameForName(name: String): String {
    return "com.pulumi.kotlin.resources." + name.split("/").first().replace(":", ".").replace("-", "")
}

fun javaPackageNameForName(name: String): String {
    return "com.pulumi." + name.split("/").first().replace(":", ".").replace("-", "")
}

fun fileNameForName(name: String): String {
    return name.split("/").last().split(":").last().replace("-", "").capitalize()
}

fun classNameForName(name: String): ClassName {
    return ClassName(packageNameForName(name), fileNameForName(name))
}

fun classNameForNameSuffix(name: String, suffix: String): ClassName {
    return ClassName(packageNameForName(name), fileNameForName(name) + suffix)
}

fun javaClassNameForNameSuffix(name: String, suffix: String): ClassName {
    return ClassName(javaPackageNameForName(name), fileNameForName(name) + suffix)
}
