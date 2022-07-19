package xyz.mf7.kotlinpoet.`fun`

import xyz.mf7.kotlinpoet.`fun`.InputOrOutput.*
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
    val packageName: String,
    val typeClassName: String,
    val typeBuilderClassName: String,
    val functionBuilderClassName: String,
    val inputOrOutput: InputOrOutput,
    val useCharacteristic: UseCharacteristic,
    val shouldConstructBuilders: Boolean
)

data class TypeWithMetadata(
    val names: TypeMetadata,
    val type: Resources.PropertySpecification
)

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
}

enum class InputOrOutput {
    Input, Output
}

enum class UseCharacteristic {
    FunctionNested, ResourceNested, ResourceRoot, FunctionRoot
}

fun packageToString(packageList: List<String>): String {
    return packageList.joinToString(".")
}

fun relativeToComPulumiKotlin(packageList: List<String>): List<String> {
    return listOf("com", "pulumi", "kotlin") + packageList
}

fun computeTypeNames(
    pulumiNameString: String,
    inputOrOutput: InputOrOutput,
    useCharacteristic: UseCharacteristic
): TypeMetadata {
    val pulumiName = PulumiName.from(pulumiNameString)

    data class Modifiers(val nameSuffix: String, val packageSuffix: List<String>, val shouldConstructBuilders: Boolean)

    val (nameSuffix, packageSuffix, shouldConstructBuilders) = when (inputOrOutput to useCharacteristic) {
        Pair(Input, FunctionNested) -> Modifiers("Args", listOf("input"), shouldConstructBuilders = true)
        Pair(Input, ResourceNested) -> Modifiers("Args", listOf("input"), shouldConstructBuilders = true)
        Pair(Input, ResourceRoot) -> Modifiers("Args", listOf(), shouldConstructBuilders = true)
        Pair(Input, FunctionRoot) -> Modifiers("Args", listOf("input"), shouldConstructBuilders = true)

        Pair(Output, FunctionNested) -> Modifiers("Result", listOf("output"), shouldConstructBuilders = false)
        Pair(Output, ResourceNested) -> Modifiers("", listOf("output"), shouldConstructBuilders = false)
        Pair(Output, FunctionRoot) -> Modifiers("Result", listOf("output"), shouldConstructBuilders = false)
        else -> error("not possible")
    }

    val fullPackage = relativeToComPulumiKotlin(pulumiName.namespace) + packageSuffix

    return TypeMetadata(
        packageToString(fullPackage),
        pulumiName.name.capitalize() + nameSuffix,
        pulumiName.name.capitalize() + "Builder" + nameSuffix,
        pulumiName.name.decapitalize() + nameSuffix,
        inputOrOutput,
        useCharacteristic,
        shouldConstructBuilders
    )
}

fun getTypeSpecs(
    resourceMap: ResourcesMap,
    typesMap: TypesMap,
    functionsMap: FunctionsMap
): List<TypeWithMetadata> {
    data class Temp(
        val grouping: Map<String, List<String>>,
        val inputOrOutput: InputOrOutput,
        val useCharacteristic: UseCharacteristic
    )

    val functionRootInputTypes = functionsMap
        .filter { (name, function) -> function.inputs != null }
        .map { (name, function) ->
            TypeWithMetadata(
                computeTypeNames(
                    name,
                    Input,
                    FunctionRoot
                ),
                function.inputs!!
            )
        }

    val functionRootOutputTypes = functionsMap
        .map { (name, function) ->
            TypeWithMetadata(
                computeTypeNames(
                    name,
                    Output,
                    FunctionRoot
                ),
                function.outputs
            )
        }

    val resourceRootInputTypes = resourceMap
        .map { (name, resource) ->
            TypeWithMetadata(
                computeTypeNames(
                    name,
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
                        computeTypeNames(name, list.inputOrOutput, list.useCharacteristic)
                    }
                    .toSet()
            }
            .map { metadata -> TypeWithMetadata(metadata, spec) }
    }

    return allTypeMetadata + functionRootInputTypes + functionRootOutputTypes + resourceRootInputTypes
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

private fun getReferencedTypes(propertySpec: Resources.PropertySpecification): List<String> {
    return when (propertySpec) {
        is Resources.ArrayProperty -> getReferencedTypes(propertySpec.items)
        is Resources.MapProperty -> getReferencedTypes(propertySpec.additionalProperties)
        is Resources.ObjectProperty -> propertySpec.properties.values.flatMap { getReferencedTypes(it) }
        is Resources.OneOf -> propertySpec.oneOf.flatMap { getReferencedTypes(it) }
        is Resources.ReferredProperty -> listOf(propertySpec.`$ref`.value.removePrefix("#/types/"))

        is Resources.StringEnumProperty -> emptyList()
        is Resources.StringProperty -> emptyList()
        is Resources.BooleanProperty -> emptyList()
        is Resources.IntegerProperty -> emptyList()
        is Resources.NumberProperty -> emptyList()
    }
}


