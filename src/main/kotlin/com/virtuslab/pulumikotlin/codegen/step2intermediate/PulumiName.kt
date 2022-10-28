package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.squareup.kotlinpoet.ClassName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.GeneratedClass.EnumClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import com.virtuslab.pulumikotlin.codegen.utils.capitalize
import com.virtuslab.pulumikotlin.codegen.utils.decapitalize

data class PulumiName(
    val packageProviderName: String,
    val namespace: List<String>,
    val name: String,
) {

    private data class Modifiers(
        val nameSuffix: String,
        val packageSuffix: List<String>,
        val shouldConstructBuilders: Boolean,
    )

    private fun getModifiers(namingFlags: NamingFlags): Modifiers {
        return when (namingFlags) {
            NamingFlags(Nested, Resource, Input, Kotlin, EnumClass) -> Modifiers(
                "",
                listOf("kotlin", "enums"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Nested, Resource, Output, Kotlin, EnumClass) -> Modifiers(
                "",
                listOf("kotlin", "enums"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Nested, Resource, Input, Java, EnumClass) -> Modifiers(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Nested, Resource, Output, Java, EnumClass) -> Modifiers(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Nested, Function, Input, Kotlin, EnumClass) -> Modifiers(
                "",
                listOf("kotlin", "enums"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Nested, Function, Output, Kotlin, EnumClass) -> Modifiers(
                "",
                listOf("kotlin", "enums"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Nested, Function, Input, Java, EnumClass) -> Modifiers(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Nested, Function, Output, Java, EnumClass) -> Modifiers(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Nested, Function, Input, Kotlin) -> Modifiers(
                "Args",
                listOf("kotlin", "inputs"),
                shouldConstructBuilders = true,
            )

            NamingFlags(Nested, Resource, Input, Kotlin) -> Modifiers(
                "Args",
                listOf("kotlin", "inputs"),
                shouldConstructBuilders = true,
            )

            NamingFlags(Root, Resource, Input, Kotlin) -> Modifiers(
                "Args",
                listOf("kotlin"),
                shouldConstructBuilders = true,
            )

            NamingFlags(Root, Function, Input, Kotlin) -> Modifiers(
                "Args",
                listOf("kotlin", "inputs"),
                shouldConstructBuilders = true,
            )

            NamingFlags(Nested, Function, Output, Kotlin) -> Modifiers(
                "Result",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Nested, Resource, Output, Kotlin) -> Modifiers(
                "",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Root, Function, Output, Kotlin) -> Modifiers(
                "Result",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Nested, Function, Input, Java) -> Modifiers(
                "",
                listOf("inputs"),
                shouldConstructBuilders = true,
            )

            NamingFlags(Nested, Resource, Input, Java) -> Modifiers(
                "Args",
                listOf("inputs"),
                shouldConstructBuilders = true,
            )

            NamingFlags(Root, Resource, Input, Java) -> Modifiers(
                "Args",
                listOf(),
                shouldConstructBuilders = true,
            )

            NamingFlags(Root, Function, Input, Java) -> Modifiers(
                "PlainArgs",
                listOf("inputs"),
                shouldConstructBuilders = true,
            )

            NamingFlags(Nested, Function, Output, Java) -> Modifiers(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Nested, Resource, Output, Java) -> Modifiers(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Root, Function, Output, Java) -> Modifiers(
                "Result",
                listOf("outputs"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Root, Resource, Output, Java) -> Modifiers(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false,
            )

            NamingFlags(Root, Resource, Output, Kotlin) -> Modifiers(
                "",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false,
            )

            else -> error("There is no mapping for $namingFlags (happened in $this)")
        }
    }

    fun toResourcePackage(namingFlags: NamingFlags): String {
        // TODO: todo
        return when (namingFlags.language) {
            Kotlin -> packageToString(namespace + listOf("kotlin"))
            Java -> packageToString(namespace)
        }
    }

    fun toFunctionGroupObjectPackage(namingFlags: NamingFlags): String {
        return when (namingFlags.language) {
            Kotlin -> packageToString(namespace + listOf("kotlin"))
            Java -> packageToString(namespace)
        }
    }

    fun toFunctionGroupObjectName(namingFlags: NamingFlags): String {
        return when (namingFlags.language) {
            Kotlin, Java -> {
                if (namespace.isEmpty()) {
                    packageProviderName.capitalize() + "Functions"
                } else {
                    namespace.last().capitalize() + "Functions"
                }
            }
        }
    }

    fun toResourceName(namingFlags: NamingFlags): String {
        return name
    }

    fun toClassName(namingFlags: NamingFlags): String {
        val modifiers = getModifiers(namingFlags)
        return name.capitalize() + modifiers.nameSuffix
    }

    fun toBuilderClassName(namingFlags: NamingFlags): String {
        val modifiers = getModifiers(namingFlags)
        return name.capitalize() + modifiers.nameSuffix + "Builder"
    }

    fun toPackage(namingFlags: NamingFlags): String {
        val modifiers = getModifiers(namingFlags)
        return packageToString(namespace + modifiers.packageSuffix)
    }

    fun toFunctionName(namingFlags: NamingFlags): String {
        return when (namingFlags.language) {
            Kotlin -> name.decapitalize()
            Java -> name.decapitalize() + "Plain" // TODO: improve
        }
    }

    private fun packageToString(packageList: List<String>): String {
        return packageList.joinToString(".")
    }

    companion object {
        private const val EXPECTED_NUMBER_OF_SEGMENTS_IN_TOKEN = 3

        fun from(token: String, namingConfiguration: PulumiNamingConfiguration): PulumiName {
            // token = pkg ":" module ":" member

            val segments = token.split(":")

            if (segments.size != EXPECTED_NUMBER_OF_SEGMENTS_IN_TOKEN) error("Malformed token $token")

            fun substituteWithOverride(name: String): String = namingConfiguration.packageOverrides[name] ?: name

            fun extractModule(): String = when (val module = segments[1]) {
                "providers" -> ""
                else -> {
                    val moduleMatches = namingConfiguration.moduleFormatRegex.findAll(module)
                        .flatMap { it.groups }
                        .map { it?.value.orEmpty() }
                        .toList()

                    if (moduleMatches.size < 2 || moduleMatches[1].startsWith("index")) {
                        ""
                    } else {
                        moduleMatches[1]
                    }
                }
            }

            val packageProviderName = substituteWithOverride(namingConfiguration.providerName)
            val moduleName = substituteWithOverride(extractModule())

            val namespace =
                (namingConfiguration.baseNamespace + packageProviderName + moduleName).filter { it.isNotBlank() }

            return PulumiName(packageProviderName, namespace, segments[2])
        }
    }
}

data class NameGeneration(private val pulumiName: PulumiName, private val namingFlags: NamingFlags) {

    val kotlinPoetClassName get() = ClassName(pulumiName.toPackage(namingFlags), pulumiName.toClassName(namingFlags))

    val className get() = pulumiName.toClassName(namingFlags)

    val builderClassName get() = pulumiName.toBuilderClassName(namingFlags)

    val packageName get() = pulumiName.toPackage(namingFlags)

    val functionName get() = pulumiName.toFunctionName(namingFlags)
}
