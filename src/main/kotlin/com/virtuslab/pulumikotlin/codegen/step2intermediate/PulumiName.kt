package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.squareup.kotlinpoet.ClassName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.GeneratedClass.EnumClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.GeneratedClass.NormalClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import com.virtuslab.pulumikotlin.codegen.utils.capitalize
import com.virtuslab.pulumikotlin.codegen.utils.decapitalize

data class PulumiName(
    val providerName: String,
    val namespace: List<String>,
    val name: String,
) {

    private data class Modifiers(
        val nameSuffix: String,
        val packageSuffix: List<String>,
        val shouldConstructBuilders: Boolean,
        val alternativeNameSuffix: String?,
    )

    private fun NamingFlags.matches(
        depth: Depth,
        subject: Subject,
        direction: Direction,
        generatedClass: GeneratedClass,
    ) = this.depth == depth &&
        this.subject == subject &&
        this.direction == direction &&
        this.generatedClass == generatedClass

    private fun NamingFlags.getModifier(
        defaultNameSuffix: String,
        packageSuffix: List<String>,
        shouldConstructBuilders: Boolean,
        alternativeNameSuffix: String? = null,
    ) =
        Modifiers(
            defaultNameSuffix,
            if (this.language == Kotlin) listOf("kotlin") + packageSuffix else packageSuffix,
            shouldConstructBuilders,
            alternativeNameSuffix,
        )

    private fun getModifiers(namingFlags: NamingFlags): Modifiers {
        return when {
//            NamingFlags(Nested, Resource, Input, Kotlin, EnumClass) -> Modifiers(
//                "",
//                listOf("kotlin", "enums"),
//                shouldConstructBuilders = false,
//            )
//            NamingFlags(Nested, Resource, Input, Java, EnumClass) -> Modifiers(
//                "",
//                listOf("enums"),
//                shouldConstructBuilders = false,
//            )
            namingFlags.matches(Nested, Resource, Input, EnumClass) -> namingFlags.getModifier(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
            )
//            NamingFlags(Nested, Resource, Output, Kotlin, EnumClass) -> Modifiers(
//                "",
//                listOf("kotlin", "enums"),
//                shouldConstructBuilders = false,
//            )
//            NamingFlags(Nested, Resource, Output, Java, EnumClass) -> Modifiers(
//                "",
//                listOf("enums"),
//                shouldConstructBuilders = false,
//            )
            namingFlags.matches(Nested, Resource, Output, EnumClass) -> namingFlags.getModifier(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
            )
//            NamingFlags(Nested, Function, Input, Kotlin, EnumClass) -> Modifiers(
//                "",
//                listOf("kotlin", "enums"),
//                shouldConstructBuilders = false,
//            )
//            NamingFlags(Nested, Function, Input, Java, EnumClass) -> Modifiers(
//                "",
//                listOf("enums"),
//                shouldConstructBuilders = false,
//            )
            namingFlags.matches(Nested, Function, Input, EnumClass) -> namingFlags.getModifier(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
            )
//            NamingFlags(Nested, Function, Output, Kotlin, EnumClass) -> Modifiers(
//                "",
//                listOf("kotlin", "enums"),
//                shouldConstructBuilders = false,
//            )
//            NamingFlags(Nested, Function, Output, Java, EnumClass) -> Modifiers(
//                "",
//                listOf("enums"),
//                shouldConstructBuilders = false,
//            )
            namingFlags.matches(Nested, Function, Output, EnumClass) -> namingFlags.getModifier(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
            )
//            NamingFlags(Nested, Function, Input, Kotlin) -> Modifiers(
//                "Args",
//                listOf("kotlin", "inputs"),
//                shouldConstructBuilders = true,
//            )
//            NamingFlags(Nested, Function, Input, Java) -> Modifiers(
//                "",
//                listOf("inputs"),
//                shouldConstructBuilders = true,
//            )
//          example: GetIAMPolicyAuditConfigArgs (in Java: GetIAMPolicyAuditConfig) FIXED
            namingFlags.matches(Nested, Function, Input, NormalClass) -> namingFlags.getModifier(
                "",
                listOf("inputs"),
                shouldConstructBuilders = true,
            )
//            NamingFlags(Nested, Resource, Input, Kotlin) -> Modifiers(
//                "Args",
//                listOf("kotlin", "inputs"),
//                shouldConstructBuilders = true,
//            )
//            NamingFlags(Nested, Resource, Input, Java) -> Modifiers(
//                "Args",
//                listOf("inputs"),
//                shouldConstructBuilders = true,
//            )
            namingFlags.matches(Nested, Resource, Input, NormalClass) -> namingFlags.getModifier(
                "Args",
                listOf("inputs"),
                shouldConstructBuilders = true,
            )
//            NamingFlags(Root, Resource, Input, Kotlin) -> Modifiers(
//                "Args",
//                listOf("kotlin"),
//                shouldConstructBuilders = true,
//            )
//            NamingFlags(Root, Resource, Input, Java) -> Modifiers(
//                "Args",
//                listOf(),
//                shouldConstructBuilders = true,
//            )
            namingFlags.matches(Root, Resource, Input, NormalClass) -> namingFlags.getModifier(
                "Args",
                emptyList(),
                shouldConstructBuilders = true,
            )
//            NamingFlags(Root, Function, Input, Kotlin) -> Modifiers(
//                "Args",
//                listOf("kotlin", "inputs"),
//                shouldConstructBuilders = true,
//            )
//            NamingFlags(Root, Function, Input, Java) -> Modifiers(
//                "PlainArgs",
//                listOf("inputs"),
//                shouldConstructBuilders = true,
//            )
//          example: GetIAMPolicyArgs (in Java: GetIAMPolicyPlainArgs) FIXED
            namingFlags.matches(Root, Function, Input, NormalClass) -> namingFlags.getModifier(
                "PlainArgs",
                listOf("inputs"),
                shouldConstructBuilders = true,
            )
//            NamingFlags(Nested, Function, Output, Kotlin) -> Modifiers(
//                "Result",
//                listOf("kotlin", "outputs"),
//                shouldConstructBuilders = false,
//            )
//            NamingFlags(Nested, Function, Output, Java) -> Modifiers(
//                "",
//                listOf("outputs"),
//                shouldConstructBuilders = false,
//            )
//          example: GetIAMPolicyAuditConfigAuditLogConfigResult (in Java: GetIAMPolicyAuditConfigAuditLogConfig) FIXED
            namingFlags.matches(Nested, Function, Output, NormalClass) -> namingFlags.getModifier(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false,
            )
//            NamingFlags(Nested, Resource, Output, Kotlin) -> Modifiers(
//                "",
//                listOf("kotlin", "outputs"),
//                shouldConstructBuilders = false,
//            )
//            NamingFlags(Nested, Resource, Output, Java) -> Modifiers(
//                "",
//                listOf("outputs"),
//                shouldConstructBuilders = false,
//            )
            namingFlags.matches(Nested, Resource, Output, NormalClass) -> namingFlags.getModifier(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false,
            )
//            NamingFlags(Root, Function, Output, Kotlin) -> Modifiers(
//                "Result",
//                listOf("kotlin", "outputs"),
//                shouldConstructBuilders = false,
//            )
//            NamingFlags(Root, Function, Output, Java) -> Modifiers(
//                "Result",
//                listOf("outputs"),
//                shouldConstructBuilders = false,
//            )
            namingFlags.matches(Root, Function, Output, NormalClass) -> namingFlags.getModifier(
                "Result",
                listOf("outputs"),
                shouldConstructBuilders = false,
                alternativeNameSuffix = "InvokeResult",
            )
//            NamingFlags(Root, Resource, Output, Java) -> Modifiers(
//                "",
//                listOf("outputs"),
//                shouldConstructBuilders = false,
//            )
//            NamingFlags(Root, Resource, Output, Kotlin) -> Modifiers(
//                "",
//                listOf("kotlin", "outputs"),
//                shouldConstructBuilders = false,
//            )
            namingFlags.matches(Root, Resource, Output, NormalClass) -> namingFlags.getModifier(
                "",
                listOf("outputs"),
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
                    providerName.capitalize() + "Functions"
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
        val suffix = getSuffix(namingFlags)
        return name.capitalize() + suffix
    }

    fun toBuilderClassName(namingFlags: NamingFlags): String {
        val suffix = getSuffix(namingFlags)
        return name.capitalize() + suffix + "Builder"
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

    private fun getSuffix(namingFlags: NamingFlags): String {
        val modifiers = getModifiers(namingFlags)

        return if (namingFlags.useAlternativeName) {
            modifiers.alternativeNameSuffix ?: error(
                "No name suffix configured to deal with naming conflict. " +
                    "Name: $name. " +
                    "Naming flags: $namingFlags",
            )
        } else {
            modifiers.nameSuffix
        }
    }

    companion object {
        private const val EXPECTED_NUMBER_OF_SEGMENTS_IN_TOKEN = 3

        fun from(token: String, namingConfiguration: PulumiNamingConfiguration): PulumiName {
            // token = pkg ":" module ":" member

            val segments = token.split(":")

            require(segments.size == EXPECTED_NUMBER_OF_SEGMENTS_IN_TOKEN) { "Malformed token $token" }

            fun substituteWithOverride(name: String) = namingConfiguration.packageOverrides[name] ?: name

            val module = when (segments[1]) {
                "providers" -> ""
                else -> {
                    val moduleMatches = namingConfiguration.moduleFormatRegex.matchEntire(segments[1])
                        ?.groupValues
                        .orEmpty()

                    if (moduleMatches.size < 2 || moduleMatches[1].startsWith("index")) {
                        ""
                    } else {
                        moduleMatches[1]
                    }
                }
            }

            val providerName = substituteWithOverride(namingConfiguration.providerName)
            val moduleName = substituteWithOverride(module)

            val namespace =
                (namingConfiguration.baseNamespace + providerName + moduleName).filter { it.isNotBlank() }

            return PulumiName(providerName, namespace, segments[2])
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
