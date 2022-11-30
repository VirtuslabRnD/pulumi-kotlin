package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ArrayProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.BooleanProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.GenericTypeProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.IntegerProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.MapProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.NumberProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ObjectProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.OneOfProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PrimitiveProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.Property
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyName
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ReferenceProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.RootTypeProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.Schema
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.StringEnumProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.StringProperty
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.GeneratedClass.EnumClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.GeneratedClass.NormalClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.KDoc
import com.virtuslab.pulumikotlin.codegen.step3codegen.OutputWrappedField
import com.virtuslab.pulumikotlin.codegen.utils.DEFAULT_PROVIDER_TOKEN
import com.virtuslab.pulumikotlin.codegen.utils.filterNotNullValues
import com.virtuslab.pulumikotlin.codegen.utils.letIf
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging

/**
 * Takes parsed schema as an input and produces types that are prepared for code generation. More specifically:
 * - it finds out which functions and resources reference particular types (recursively)
 * - it finds out which types will be used as inputs / outputs
 * - it generates synthetic types from resources (input properties) and functions (inputs and outputs)
 */
object IntermediateRepresentationGenerator {

    private val logger = KotlinLogging.logger {}

    fun getIntermediateRepresentation(schema: Schema): IntermediateRepresentation {
        val referenceFinder = ReferenceFinder(schema)
        val referencedStringTypesResolver = ReferencedStringTypesResolver(schema.types)
        val context = Context(schema, referenceFinder, referencedStringTypesResolver)

        val types = createTypes(context)
        val typeMap = types
            .associateBy { TypeKey.from(it.metadata) }
            .transformKeys { it.withLowercaseName() }

        val resources = if (schema.provider != null) {
            createResources(typeMap, context) +
                createResource(DEFAULT_PROVIDER_TOKEN, schema.provider, context, typeMap, isProvider = true)
        } else {
            createResources(typeMap, context)
        }.filterNotNull()

        return IntermediateRepresentation(
            types = types,
            resources = resources,
            functions = createFunctions(typeMap, context),
        )
    }

    private fun createTypes(context: Context): List<RootType> {
        val schema = context.schema

        fun <V> createTypes(
            map: Map<String, V>,
            usageKind: UsageKind? = null,
            propertyExtractor: (V) -> RootTypeProperty?,
        ) = map
            .mapValues { propertyExtractor(it.value) }
            .filterNotNullValues()
            .flatMap { (name, value) ->
                createRootTypes(context, name, value, listOfNotNull(usageKind))
            }

        val syntheticTypes = listOf(
            createTypes(schema.functions, UsageKind(Root, Function, Input)) { function -> function.inputs },
            createTypes(schema.functions, UsageKind(Root, Function, Output)) { function -> function.outputs },
            createTypes(schema.resources, UsageKind(Root, Resource, Input)) { resource ->
                ObjectProperty(
                    properties = resource.inputProperties,
                    description = resource.description,
                    deprecationMessage = resource.deprecationMessage,
                )
            },
            schema.provider?.let {
                createTypes(
                    mapOf(DEFAULT_PROVIDER_TOKEN to schema.provider),
                    UsageKind(Root, Resource, Input),
                ) { resource ->
                    ObjectProperty(
                        properties = resource.inputProperties,
                        description = resource.description,
                        deprecationMessage = resource.deprecationMessage,
                    )
                }
            }.orEmpty(),
        )

        val regularTypes = listOf(
            createTypes(schema.types) { it },
        )

        return (syntheticTypes + regularTypes).flatten()
    }

    private fun createResources(types: Map<TypeKey, RootType>, context: Context): List<ResourceType> {
        return context.schema.resources.mapNotNull { (typeToken, resource) ->
            createResource(typeToken, resource, context, types)
        }
    }

    private fun createResource(
        typeToken: String,
        resource: SchemaModel.Resource,
        context: Context,
        types: Map<TypeKey, RootType>,
        isProvider: Boolean = false,
    ): ResourceType? {
        val resultFields = resource.properties
            .letIf(isProvider, ::filterStringProperties)
            .map { (propertyName, property) ->
                val outputFieldsUsageKind = UsageKind(Nested, Resource, Output)
                val isRequired = resource.required.contains(propertyName)
                val reference = resolveNestedTypeReference(
                    context,
                    property,
                    outputFieldsUsageKind,
                    isRequired,
                )
                Field(propertyName.value, OutputWrappedField(reference), isRequired, kDoc = getKDoc(property))
            }

        return try {
            val pulumiName = PulumiName.from(typeToken, context.namingConfiguration)
            val inputUsageKind = UsageKind(Root, Resource, Input)
            val argumentType =
                findTypeAsReference<ReferencedComplexType>(
                    types,
                    TypeKey.from(pulumiName, inputUsageKind),
                )
            ResourceType(pulumiName, argumentType, resultFields, getKDoc(resource), isProvider)
        } catch (e: InvalidPulumiName) {
            logger.warn("Invalid name", e)
            null
        }
    }

