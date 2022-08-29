package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.squareup.kotlinpoet.ClassName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic.*

data class PulumiName(
    val providerName: String,
    val namespace: List<String>,
    val name: String
) {
    companion object {
        fun from(string: String): PulumiName {
            val segments = string.split("/").first().split(":")
            val providerName = segments.get(0)
            val namespace = segments.drop(1)
            val name = string.split("/").last().split(":").last()

            return PulumiName(providerName, namespace, name)
        }
    }

    private data class Modifiers(
        val nameSuffix: String,
        val packageSuffix: List<String>,
        val shouldConstructBuilders: Boolean
    )

    private fun getModifiers(namingFlags: NamingFlags): Modifiers {
        return when (namingFlags) {
            NamingFlags(Input, FunctionNested, Kotlin) -> Modifiers(
                "Args",
                listOf("kotlin", "inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(Input, ResourceNested, Kotlin) -> Modifiers(
                "Args",
                listOf("kotlin", "inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(Input, ResourceRoot, Kotlin) -> Modifiers(
                "Args",
                listOf("kotlin"),
                shouldConstructBuilders = true
            )
            NamingFlags(Input, FunctionRoot, Kotlin) -> Modifiers(
                "Args",
                listOf("kotlin", "inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(Output, FunctionNested, Kotlin) -> Modifiers(
                "Result",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(Output, ResourceNested, Kotlin) -> Modifiers(
                "",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(Output, FunctionRoot, Kotlin) -> Modifiers(
                "Result",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(Input, FunctionNested, Java) -> Modifiers(
                "",
                listOf("inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(Input, ResourceNested, Java) -> Modifiers(
                "Args",
                listOf("inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(Input, ResourceRoot, Java) -> Modifiers(
                "Args",
                listOf(),
                shouldConstructBuilders = true
            )
            NamingFlags(Input, FunctionRoot, Java) -> Modifiers(
                "PlainArgs",
                listOf("inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(Output, FunctionNested, Java) -> Modifiers(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(Output, ResourceNested, Java) -> Modifiers(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(Output, FunctionRoot, Java) -> Modifiers(
                "Result",
                listOf("outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(Output,  ResourceRoot, Java) -> Modifiers(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(Output,  ResourceRoot, Kotlin) -> Modifiers(
                "",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false
            )
            else -> error("not possible")
        }
    }

    fun toResourcePackage(namingFlags: NamingFlags): String {
        // TODO: todo
        return when(namingFlags.language) {
            Kotlin -> packageToString(relativeToProviderPackage(namespace) + listOf("kotlin"))
            Java -> packageToString(relativeToProviderPackage(namespace))
        }
    }

    fun toFunctionGroupObjectPackage(namingFlags: NamingFlags): String {
        return when(namingFlags.language) {
            Kotlin -> packageToString(relativeToProviderPackage(namespace) + listOf("kotlin"))
            Java -> packageToString(relativeToProviderPackage(namespace))
        }
    }

    fun toFunctionGroupObjectName(namingFlags: NamingFlags): String {
        return when(namingFlags.language) {
            Kotlin -> namespace.last().capitalize() + "Functions"
            Java -> namespace.last().capitalize() + "Functions"
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
        return packageToString(relativeToProviderPackage(namespace) + modifiers.packageSuffix)
    }

    fun toFunctionName(namingFlags: NamingFlags): String {
        return when(namingFlags.language) {
            Kotlin -> name.decapitalize()
            Java -> name.decapitalize() + "Plain" // TODO: improve
        }
    }

    private fun packageToString(packageList: List<String>): String {
        return packageList.joinToString(".")
    }

    private fun relativeToProviderPackage(packageList: List<String>): List<String> {
        return listOf("com", "pulumi", providerName) + packageList
    }
}

data class NameGeneration(private val pulumiName: PulumiName, private val namingFlags: NamingFlags) {

    val kotlinPoetClassName get() = ClassName(pulumiName.toPackage(namingFlags), pulumiName.toClassName(namingFlags))

    val className get() = pulumiName.toClassName(namingFlags)

    val builderClassName get() = pulumiName.toBuilderClassName(namingFlags)

    val packageName get() = pulumiName.toPackage(namingFlags)

    val functionName get() = pulumiName.toFunctionName(namingFlags)
}
