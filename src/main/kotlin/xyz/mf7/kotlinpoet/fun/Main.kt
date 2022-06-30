package xyz.mf7.kotlinpoet.`fun`

import com.pulumi.aws.emr.inputs.ClusterBootstrapActionArgs
import com.pulumi.kotlin.aws.emr.*
import com.squareup.kotlinpoet.*
import kotlinx.serialization.json.*
import xyz.mf7.kotlinpoet.sdk.pulumiArgsFromKotlinToJava
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString


fun main() {
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
//    val typesForAwsNative = Json.decodeFromJsonElement<TypeMap>(schemaFromJson.jsonObject["types"]!!)
//
//    val typesForAwsClassic = Json.decodeFromJsonElement<TypeMap>(schemaFromJsonClassic.jsonObject["types"]!!)
//
//    val functionsForAwsClassic =
//        Json.decodeFromJsonElement<FunctionsMap>(schemaFromJsonClassic.jsonObject["functions"]!!)
//
//    val destination = "/Users/mfudala/workspace/pulumi-fun/calendar-ninja/infra-pulumi/app/src/main/java"
//
//    generateTypes(typesForAwsClassic).forEach {
//        it.writeTo(File(destination))
//    }
//
////    generateTypes(typesForAwsNative).forEach {x
////        it.writeTo(File("/Users/mfudala/workspace/kotlin-poet-fun/generated_functions/src/main/kotlin"))
////    }
//
//
//    generateFunctions(functionsForAwsClassic).generatedFiles.forEach {
//        it.writeTo(File(destination))
//    }
//
//    generateAndSaveCommon(destination, "com.pulumi.kotlin.aws")
//
//    generateAndSaveVersionAndPluginFile(File(File(destination).parent, "resources").absolutePath, "com.pulumi.kotlin.aws")

    data class A(val string: String, val znt: Int)

    val omg = A("asd", 1)

    val kotlin = ClusterBootstrapAction(
        listOf("1", "2"),
        "omg",
        "a123123asdasd"
    )

    val omg2 = GetReleaseLabelsArgs(
        GetReleaseLabelsFilters(
            application = "Asd",
            prefix = "omgomg"
        )
    )

    val converted = pulumiArgsFromKotlinToJava(omg2, com.pulumi.aws.emr.inputs.GetReleaseLabelsArgs::class.java)

//    val converted = justTesting(omg)

    println(converted.filters().flatMap { it.application() })
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

fun fileNameForName(name: String): String {
    return name.split("/").last().split(":").last().replace("-", "").capitalize()
}

fun classNameForName(name: String): ClassName {
    return ClassName(packageNameForName(name), fileNameForName(name))
}

data class GeneratedResource(
    val generatedFiles: List<FileSpec>,
    val identifiedOutputReferences: Set<Resources.PropertyName>,
    val identifiedInputReferences: Set<Resources.PropertyName>
)

//fun generateResources(resourceMap: ResourcesMap): GeneratedResource {
//
//}