    private fun createFunctions(types: Map<TypeKey, RootType>, context: Context): List<FunctionType> {
        return context.schema.functions.mapNotNull { (typeName, function) ->
            try {
                val pulumiName = PulumiName.from(typeName, context.namingConfiguration)

                val inputUsageKind = UsageKind(Root, Function, Input)
                val argumentType = findTypeOrEmptyComplexType(
                    types,
                    TypeKey.from(pulumiName, inputUsageKind),
                    getKDoc(function),
                )

                val outputUsageKind = UsageKind(Root, Function, Output)
                val resultType =
                    findTypeAsReference<ReferencedRootType>(types, TypeKey.from(pulumiName, outputUsageKind))

                FunctionType(pulumiName, argumentType, resultType, getKDoc(function))
            } catch (e: InvalidPulumiName) {
                logger.warn("Invalid name", e)
                null
            }
        }
    }

    private fun findTypeOrEmptyComplexType(types: Map<TypeKey, RootType>, typeKey: TypeKey, kDoc: KDoc) =
        findType(types, typeKey) ?: ComplexType(TypeMetadata(typeKey.name, typeKey.usageKind, kDoc), emptyMap())

    private fun findType(types: Map<TypeKey, RootType>, typeKey: TypeKey) =
        types[typeKey]

    private inline fun <reified T : ReferencedRootType> findTypeAsReference(
        types: Map<TypeKey, RootType>,
        typeKey: TypeKey,
    ) =
        findType(types, typeKey)
            ?.toReference()
            as? T
            ?: error("Unable to find $typeKey – reference cannot be cast to ${T::class}")

    private fun createRootTypes(
        context: Context,
        typeName: String,
        rootType: RootTypeProperty,
        forcedUsageKinds: List<UsageKind> = emptyList(),
    ): List<RootType> {
        val usages = forcedUsageKinds.ifEmpty {
            val allUsagesForTypeName = context.referenceFinder.getUsages(typeName)
            allUsagesForTypeName
        }

        if (usages.isEmpty()) {
            logger.info(
                "$typeName is not used anywhere, no RootTypes will be created for it (${rootType.javaClass})",
            )
        }

        try {
            val pulumiName = PulumiName.from(typeName, context.namingConfiguration)
            return usages.map { usage ->
                when (rootType) {
                    is ObjectProperty -> ComplexType(
                        TypeMetadata(pulumiName, usage, getKDoc(rootType), NormalClass),
                        createComplexTypeFields(rootType, context, usage),
                    )

                    is StringEnumProperty -> EnumType(
                        TypeMetadata(pulumiName, usage, getKDoc(rootType), EnumClass),
                        rootType.enum.map {
                            EnumValue(
                                it.name
                                    ?: jsonElementToStringOrNull(it.value)
                                    ?: error("Unexpected, enum must have a name or a value ($rootType)"),
                                KDoc(it.description, it.deprecationMessage),
                            )
                        },
                    )
                }
            }
        } catch (e: InvalidPulumiName) {
            logger.warn("Invalid name", e)
            return emptyList()
        }
    }

