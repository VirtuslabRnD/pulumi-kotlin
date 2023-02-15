package org.virtuslab.pulumikotlin.codegen.step2intermediate

import com.squareup.kotlinpoet.ClassName
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import org.virtuslab.pulumikotlin.codegen.step2intermediate.GeneratedClass.EnumClass
import org.virtuslab.pulumikotlin.codegen.step2intermediate.GeneratedClass.NormalClass
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import org.virtuslab.pulumikotlin.codegen.utils.capitalize
import org.virtuslab.pulumikotlin.codegen.utils.decapitalize

data class PulumiName(
    val providerName: String,
    val namespace: List<String>,
    val name: String,
) {

    private data class Modifiers(
        val nameSuffix: String,
        val packageSuffix: List<String>,
        val shouldConstructBuilders: Boolean,
        val shouldImplementToJava: Boolean,
        val shouldImplementToKotlin: Boolean,
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
        shouldImplementToJava: Boolean,
        shouldImplementToKotlin: Boolean,
        alternativeNameSuffix: String? = null,
    ) =
        Modifiers(
            defaultNameSuffix,
            if (this.language == Kotlin) listOf("kotlin") + packageSuffix else packageSuffix,
            shouldConstructBuilders,
            shouldImplementToJava,
            shouldImplementToKotlin,
            alternativeNameSuffix,
        )

    private fun getModifiers(namingFlags: NamingFlags): Modifiers {
        return when {
            namingFlags.matches(Nested, Resource, Input, EnumClass) -> namingFlags.getModifier(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
                shouldImplementToJava = true,
                shouldImplementToKotlin = false,
            )

            namingFlags.matches(Nested, Resource, Output, EnumClass) -> namingFlags.getModifier(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
                shouldImplementToJava = false,
                shouldImplementToKotlin = true,
            )

            namingFlags.matches(Nested, Function, Input, EnumClass) -> namingFlags.getModifier(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
                shouldImplementToJava = true,
                shouldImplementToKotlin = false,
            )

            namingFlags.matches(Nested, Function, Output, EnumClass) -> namingFlags.getModifier(
                "",
                listOf("enums"),
                shouldConstructBuilders = false,
                shouldImplementToJava = false,
                shouldImplementToKotlin = true,
            )

            namingFlags.matches(Nested, Resource, Input, NormalClass) -> namingFlags.getModifier(
                "Args",
                listOf("inputs"),
                shouldConstructBuilders = true,
                shouldImplementToJava = true,
                shouldImplementToKotlin = false,
            )

            namingFlags.matches(Nested, Resource, Output, NormalClass) -> namingFlags.getModifier(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false,
                shouldImplementToJava = false,
                shouldImplementToKotlin = true,
            )

            namingFlags.matches(Nested, Function, Input, NormalClass) -> namingFlags.getModifier(
                "",
                listOf("inputs"),
                shouldConstructBuilders = true,
                shouldImplementToJava = true,
                shouldImplementToKotlin = false,
            )

            namingFlags.matches(Nested, Function, Output, NormalClass) -> namingFlags.getModifier(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false,
                shouldImplementToJava = false,
                shouldImplementToKotlin = true,
            )

            namingFlags.matches(Root, Resource, Input, NormalClass) -> namingFlags.getModifier(
                "Args",
                emptyList(),
                shouldConstructBuilders = true,
                shouldImplementToJava = true,
                shouldImplementToKotlin = false,
            )

            namingFlags.matches(Root, Resource, Output, NormalClass) -> namingFlags.getModifier(
                "",
                listOf("outputs"),
                shouldConstructBuilders = false,
                shouldImplementToJava = false,
                shouldImplementToKotlin = true,
            )

            namingFlags.matches(Root, Function, Input, NormalClass) -> namingFlags.getModifier(
                "PlainArgs",
                listOf("inputs"),
                shouldConstructBuilders = true,
                shouldImplementToJava = true,
                shouldImplementToKotlin = false,
            )

            namingFlags.matches(Root, Function, Output, NormalClass) -> namingFlags.getModifier(
                "Result",
                listOf("outputs"),
                shouldConstructBuilders = false,
                shouldImplementToJava = false,
                shouldImplementToKotlin = true,
                alternativeNameSuffix = "InvokeResult",
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
                    namespace.last().replace(".", "_").capitalize() + "Functions"
                }
            }
        }
    }

    fun toResourceName(namingFlags: NamingFlags): String {
        return name.capitalize()
    }

    fun toClassName(namingFlags: NamingFlags): String {
        val suffix = getNameSuffix(namingFlags)
        return name.capitalize() + suffix
    }

    fun toBuilderClassName(namingFlags: NamingFlags): String {
        val suffix = getNameSuffix(namingFlags)
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

    private fun getNameSuffix(namingFlags: NamingFlags): String {
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

    fun shouldConstructBuilder(namingFlags: NamingFlags): Boolean {
        val modifiers = getModifiers(namingFlags)
        return modifiers.shouldConstructBuilders
    }

    fun shouldImplementToJava(namingFlags: NamingFlags): Boolean {
        val modifiers = getModifiers(namingFlags)
        return modifiers.shouldImplementToJava
    }

    fun shouldImplementToKotlin(namingFlags: NamingFlags): Boolean {
        val modifiers = getModifiers(namingFlags)
        return modifiers.shouldImplementToKotlin
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

            val namespace = (namingConfiguration.baseNamespace + providerName + moduleName)
                .filter { it.isNotBlank() }
                .map { it.replace("-", "") }

            val name = segments[2]

            if (name.contains("/")) {
                throw InvalidPulumiName(name, namespace)
            }

            return PulumiName(providerName, namespace, name)
        }
    }
}

class InvalidPulumiName(name: String, namespace: List<String>) : RuntimeException(
    "Skipping generation of $name from namespace $namespace",
)

data class NameGeneration(private val pulumiName: PulumiName, private val namingFlags: NamingFlags) {

    val kotlinPoetClassName get() = ClassName(packageName, className)

    val className get() = pulumiName.toClassName(namingFlags)

    val builderClassName get() = pulumiName.toBuilderClassName(namingFlags)

    val packageName get() = pulumiName.toPackage(namingFlags)

    val functionName get() = pulumiName.toFunctionName(namingFlags)

    val shouldConstructBuilders get() = pulumiName.shouldConstructBuilder(namingFlags)

    val shouldImplementToJava get() = pulumiName.shouldImplementToJava(namingFlags)

    val shouldImplementToKotlin get() = pulumiName.shouldImplementToKotlin(namingFlags)
}
