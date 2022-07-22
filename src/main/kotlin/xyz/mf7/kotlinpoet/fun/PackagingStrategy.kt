package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import xyz.mf7.kotlinpoet.`fun`.InputOrOutput.*
import xyz.mf7.kotlinpoet.`fun`.LanguageType.Kotlin
import xyz.mf7.kotlinpoet.`fun`.UseCharacteristic.*

@JvmInline
value class Package(val value: String)

data class PackageWithSpec<T>(val thePackage: String, val spec: T)

data class FunctionSpec(
    val packageName: String,
    val className: String,
    val functionName: String,
    val function: Function
)

data class TypeMetadata(
    val pulumiName: PulumiName,
    val inputOrOutput: InputOrOutput,
    val useCharacteristic: UseCharacteristic,
) {

    private fun namingFlags(language: LanguageType) =
        NamingFlags(inputOrOutput, useCharacteristic, language)

    fun toClassName(language: LanguageType): String {
        return pulumiName.toClassName(namingFlags(language))
    }

    fun toPackage(language: LanguageType): String {
        return pulumiName.toPackage(namingFlags(language))
    }

    fun toFunctionName(language: LanguageType): String {
        return pulumiName.toFunctionName(namingFlags(language))
    }
}

data class TypeWithMetadata(
    val metadata: TypeMetadata,
    val parent: Type,
    val type: Type
)

sealed class Type {
    abstract fun toTypeName(): TypeName
}

data class ComplexType(val metadata: TypeMetadata, val parent: Type, val fields: Map<String, Type>) : Type() {
    override fun toTypeName(): TypeName {
        return ClassName(metadata.toPackage(LanguageType.Kotlin), metadata.toClassName(LanguageType.Kotlin))
    }
}

data class ListType(val type: Type) : Type() {
    override fun toTypeName(): TypeName {
        return LIST.parameterizedBy(type.toTypeName())
    }
}

data class MapType(val type: Type) : Type() {
    override fun toTypeName(): TypeName {
        return MAP.parameterizedBy(
            listOf(STRING, type.toTypeName())
        )
    }

}

data class PrimitiveType(val name: String) : Type() {
    override fun toTypeName(): TypeName {
        return ClassName("kotlin", name)
    }
}

data class ResourceSpec(
    val packageName: String,
    val className: String,
    val resource: Resources.Resource
)

data class Specs(
    val resourceSpec: ResourceSpec,
    val typeSpec: TypeWithMetadata,
    val functionSpec: FunctionSpec
)

fun <T> Map<String, T>.grouping(f: (T) -> List<String>): Map<String, List<String>> {
    return this
        .flatMap { (name, resource) ->
            val referencedTypes = f(resource)
            referencedTypes.map {
                it to name
            }
        }
        .groupBy(
            { (typeName, resourceName) ->
                typeName
            },
            { (typeName, resourceName) ->
                resourceName
            },
        )
}

data class PackageAndClassName(
    val packageString: String,
    val className: String
)

enum class InputOrOutput {
    Input, Output
}

enum class UseCharacteristic {
    FunctionNested, ResourceNested, ResourceRoot, FunctionRoot;

    fun toNested() = when (this) {
        FunctionNested -> FunctionNested
        ResourceNested -> ResourceNested
        ResourceRoot -> ResourceRoot
        FunctionRoot -> FunctionRoot
    }
}

enum class LanguageType {
    Kotlin, Java
}

data class NamingFlags(
    val inputOrOutput: InputOrOutput,
    val usage: UseCharacteristic,
    val language: LanguageType
)

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
                listOf("input"),
                shouldConstructBuilders = true
            )
            NamingFlags(Input, ResourceNested, Kotlin) -> Modifiers(
                "Args",
                listOf("input"),
                shouldConstructBuilders = true
            )
            NamingFlags(Input, ResourceRoot, Kotlin) -> Modifiers("Args", listOf(), shouldConstructBuilders = true)
            NamingFlags(Input, FunctionRoot, Kotlin) -> Modifiers(
                "Args",
                listOf("input"),
                shouldConstructBuilders = true
            )
            NamingFlags(Output, FunctionNested, Kotlin) -> Modifiers(
                "Result",
                listOf("output"),
                shouldConstructBuilders = false
            )
            NamingFlags(Output, ResourceNested, Kotlin) -> Modifiers(
                "",
                listOf("output"),
                shouldConstructBuilders = false
            )
            NamingFlags(Output, FunctionRoot, Kotlin) -> Modifiers(
                "Result",
                listOf("output"),
                shouldConstructBuilders = false
            )
            else -> error("not possible")
        }
    }

    fun toClassName(namingFlags: NamingFlags): String {
        val modifiers = getModifiers(namingFlags)
        return name.capitalize() + modifiers.nameSuffix
    }

    fun toPackage(namingFlags: NamingFlags): String {
        val modifiers = getModifiers(namingFlags)
        return packageToString(relativeToComPulumiKotlin(namespace)) + modifiers.packageSuffix
    }

    fun toFunctionName(namingFlags: NamingFlags): String {
        val modifiers = getModifiers(namingFlags)
        return name.decapitalize()
    }

    private fun packageToString(packageList: List<String>): String {
        return packageList.joinToString(".")
    }

    private fun relativeToComPulumiKotlin(packageList: List<String>): List<String> {
        return listOf("com", "pulumi", "kotlin") + packageList
    }
}

