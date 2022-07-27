package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*
import kotlinx.serialization.json.*

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString


fun main(args: Array<String>) {
//
//    val loadedSchema = {}::class.java.getResourceAsStream("/schema.json")!!
//
//    val schemaFromJson = Json.parseToJsonElement(
//        loadedSchema.bufferedReader().readText()
//    )
//
//    val loadedSchemaClassic = { }::class.java.getResourceAsStream("/schema-aws-classic.json")!!
//
//    val schemaFromJsonClassic = Json.parseToJsonElement(
//        loadedSchemaClassic.bufferedReader().readText()
//    )
//
//    val typesForAwsNative = Json.decodeFromJsonElement<TypesMap>(schemaFromJson.jsonObject["types"]!!)
//
//    val typesForAwsClassic = Json.decodeFromJsonElement<TypesMap>(schemaFromJsonClassic.jsonObject["types"]!!)
//
//    val functionsForAwsClassic =
//        Json.decodeFromJsonElement<FunctionsMap>(schemaFromJsonClassic.jsonObject["functions"]!!)
//
//    val resourcesForAwsClassic =
//        Json.decodeFromJsonElement<ResourcesMap>(schemaFromJsonClassic.jsonObject["resources"]!!)
//
//    val destination = "/Users/mfudala/workspace/pulumi-fun/calendar-ninja/infra-pulumi/app/src/main/java"
//
//    generateTypes(typesForAwsClassic).forEach {
//        it.writeTo(File(destination))
//    }

//    val types2 = generateTypes2(
//        getTypeSpecs(
//            resourcesForAwsClassic,
//            typesForAwsClassic,
//            functionsForAwsClassic
//        )
//    )
//
//    types2.forEach {
//        it.writeTo(File("/Users/mfudala/workspace/pulumi-fun/calendar-ninja/infra-pulumi/app/src/main/java/test_new_types"))
//    }

    println(
        PulumiName("test", listOf("a", "b", "O"), "name")
            .toClassName(
                NamingFlags(InputOrOutput.Input, UseCharacteristic.ResourceRoot, LanguageType.Kotlin)
            )
    )

    println(
        PulumiName("test", listOf("a", "b", "O"), "name")
            .toBuilderClassName(
                NamingFlags(InputOrOutput.Input, UseCharacteristic.ResourceRoot, LanguageType.Kotlin)
            )
    )

    val complexType2 = ComplexType(
        TypeMetadata(PulumiName("test2", listOf("a2", "b2", "O2"), "name2"), InputOrOutput.Input, UseCharacteristic.ResourceNested),
        mapOf(
            "whatever2" to PrimitiveType("String")
        )
    )

    val complexType = ComplexType(
        TypeMetadata(PulumiName("test", listOf("a", "b", "O"), "name"), InputOrOutput.Input, UseCharacteristic.ResourceRoot),
        mapOf(
            "whatever" to complexType2
        )
    )


    val generatedFuns = generateTypeWithNiceBuilders("whatever", "whatever", "whatever", "whatever2", listOf(
        Field("someField", OutputWrappedField(complexType), true, listOf(
            NormalField(complexType, { from, to -> CodeBlock.of("val ${to} = Output.of(${from})") }),
            NormalField(ListType(complexType), { from, to -> CodeBlock.of("val ${to} = Output.of(${from})") })
        ))
    ))


    generatedFuns.writeTo(File("/Users/mfudala/workspace/kotlin-poet-fun/generated-funs"))

//    generateFunctions(functionsForAwsClassic).generatedFiles.forEach {
//        it.writeTo(File(destination))
//    }
//
//    generateResources(resourcesForAwsClassic).generatedFiles.forEach {
//        it.writeTo(File(destination))
//    }
//
//    generateAndSaveCommon(destination, "com.pulumi.kotlin.aws")
//
//    generateAndSaveVersionAndPluginFile(File(File(destination).parent, "resources").absolutePath, "com.pulumi.kotlin.aws")

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

fun generateAndSaveCommon(baseJavaPath: String, packageName: String) {
    val preparedPackageName = packageName.replace("-", "")

    val packagePath = preparedPackageName.replace(".", "/")

    File(baseJavaPath, "$packagePath/Utilities.java").writeText(generateUtilsFile(packagePath, preparedPackageName))
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
