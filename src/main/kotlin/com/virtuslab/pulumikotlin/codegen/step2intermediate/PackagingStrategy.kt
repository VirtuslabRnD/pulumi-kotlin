package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.*
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput.*
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic.*
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.*
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.FieldType
import com.virtuslab.pulumikotlin.codegen.step3codegen.OutputWrappedField


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
                        key.value to TypeAndOptionality(toType(references, usage, complexTypes, value), spec.required.contains(key))
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


private fun toTypeNestedReference(
    references: References,
    complexTypes: Map<String, Resources.PropertySpecification>,
    spec: Resources.PropertySpecification
): Type {
    return when(spec) {
        is Resources.ArrayProperty -> ListType(toTypeNestedReference(references, complexTypes, spec.items))
        is Resources.BooleanProperty -> PrimitiveType("Boolean")
        is Resources.IntegerProperty -> PrimitiveType("Int")
        is Resources.MapProperty -> MapType(PrimitiveType("String"), toTypeNestedReference(references, complexTypes, spec.additionalProperties))
        is Resources.NumberProperty -> PrimitiveType("Double") // TODO: Double or Long or BigDecimal?
        is Resources.ObjectProperty -> error("nested objects not supported")
        is Resources.OneOf -> EitherType(toTypeNestedReference(references, complexTypes, spec.oneOf.get(0)), toTypeNestedReference(references, complexTypes, spec.oneOf.get(1)))
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
                    when(foundType) {
                        is Resources.ObjectProperty -> {
                            ComplexType(
                                TypeMetadata(PulumiName.from(referencedType), Usage(Output, ResourceNested)),
                                foundType.properties.map { (name, spec) ->
                                    name.value to TypeAndOptionality(toTypeNestedReference(references, complexTypes, spec), foundType.required.contains(name))
                                }.toMap()
                            )
                        }
                        is Resources.StringEnumProperty -> {
                            println("enum not supported yet") // TODO: support enum
                            AnyType
                        }
                        else -> {
                            error("not expected")
                        }
                    }
                }
            }
        }
        is Resources.StringEnumProperty -> PrimitiveType("String") // TODO: support enum
        is Resources.StringProperty -> PrimitiveType("String")
    }
}

data class ResourceType(val name: PulumiName, val argsType: Type, val outputFields: List<Field<*>>)

fun getResourceSpecs(parsedSchema: ParsedSchema): List<ResourceType> {
    // TODO: deduplicate with getTypeSpecs
    val lowercasedTypesMap = parsedSchema.types.map { (key, value) -> key.lowercase() to value }.toMap()

    val references = computeReferences(parsedSchema.resources, lowercasedTypesMap, parsedSchema.functions)

    return parsedSchema.resources.map { (name, spec) ->
        val outputFields = spec.properties.map { (propertyName, propertySpec) ->
            val isRequired = spec.required.contains(propertyName)
            Field(propertyName.value, OutputWrappedField(toTypeNestedReference(references, lowercasedTypesMap, propertySpec)), isRequired, emptyList())
        }

        val argument = toTypeRoot(references + mapOf(name.lowercase() to listOf(Usage(Input, ResourceRoot))), lowercasedTypesMap, name, Resources.ObjectProperty(properties = spec.inputProperties)).get(0)

        ResourceType(PulumiName.from(name), argument, outputFields)
    }
}

fun getTypeSpecs(parsedSchema: ParsedSchema): List<AutonomousType> {

    // TODO: resources can also be types
    // TODO: update2 ^ probably not, it's just that some types do not exist despite being referenced
    // TODO: do something about lowercaseing

    val lowercasedTypesMap = parsedSchema.types.map { (key, value) -> key.lowercase() to value }.toMap()

    val references = computeReferences(parsedSchema.resources, lowercasedTypesMap, parsedSchema.functions)

    // TODO: improve
    val syntheticInputFunctionTypes = parsedSchema.functions
        .map { (name, spec) -> spec.inputs ?. let { name to it } }
        .filterNotNull()
        .flatMap { (name, value) -> toTypeRoot(references + mapOf(name.lowercase() to listOf(Usage(Input, FunctionRoot))), lowercasedTypesMap, name, value) }

    // TODO: improve
    val syntheticOutputFunctionTypes = parsedSchema.functions
        .map { (name, spec) -> name to spec.outputs }
        .flatMap { (name, value) -> toTypeRoot(references + mapOf(name.lowercase() to listOf(Usage(Output, FunctionRoot))), lowercasedTypesMap, name, value) }

    // TODO: improve
    val syntheticResourceTypes = parsedSchema.resources
        .flatMap { (name, spec) -> toTypeRoot(references + mapOf(name.lowercase() to listOf(Usage(Input, ResourceRoot))), lowercasedTypesMap, name, Resources.ObjectProperty(properties = spec.inputProperties)) }

    val lowercasedReferences = references.map { (key, value) -> key.lowercase() to value }.toMap()

    val resolvedComplexTypes = parsedSchema.types.flatMap { (name, spec) ->
        toTypeRoot(lowercasedReferences, lowercasedTypesMap, name, spec)
    }

    return resolvedComplexTypes + syntheticInputFunctionTypes + syntheticOutputFunctionTypes + syntheticResourceTypes
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
                Usage(Input, ResourceNested),
                it.inputProperties.values.toList()
            )
        },

        resourceMap.values.flatMap {
            getReferencedTypes1(
                typesMap,
                Usage(Output, ResourceNested),
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


