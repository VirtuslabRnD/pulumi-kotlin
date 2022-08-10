package com.virtuslab.pulumikotlin.codegen

import com.virtuslab.pulumikotlin.codegen.InputOrOutput.*
import com.virtuslab.pulumikotlin.codegen.UseCharacteristic.*

fun <T> Map<String, T>.grouping(f: (T) -> Iterable<String>): Map<String, Set<String>> {
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
        .mapValues { (_, values) -> values.toSet() }
}

data class Usage(
    val inputOrOutput: InputOrOutput,
    val useCharacteristic: UseCharacteristic
)

enum class InputOrOutput {
    Input, Output
}

enum class UseCharacteristic {
    FunctionNested, ResourceNested, ResourceRoot, FunctionRoot;

    fun toNested() = when (this) {
        FunctionNested -> FunctionNested
        ResourceNested -> ResourceNested
        ResourceRoot -> ResourceNested
        FunctionRoot -> FunctionNested
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


typealias References = Map<String, List<Usage>>

fun toTypeRoot(
    references: References,
    complexTypes: Map<String, Resources.PropertySpecification>,
    name: String,
    spec: Resources.PropertySpecification
): List<AutonomousType> {
    return when(spec) {
        is Resources.ArrayProperty -> error("unexpected")
        is Resources.BooleanProperty -> error("unexpected")
        is Resources.IntegerProperty -> error("unexpected")
        is Resources.MapProperty -> error("unexpected")
        is Resources.NumberProperty -> error("unexpected")
        is Resources.ObjectProperty -> {
            val allReferences = references[name.lowercase()] ?: emptyList()

            if(allReferences.isEmpty()) {
                println("${name} references were empty")
            }

            allReferences.map { usage ->
                ComplexType(
                    TypeMetadata(PulumiName.from(name), usage),
                    spec.properties.map { (key, value) ->
                        key.value to toType(references, usage, complexTypes, value)
                    }.toMap()
                )
            }
        }
        is Resources.OneOf -> error("unexpected")
        is Resources.ReferredProperty -> error("unexpected")
        is Resources.StringEnumProperty -> {
            val allReferences = references[name.lowercase()] ?: emptyList()

            if(allReferences.isEmpty()) {
                println("${name} references were empty enum")
            }

            allReferences.map { usage ->
                EnumType(
                    TypeMetadata(PulumiName.from(name), usage),
                    spec.enum.map { it.value }
                )
            }
        }
        is Resources.StringProperty -> error("unexpected")
    }
}

fun toType(
    references: References,
    chosenUsage: Usage,
    complexTypes: Map<String, Resources.PropertySpecification>,
    spec: Resources.PropertySpecification
): Type {
    return when(spec) {
        is Resources.ArrayProperty -> ListType(toType(references, chosenUsage, complexTypes, spec.items))
        is Resources.BooleanProperty -> PrimitiveType("Boolean")
        is Resources.IntegerProperty -> PrimitiveType("Int")
        is Resources.MapProperty -> MapType(PrimitiveType("String"), toType(references, chosenUsage, complexTypes, spec.additionalProperties))
        is Resources.NumberProperty -> PrimitiveType("Double") // TODO: Double or Long or BigDecimal?
        is Resources.ObjectProperty -> error("nested objects not supported")
        is Resources.OneOf -> EitherType(toType(references, chosenUsage, complexTypes, spec.oneOf.get(0)), toType(references, chosenUsage, complexTypes, spec.oneOf.get(1)))
        is Resources.ReferredProperty -> {
            val referencedType = spec.`$ref`.value.removePrefix("#/types/")
            if(referencedType.startsWith("pulumi")) {
                AnyType
            } else {
                val foundType = complexTypes.get(referencedType.lowercase())
                if(foundType == null) {
                    println("Not found type for ${referencedType}, defaulting to Any")
                    AnyType
                } else {
                    toTypeRoot(references, complexTypes, referencedType, foundType).find {
                        it.metadata.inputOrOutput == chosenUsage.inputOrOutput && it.metadata.useCharacteristic == chosenUsage.useCharacteristic
                    }!!
                }
            }
        }
        is Resources.StringEnumProperty -> PrimitiveType("String") // TODO: support enum
        is Resources.StringProperty -> PrimitiveType("String")
    }
}

fun getTypeSpecs(
    resourceMap: ResourcesMap,
    typesMap: TypesMap,
    functionsMap: FunctionsMap
): List<AutonomousType> {

//    resourceMap.map { it.value.properties }

    // TODO: resources can also be types

    val lowercasedTypesMap = typesMap.map { (key, value) -> key.lowercase() to value }.toMap()

    val references = computeReferences(resourceMap, lowercasedTypesMap, functionsMap)

    val lowercasedReferences = references.map { (key, value) -> key.lowercase() to value }.toMap()

    val resolvedComplexTypes = typesMap.flatMap { (name, spec) ->
        toTypeRoot(lowercasedReferences, lowercasedTypesMap, name, spec)
    }

    return resolvedComplexTypes

//    allComplexTypesFor(Map<String, >)
}

data class Referenced(
    val byName: String,
    val inputOrOutput: InputOrOutput,
    val usage: UseCharacteristic
)

fun computeReferences(
    resourceMap: ResourcesMap,
    typesMap: TypesMap,
    functionsMap: FunctionsMap
): References {
    data class Temp(
        val grouping: Map<String, Set<String>>,
        val inputOrOutput: InputOrOutput,
        val useCharacteristic: UseCharacteristic
    )

    val lists = listOf(
        Temp(resourceMap.grouping { x -> getReferencedInputTypes(typesMap, x) }, Input, ResourceNested),
        Temp(resourceMap.grouping { x -> getReferencedOutputTypes(typesMap, x) }, Output, ResourceNested),
        Temp(functionsMap.grouping { x -> getReferencedInputTypes(typesMap, x) }, Input, FunctionNested),
        Temp(functionsMap.grouping { x -> getReferencedInputTypes(typesMap, x) }, Output, FunctionNested)
    )

    val allTypeMetadata = typesMap.map { (name, spec) ->
        val referenced = lists
            .flatMap { list ->
                list.grouping[name].orEmpty()
                    .map {
                        Referenced(
                            name,
                            list.inputOrOutput,
                            list.useCharacteristic
                        )
                    }
                    .toSet()
            }

        name.lowercase() to referenced.map { Usage(it.inputOrOutput, it.usage) }
    }
        .toMap()

    return allTypeMetadata
}

private fun getReferencedOutputTypes(typeMap: TypesMap, resource: Resources.Resource): List<String> {
    return resource.properties.flatMap { (name, propertySpec) ->
        getReferencedTypes(typeMap, propertySpec)
    }
        .map { it.lowercase() }
}

private fun getReferencedOutputTypes(typeMap: TypesMap, function: Function): List<String> {
    return function.outputs.properties.flatMap { (name, propertySpec) ->
        getReferencedTypes(typeMap, propertySpec)
    }
        .map { it.lowercase() }
}

private fun getReferencedInputTypes(typeMap: TypesMap, function: Function): List<String> {
    return function.inputs?.properties.orEmpty().flatMap { (name, propertySpec) ->
        getReferencedTypes(typeMap, propertySpec)
    }
        .map { it.lowercase() }
}

private fun getReferencedInputTypes(typeMap: TypesMap, resource: Resources.Resource): List<String> {
    return resource.inputProperties.flatMap { (name, propertySpec) ->
        getReferencedTypes(typeMap, propertySpec)
    }
        .map { it.lowercase() }
}

private fun getReferencedTypes(
    typeMap: TypesMap,
    propertySpec: Resources.PropertySpecification
): List<String> {
    return when (propertySpec) {
        is Resources.ArrayProperty -> getReferencedTypes(typeMap, propertySpec.items)
        is Resources.MapProperty -> getReferencedTypes(typeMap, propertySpec.additionalProperties)
        is Resources.ObjectProperty -> propertySpec.properties.values.flatMap { getReferencedTypes(typeMap, it) }
        is Resources.OneOf -> propertySpec.oneOf.flatMap { getReferencedTypes(typeMap, it) }
        is Resources.ReferredProperty -> {
            val typeName = propertySpec.`$ref`.value.removePrefix("#/types/")
            listOf(typeName) + (typeMap[typeName.lowercase()] ?. let { getReferencedTypes(typeMap, it) } ?: run {
                println("could not for ${typeName}")
                emptyList()
            })
        }

        is Resources.StringEnumProperty -> emptyList()
        is Resources.StringProperty -> emptyList()
        is Resources.BooleanProperty -> emptyList()
        is Resources.IntegerProperty -> emptyList()
        is Resources.NumberProperty -> emptyList()
    }
}


