package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.squareup.kotlinpoet.ClassName

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
            NamingFlags(InputOrOutput.Input, UseCharacteristic.FunctionNested, LanguageType.Kotlin) -> Modifiers(
                "Args",
                listOf("kotlin", "inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(InputOrOutput.Input, UseCharacteristic.ResourceNested, LanguageType.Kotlin) -> Modifiers(
                "Args",
                listOf("kotlin", "inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(InputOrOutput.Input, UseCharacteristic.ResourceRoot, LanguageType.Kotlin) -> Modifiers("Args", listOf("kotlin"), shouldConstructBuilders = true)
            NamingFlags(InputOrOutput.Input, UseCharacteristic.FunctionRoot, LanguageType.Kotlin) -> Modifiers(
                "Args",
                listOf("kotlin", "inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(InputOrOutput.Output, UseCharacteristic.FunctionNested, LanguageType.Kotlin) -> Modifiers(
                "Result",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(InputOrOutput.Output, UseCharacteristic.ResourceNested, LanguageType.Kotlin) -> Modifiers(
                "",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(InputOrOutput.Output, UseCharacteristic.FunctionRoot, LanguageType.Kotlin) -> Modifiers(
                "Result",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(InputOrOutput.Input, UseCharacteristic.FunctionNested, LanguageType.Java) -> Modifiers(
                "",
                listOf("inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(InputOrOutput.Input, UseCharacteristic.ResourceNested, LanguageType.Java) -> Modifiers(
                "Args",
                listOf("inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(InputOrOutput.Input, UseCharacteristic.ResourceRoot, LanguageType.Java) -> Modifiers("Args", listOf("inputs"), shouldConstructBuilders = true)
            NamingFlags(InputOrOutput.Input, UseCharacteristic.FunctionRoot, LanguageType.Java) -> Modifiers(
                "",
                listOf("inputs"),
                shouldConstructBuilders = true
            )
            NamingFlags(InputOrOutput.Output, UseCharacteristic.FunctionNested, LanguageType.Java) -> Modifiers(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(InputOrOutput.Output, UseCharacteristic.ResourceNested, LanguageType.Java) -> Modifiers(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(InputOrOutput.Output, UseCharacteristic.FunctionRoot, LanguageType.Java) -> Modifiers(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(InputOrOutput.Output,  UseCharacteristic.ResourceRoot, LanguageType.Java) -> Modifiers(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false
            )
            NamingFlags(InputOrOutput.Output,  UseCharacteristic.ResourceRoot, LanguageType.Kotlin) -> Modifiers(
                "",
                listOf("kotlin", "outputs"),
                shouldConstructBuilders = false
            )
            else -> error("not possible")
        }
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
        val modifiers = getModifiers(namingFlags)
        return name.decapitalize()
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