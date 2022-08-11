package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.*
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput.*
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic.*
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.*


data class Usage(
    val inputOrOutput: InputOrOutput,
    val useCharacteristic: UseCharacteristic
) {
    fun toNested() = copy(useCharacteristic = useCharacteristic.toNested())
}

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

private fun toTypeRoot(
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

private fun toType(
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
                        it.metadata.inputOrOutput == chosenUsage.inputOrOutput
                    }!!
                }
            }
        }
        is Resources.StringEnumProperty -> PrimitiveType("String") // TODO: support enum
        is Resources.StringProperty -> PrimitiveType("String")
    }
}

fun getTypeSpecs(parsedSchema: ParsedSchema): List<AutonomousType> {

    // TODO: resources can also be types
    // TODO: update2 ^ probably not, it's just that some types do not exist despite being referenced
    // TODO: do something about lowercaseing

    val lowercasedTypesMap = parsedSchema.types.map { (key, value) -> key.lowercase() to value }.toMap()

    val references = computeReferences(parsedSchema.resources, lowercasedTypesMap, parsedSchema.functions)

    val syntheticInputFunctionTypes = parsedSchema.functions
        .map { (name, spec) -> spec.inputs ?. let { name to it } }
        .filterNotNull()
        .flatMap { (name, value) -> toTypeRoot(references + mapOf(name.lowercase() to listOf(Usage(Input, FunctionRoot))), lowercasedTypesMap, name, value) }

    val syntheticOutputFunctionTypes = parsedSchema.functions
        .map { (name, spec) -> name to spec.outputs }
        .flatMap { (name, value) -> toTypeRoot(references + mapOf(name.lowercase() to listOf(Usage(Output, FunctionRoot))), lowercasedTypesMap, name, value) }

    val lowercasedReferences = references.map { (key, value) -> key.lowercase() to value }.toMap()

    val resolvedComplexTypes = parsedSchema.types.flatMap { (name, spec) ->
        toTypeRoot(lowercasedReferences, lowercasedTypesMap, name, spec)
    }

    return resolvedComplexTypes + syntheticInputFunctionTypes + syntheticOutputFunctionTypes
}

private fun computeReferences(
    resourceMap: ResourcesMap,
    typesMap: TypesMap,
    functionsMap: FunctionsMap
): References {

    val lists = listOf(
        resourceMap.values.flatMap {
            getReferencedTypes1(
                typesMap,
                Usage(Input, ResourceRoot),
                it.inputProperties.values.toList()
            )
        },

        resourceMap.values.flatMap {
            getReferencedTypes1(
                typesMap,
                Usage(Output, ResourceRoot),
                it.properties.values.toList()
            )
        },

        functionsMap.values.flatMap {
            getReferencedTypes1(
                typesMap,
                Usage(Output, FunctionNested),
                it.outputs.properties.values.toList()
            )
        },

        functionsMap.values.flatMap {
            getReferencedTypes1(
                typesMap,
                Usage(Input, FunctionNested),
                it.inputs?.properties?.values.orEmpty().toList()
            )
        }
    )

    return lists.flatten().groupBy({ it.content.lowercase() }, { it.usage })
}

private fun getReferencedTypes1(
    typeMap: TypesMap,
    usage: Usage,
    specs: List<Resources.PropertySpecification>
): List<UsageWithName> {
    return specs
        .flatMap { propertySpec ->
            getReferencedTypes(typeMap, propertySpec, usage)
        }
        .map { it.copy(it.content.lowercase()) }
}

private data class UsageWith<T>(val content: T, val usage: Usage)
private typealias UsageWithName = UsageWith<String>

private fun getReferencedTypes(
    typeMap: TypesMap,
    propertySpec: Resources.PropertySpecification,
    usage: Usage
): List<UsageWithName> {
    return when (propertySpec) {
        is Resources.ArrayProperty -> getReferencedTypes(typeMap, propertySpec.items, usage)
        is Resources.MapProperty -> getReferencedTypes(typeMap, propertySpec.additionalProperties, usage)
        is Resources.ObjectProperty -> propertySpec.properties.values.flatMap { getReferencedTypes(typeMap, it, usage) }
        is Resources.OneOf -> propertySpec.oneOf.flatMap { getReferencedTypes(typeMap, it, usage) }
        is Resources.ReferredProperty -> {
            val typeName = propertySpec.`$ref`.value.removePrefix("#/types/")
            listOf(UsageWith(typeName, usage)) + (typeMap[typeName.lowercase()] ?. let { getReferencedTypes(typeMap, it, usage.toNested()) } ?: run {
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