    private fun jsonElementToStringOrNull(element: JsonElement?) =
        (element as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun createComplexTypeFields(property: ObjectProperty, context: Context, usageKind: UsageKind) =
        property.properties
            .map { (name, value) ->
                name.value to TypeAndOptionality(
                    resolveNestedTypeReference(context, value, usageKind.toNested(), property.required.contains(name)),
                    getKDoc(value),
                )
            }
            .toMap()

    private fun resolveNestedTypeReference(
        context: Context,
        property: Property,
        usageKind: UsageKind,
        isRequired: Boolean,
    ): ReferencedType {
        require(usageKind.depth != Root) { "Root properties are not supported here (usageKind was $usageKind)" }

        val referencedType = when (property) {
            is ReferenceProperty -> resolveSingleTypeReference(context, property, usageKind)
            is GenericTypeProperty -> mapGenericTypes(property) {
                resolveNestedTypeReference(
                    context,
                    it,
                    usageKind,
                    true,
                )
            }

            is PrimitiveProperty -> mapPrimitiveTypes(property)
            is ObjectProperty -> MapType(StringType, StringType)
            is StringEnumProperty -> error("Nesting not supported for ${property.javaClass}")
        }
        return if (isRequired) {
            referencedType
        } else {
            OptionalType(referencedType)
        }
    }

    private fun resolveSingleTypeReference(
        context: Context,
        property: ReferenceProperty,
        usageKind: UsageKind,
    ): ReferencedType {
        val referencedTypeName = property.referencedTypeName

        return if (property.isAssetOrArchive()) {
            AssetOrArchiveType
        } else if (property.isArchive()) {
            ArchiveType
        } else if (property.isAny()) {
            AnyType
        } else if (property.isJson()) {
            JsonType
        } else if (context.referencedStringTypesResolver.shouldGenerateStringType(referencedTypeName)) {
            StringType
        } else {
            val pulumiName = PulumiName.from(referencedTypeName, context.namingConfiguration)
            when (context.referenceFinder.resolve(referencedTypeName)) {
                is ObjectProperty -> ReferencedComplexType(
                    TypeMetadata(pulumiName, usageKind, getKDoc(property)),
                )

                is StringEnumProperty -> ReferencedEnumType(
                    TypeMetadata(pulumiName, usageKind, getKDoc(property), EnumClass),
                )

                null -> {
                    logger.info("Not found type for $referencedTypeName, defaulting to Any")
                    AnyType
                }
            }
        }
    }

    private fun getKDoc(property: Property): KDoc {
        return KDoc(property.description, property.deprecationMessage)
    }

    private fun getKDoc(resource: SchemaModel.Resource): KDoc {
        return KDoc(resource.description, resource.deprecationMessage)
    }

    private fun getKDoc(function: SchemaModel.Function): KDoc {
        return KDoc(function.description, function.deprecationMessage)
    }

    private fun mapGenericTypes(
        property: GenericTypeProperty,
        innerTypeMapper: (Property) -> ReferencedType,
    ): ReferencedType {
        return when (property) {
            is ArrayProperty -> ListType(innerTypeMapper(property.items))
            is MapProperty -> MapType(StringType, innerTypeMapper(property.additionalProperties))
            is OneOfProperty -> {
                val innerTypes = property.oneOf.map { innerTypeMapper(it) }
                val isFilledWithStringsOnly = innerTypes.all { it is StringType }

                if (isFilledWithStringsOnly) {
                    StringType
                } else if (innerTypes.size == 2) {
                    EitherType(innerTypes[0], innerTypes[1])
                } else {
                    AnyType
                }
            }
        }
    }

    private fun mapPrimitiveTypes(property: PrimitiveProperty): PrimitiveType {
        return when (property) {
            is BooleanProperty -> BooleanType
            is IntegerProperty -> IntType
            is NumberProperty -> DoubleType
            is StringProperty -> StringType
        }
    }

    /**
     * Since non-primitive provider configuration is currently JSON serialized, we can't handle it without
     * modifying the path by which it's looked up. As a temporary workaround to enable access to config which
     * values are primitives, we'll simply remove any properties for the provider resource which are not
     * strings, or types with an underlying type of string, before we generate the provider code.
     */
    private fun filterStringProperties(propertyMap: Map<PropertyName, Property>): Map<PropertyName, Property> =
        propertyMap.filter { (_, property) ->
            property is StringProperty ||
                (property is ReferenceProperty && property.type == SchemaModel.PropertyType.StringType)
        }

    private data class Context(
        val schema: Schema,
        val referenceFinder: ReferenceFinder,
        val referencedStringTypesResolver: ReferencedStringTypesResolver,
    ) {
        val namingConfiguration: PulumiNamingConfiguration =
            PulumiNamingConfiguration.create(
                schema.providerName,
                schema.metadata?.moduleFormat,
                schema.providerLanguage?.java?.basePackage,
                schema.providerLanguage?.java?.packages,
            )
    }

    private data class TypeKey(val name: PulumiName, val usageKind: UsageKind) {

        fun withLowercaseName() =
            copy(
                name = with(name) {
                    PulumiName(
                        providerName.lowercase(),
                        namespace.map { it.lowercase() },
                        name.lowercase(),
                    )
                },
            )

        companion object {
            fun from(pulumiName: PulumiName, usageKind: UsageKind): TypeKey =
                TypeKey(pulumiName, usageKind)

            fun from(metadata: TypeMetadata): TypeKey =
                from(metadata.pulumiName, metadata.usageKind)
        }
    }
}
