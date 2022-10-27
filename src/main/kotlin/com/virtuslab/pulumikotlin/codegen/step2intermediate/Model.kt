package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.GeneratedClass.NormalClass
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.KDoc

data class UsageKind(val depth: Depth, val subject: Subject, val direction: Direction) {
    fun toNested() = copy(depth = Nested)
}

enum class Direction {
    Input, Output
}

enum class Depth {
    Root, Nested
}

enum class Subject {
    Function, Resource
}

enum class LanguageType {
    Kotlin, Java
}

enum class GeneratedClass {
    EnumClass, NormalClass
}

data class NamingFlags(
    val depth: Depth,
    val subject: Subject,
    val direction: Direction,
    val language: LanguageType,
    val generatedClass: GeneratedClass = NormalClass,
)

/**
 * Bundles variables regarding naming conventions contained in schema:
 * * [providerName] - Unqualified name of the package (e.g. “aws”, “azure”, “gcp”, “kubernetes”, “random”).
 * * [moduleFormatRegex] - ModuleFormat is a regex that is used by the importer to extract a module name
 *      from the module portion of a type token. Packages that use the module format
 *      “namespace1/namespace2/…/namespaceN” do not need to specify a format. The regex must define one capturing group
 *      that contains the module name, which must be formatted as “namespace1/namespace2/…namespaceN”.
 *
 *      ```json
 *      "meta": {
 *          "moduleFormat": "(.*)(?:/[^/]*)"
 *      }
 *      ```
 * * [basePackage] - Prefixes the generated Java package. This setting defaults to “com.pulumi”.
 * * [packageOverrides] - Overrides for module names to Java package names.
 *      ```json
 *      "language": {
 *          "java": {
 *              "packages": {
 *                  "aws-native": "awsnative"
 *              }
 *          }
 *      }
 *      ```
 */
data class PulumiNamingConfiguration private constructor(
    val providerName: String,
    val moduleFormatRegex: Regex,
    val basePackage: String,
    val packageOverrides: Map<String, String>,
) {

    val baseNamespace: List<String>
        get() = basePackage.split(".")

    companion object {
        private const val DEFAULT_BASE_PACKAGE = "com.pulumi"
        private const val DEFAULT_MODULE_FORMAT_REGEX_LITERAL = "(.*)"

        operator fun invoke(
            providerName: String,
            moduleFormat: String? = null,
            basePackage: String? = null,
            packageOverrides: Map<String, String>? = null,
        ) = PulumiNamingConfiguration(
            providerName,
            moduleFormat?.toRegex() ?: DEFAULT_MODULE_FORMAT_REGEX_LITERAL.toRegex(),
            basePackage ?: DEFAULT_BASE_PACKAGE,
            packageOverrides ?: emptyMap(),
        )
    }
}

data class ResourceType(
    val name: PulumiName,
    val argsType: ReferencedComplexType,
    val outputFields: List<Field<*>>,
    val kDoc: KDoc,
)

data class FunctionType(
    val name: PulumiName,
    val argsType: RootType,
    val outputType: ReferencedRootType,
    val kDoc: KDoc,
)

data class IntermediateRepresentation(
    val resources: List<ResourceType>,
    val functions: List<FunctionType>,
    val types: List<RootType>,
)