fun getTypeSpecs(
    resourceMap: ResourcesMap,
    typesMap: TypesMap,
    functionsMap: FunctionsMap
): List<ComplexType> {
    data class Temp(
        val grouping: Map<String, List<String>>,
        val inputOrOutput: InputOrOutput,
        val useCharacteristic: UseCharacteristic
    )

    val functionRootInputTypes = functionsMap
        .filter { (_, function) -> function.inputs != null }
        .map { (name, function) ->
            TypeWithMetadata(
                TypeMetadata(
                    PulumiName.from(name),
                    PulumiName.from(name),
                    Input,
                    FunctionRoot
                ),
                function.inputs!!
            )
        }

    val functionRootOutputTypes = functionsMap
        .map { (name, function) ->
            TypeWithMetadata(
                TypeMetadata(
                    PulumiName.from(name),
                    PulumiName.from(name),
                    Output,
                    FunctionRoot
                ),
                function.outputs
            )
        }

    val resourceRootInputTypes = resourceMap
        .map { (name, resource) ->
            TypeWithMetadata(
                TypeMetadata(
                    PulumiName.from(name),
                    PulumiName.from(name),
                    Input,
                    ResourceRoot
                ),
                Resources.ObjectProperty(
                    properties = resource.inputProperties
                )
            )
        }

    val lists = listOf(
        Temp(resourceMap.grouping(::getReferencedInputTypes), Input, ResourceNested),
        Temp(resourceMap.grouping(::getReferencedOutputTypes), Output, ResourceNested),
        Temp(functionsMap.grouping(::getReferencedInputTypes), Input, FunctionNested),
        Temp(functionsMap.grouping(::getReferencedOutputTypes), Output, FunctionNested)
    )

    val allTypeMetadata = typesMap.flatMap { (name, spec) ->
        lists
            .flatMap { list ->
                list.grouping[name].orEmpty()
                    .map {
                        TypeMetadata(
                            PulumiName.from(name),
                            PulumiName.from(it),
                            list.inputOrOutput,
                            list.useCharacteristic
                        )
                    }
                    .toSet()
            }
            .map { metadata -> TypeWithMetadata(metadata, spec) }
    }
    return (allTypeMetadata + functionRootInputTypes + functionRootOutputTypes + resourceRootInputTypes)
        .associateBy { it.metadata.pulumiName }
}

private fun getReferencedOutputTypes(resource: Resources.Resource): List<String> {
    return resource.properties.flatMap { (name, propertySpec) ->
        getReferencedTypes(propertySpec)
    }
}

private fun getReferencedOutputTypes(function: Function): List<String> {
    return function.outputs.properties.flatMap { (name, propertySpec) ->
        getReferencedTypes(propertySpec)
    }
}

private fun getReferencedInputTypes(function: Function): List<String> {
    return function.inputs?.properties.orEmpty().flatMap { (name, propertySpec) ->
        getReferencedTypes(propertySpec)
    }
}

private fun getReferencedInputTypes(resource: Resources.Resource): List<String> {
    return resource.inputProperties.flatMap { (name, propertySpec) ->
        getReferencedTypes(propertySpec)
    }
}

private fun getReferencedTypes(
    parent: Type,
    inputOrOutput: InputOrOutput,
    useChar: UseCharacteristic,
    propertySpec: Resources.PropertySpecification
): Type {
    return when (propertySpec) {
        is Resources.ArrayProperty -> getReferencedTypes(parent, inputOrOutput, useChar.toNested(), propertySpec.items)
        is Resources.MapProperty -> getReferencedTypes(parent, inputOrOutput, useChar.toNested(), propertySpec.additionalProperties)
        is Resources.ObjectProperty -> propertySpec.properties.values.flatMap { getReferencedTypes(it) }
        is Resources.OneOf -> propertySpec.oneOf.flatMap { getReferencedTypes(it) }
        is Resources.ReferredProperty -> listOf(propertySpec.`$ref`.value.removePrefix("#/types/"))

        is Resources.StringEnumProperty -> PrimitiveType()
        is Resources.StringProperty -> emptyList()
        is Resources.BooleanProperty -> emptyList()
        is Resources.IntegerProperty -> emptyList()
        is Resources.NumberProperty -> emptyList()
    }
}


