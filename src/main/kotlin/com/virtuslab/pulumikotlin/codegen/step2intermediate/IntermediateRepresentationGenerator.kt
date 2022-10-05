package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.FunctionsMap
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.ParsedSchema
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Resources
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.ResourcesMap
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.TypesMap
import com.virtuslab.pulumikotlin.codegen.step2intermediate.GeneratedClass.EnumClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.IntermediateRepresentationGenerator.UsageWith
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic.FunctionNested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic.FunctionRoot
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic.ResourceNested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic.ResourceRoot
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.OutputWrappedField

data class Usage(
    val inputOrOutput: InputOrOutput,
    val useCharacteristic: UseCharacteristic,
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

enum class GeneratedClass {
    EnumClass, NormalClass
}

data class NamingFlags(
    val inputOrOutput: InputOrOutput,
    val usage: UseCharacteristic,
    val language: LanguageType,
    val generatedClass: GeneratedClass = GeneratedClass.NormalClass,
)

private typealias UsageWithName = UsageWith<String>

typealias References = Map<String, List<Usage>>

data class ResourceType(val name: PulumiName, val argsType: Type, val outputFields: List<Field<*>>)
data class FunctionType(val name: PulumiName, val argsType: AutonomousType, val outputType: AutonomousType)

/**
 * Takes parsed schema as an input and produce types that are prepared for code generation. More specifically:
 * - it finds out which functions and resources reference particular types (recursively)
 * - it finds out which types will be used as inputs / outputs
 * - it generates synthetic types from resources (input properties) and functions (inputs and outputs)
 */
object IntermediateRepresentationGenerator {

    data class IntermediateRepresentation(
        val resources: List<ResourceType>,
        val functions: List<FunctionType>,
        val types: List<AutonomousType>,
    )

    fun getIntermediateRepresentation(parsedSchema: ParsedSchema): IntermediateRepresentation {
        return IntermediateRepresentation(
            resources = getResourceSpecs(parsedSchema),
            functions = getFunctionSpecs(parsedSchema),
            types = getTypeSpecs(parsedSchema),
        )
    }

    private fun toTypeRoot(
        references: References,
        complexTypes: Map<String, Resources.PropertySpecification>,
        name: String,
        spec: Resources.PropertySpecification,
    ): List<AutonomousType> {
        return when (spec) {
            is Resources.ArrayProperty -> error("unexpected")
            is Resources.BooleanProperty -> error("unexpected")
            is Resources.IntegerProperty -> error("unexpected")
            is Resources.MapProperty -> error("unexpected")
            is Resources.NumberProperty -> error("unexpected")
            is Resources.ObjectProperty -> {
                val allReferences = references[name.lowercase()] ?: emptyList()

                if (allReferences.isEmpty()) {
                    println("$name references were empty")
                }

                allReferences.map { usage ->
                    ComplexType(
                        TypeMetadata(PulumiName.from(name), usage),
                        spec.properties.map { (key, value) ->
                            key.value to TypeAndOptionality(
                                toType(references, usage, complexTypes, value),
                                spec.required.contains(key),
                            )
                        }.toMap(),
                    )
                }
            }

            is Resources.OneOf -> error("unexpected")
            is Resources.ReferredProperty -> error("unexpected")
            is Resources.StringEnumProperty -> {
                val allReferences = references[name.lowercase()] ?: emptyList()

                if (allReferences.isEmpty()) {
                    println("$name references were empty enum")
                }

                allReferences.map { usage ->
                    EnumType(
                        TypeMetadata(PulumiName.from(name), usage, EnumClass),
                        spec.enum.map { it.name ?: it.value },
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
        spec: Resources.PropertySpecification,
    ): Type {
        return when (spec) {
            is Resources.ArrayProperty -> ListType(toType(references, chosenUsage, complexTypes, spec.items))
            is Resources.BooleanProperty -> PrimitiveType("Boolean")
            is Resources.IntegerProperty -> PrimitiveType("Int")
            is Resources.MapProperty -> MapType(
                PrimitiveType("String"),
                toType(references, chosenUsage, complexTypes, spec.additionalProperties),
            )

            is Resources.NumberProperty -> PrimitiveType("Double") // TODO: Double or Long or BigDecimal?
            is Resources.ObjectProperty -> error("nested objects not supported")
            is Resources.OneOf -> EitherType(
                toType(references, chosenUsage, complexTypes, spec.oneOf.get(0)),
                toType(references, chosenUsage, complexTypes, spec.oneOf.get(1)),
            )

            is Resources.ReferredProperty -> {
                val referencedType = spec.`$ref`.value.removePrefix("#/types/")
                if (referencedType.startsWith("pulumi")) {
                    AnyType
                } else {
                    val foundType = complexTypes.get(referencedType.lowercase())
                    if (foundType == null) {
                        println("Not found type for $referencedType, defaulting to Any")
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
        spec: Resources.PropertySpecification,
    ): Type {
        return when (spec) {
            is Resources.ArrayProperty -> ListType(toTypeNestedReference(references, complexTypes, spec.items))
            is Resources.BooleanProperty -> PrimitiveType("Boolean")
            is Resources.IntegerProperty -> PrimitiveType("Int")
            is Resources.MapProperty -> MapType(
                PrimitiveType("String"),
                toTypeNestedReference(references, complexTypes, spec.additionalProperties),
            )

            is Resources.NumberProperty -> PrimitiveType("Double") // TODO: Double or Long or BigDecimal?
            is Resources.ObjectProperty -> error("nested objects not supported")
            is Resources.OneOf -> EitherType(
                toTypeNestedReference(references, complexTypes, spec.oneOf.get(0)),
                toTypeNestedReference(references, complexTypes, spec.oneOf.get(1)),
            )

            is Resources.ReferredProperty -> {
                val referencedType = spec.`$ref`.value.removePrefix("#/types/")
                if (referencedType.startsWith("pulumi")) {
                    AnyType
                } else {
                    val foundType = complexTypes.get(referencedType.lowercase())
                    if (foundType == null) {
                        println("Not found type for $referencedType, defaulting to Any")
                        AnyType
                    } else {
                        when (foundType) {
                            is Resources.ObjectProperty -> {
                                ComplexType(
                                    TypeMetadata(PulumiName.from(referencedType), Usage(Output, ResourceNested)),
                                    foundType.properties.map { (name, spec) ->
                                        name.value to TypeAndOptionality(
                                            toTypeNestedReference(
                                                references,
                                                complexTypes,
                                                spec,
                                            ),
                                            foundType.required.contains(name),
                                        )
                                    }.toMap(),
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

    private fun getResourceSpecs(parsedSchema: ParsedSchema): List<ResourceType> {
        // TODO: deduplicate with getTypeSpecs
        val lowercasedTypesMap = parsedSchema.types.map { (key, value) -> key.lowercase() to value }.toMap()

        val references = computeReferences(parsedSchema.resources, lowercasedTypesMap, parsedSchema.functions)

        return parsedSchema.resources.map { (name, spec) ->
            val outputFields = spec.properties.map { (propertyName, propertySpec) ->
                val isRequired = spec.required.contains(propertyName)
                Field(
                    propertyName.value,
                    OutputWrappedField(toTypeNestedReference(references, lowercasedTypesMap, propertySpec)),
                    isRequired,
                    emptyList(),
                )
            }

            val argument = toTypeRoot(
                references + mapOf(name.lowercase() to listOf(Usage(Input, ResourceRoot))),
                lowercasedTypesMap,
                name,
                Resources.ObjectProperty(properties = spec.inputProperties),
            ).get(0)

            ResourceType(PulumiName.from(name), argument, outputFields)
        }
    }

    private fun getFunctionSpecs(parsedSchema: ParsedSchema): List<FunctionType> {
        val lowercasedTypesMap = parsedSchema.types.map { (key, value) -> key.lowercase() to value }.toMap()

        val references = computeReferences(parsedSchema.resources, lowercasedTypesMap, parsedSchema.functions)

        return parsedSchema.functions.map { (name, spec) ->
            val argument = toTypeRoot(
                references + mapOf(name.lowercase() to listOf(Usage(Input, FunctionRoot))),
                lowercasedTypesMap,
                name,
                spec.inputs ?: Resources.ObjectProperty(),
            ).get(0)
            val output = toTypeRoot(
                references + mapOf(name.lowercase() to listOf(Usage(Output, FunctionRoot))),
                lowercasedTypesMap,
                name,
                spec.outputs,
            ).get(0)

            FunctionType(PulumiName.from(name), argument, output)
        }
    }

    private fun getTypeSpecs(parsedSchema: ParsedSchema): List<AutonomousType> {
        // TODO: resources can also be types
        // TODO: update2 ^ probably not, it's just that some types do not exist despite being referenced
        // TODO: do something about lowercaseing

        val lowercasedTypesMap = parsedSchema.types.map { (key, value) -> key.lowercase() to value }.toMap()

        val references = computeReferences(parsedSchema.resources, lowercasedTypesMap, parsedSchema.functions)

        // TODO: improve
        val syntheticInputFunctionTypes = parsedSchema.functions
            .map { (name, spec) -> spec.inputs?.let { name to it } }
            .filterNotNull()
            .flatMap { (name, value) ->
                toTypeRoot(
                    references + mapOf(
                        name.lowercase() to listOf(
                            Usage(
                                Input,
                                FunctionRoot,
                            ),
                        ),
                    ),
                    lowercasedTypesMap,
                    name,
                    value,
                )
            }

        // TODO: improve
        val syntheticOutputFunctionTypes = parsedSchema.functions
            .map { (name, spec) -> name to spec.outputs }
            .flatMap { (name, value) ->
                toTypeRoot(
                    references + mapOf(
                        name.lowercase() to listOf(
                            Usage(
                                Output,
                                FunctionRoot,
                            ),
                        ),
                    ),
                    lowercasedTypesMap,
                    name,
                    value,
                )
            }

        // TODO: improve
        val syntheticResourceTypes = parsedSchema.resources
            .flatMap { (name, spec) ->
                toTypeRoot(
                    references + mapOf(
                        name.lowercase() to listOf(
                            Usage(
                                Input,
                                ResourceRoot,
                            ),
                        ),
                    ),
                    lowercasedTypesMap,
                    name,
                    Resources.ObjectProperty(properties = spec.inputProperties),
                )
            }

        val lowercasedReferences = references.map { (key, value) -> key.lowercase() to value }.toMap()

        val resolvedComplexTypes = parsedSchema.types.flatMap { (name, spec) ->
            toTypeRoot(lowercasedReferences, lowercasedTypesMap, name, spec)
        }

        return resolvedComplexTypes + syntheticInputFunctionTypes + syntheticOutputFunctionTypes + syntheticResourceTypes
    }

    private fun computeReferences(
        resourceMap: ResourcesMap,
        typesMap: TypesMap,
        functionsMap: FunctionsMap,
    ): References {
        val lists = listOf(
            resourceMap.values.flatMap {
                getReferencedTypes1(
                    typesMap,
                    Usage(Input, ResourceNested),
                    it.inputProperties.values.toList(),
                )
            },

            resourceMap.values.flatMap {
                getReferencedTypes1(
                    typesMap,
                    Usage(Output, ResourceNested),
                    it.properties.values.toList(),
                )
            },

            functionsMap.values.flatMap {
                getReferencedTypes1(
                    typesMap,
                    Usage(Output, FunctionNested),
                    it.outputs.properties.values.toList(),
                )
            },

            functionsMap.values.flatMap {
                getReferencedTypes1(
                    typesMap,
                    Usage(Input, FunctionNested),
                    it.inputs?.properties?.values.orEmpty().toList(),
                )
            },
        )

        return lists.flatten().groupBy({ it.content.lowercase() }, { it.usage })
    }

    private fun getReferencedTypes1(
        typeMap: TypesMap,
        usage: Usage,
        specs: List<Resources.PropertySpecification>,
    ): List<UsageWithName> {
        return specs
            .flatMap { propertySpec ->
                getReferencedTypes(typeMap, propertySpec, usage)
            }
            .map { it.copy(it.content.lowercase()) }
    }

    data class UsageWith<T>(val content: T, val usage: Usage)

    private fun getReferencedTypes(
        typeMap: TypesMap,
        propertySpec: Resources.PropertySpecification,
        usage: Usage,
    ): List<UsageWithName> {
        return when (propertySpec) {
            is Resources.ArrayProperty -> getReferencedTypes(typeMap, propertySpec.items, usage)
            is Resources.MapProperty -> getReferencedTypes(typeMap, propertySpec.additionalProperties, usage)
            is Resources.ObjectProperty -> propertySpec.properties.values.flatMap {
                getReferencedTypes(
                    typeMap,
                    it,
                    usage,
                )
            }

            is Resources.OneOf -> propertySpec.oneOf.flatMap { getReferencedTypes(typeMap, it, usage) }
            is Resources.ReferredProperty -> {
                val typeName = propertySpec.`$ref`.value.removePrefix("#/types/")
                listOf(UsageWith(typeName, usage)) + (
                    typeMap[typeName.lowercase()]?.let { getReferencedTypes(typeMap, it, usage.toNested()) } ?: run {
                        println("could not for $typeName")
                        emptyList()
                    }
                    )
            }

            is Resources.StringEnumProperty -> emptyList()
            is Resources.StringProperty -> emptyList()
            is Resources.BooleanProperty -> emptyList()
            is Resources.IntegerProperty -> emptyList()
            is Resources.NumberProperty -> emptyList()
        }
    }
}
