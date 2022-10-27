package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.ParsedSchema
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
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ReferenceProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.RootTypeProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.StringEnumProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.StringProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.isAny
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.isArchive
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.isAssetOrArchive
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.referencedTypeName
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
import com.virtuslab.pulumikotlin.codegen.utils.filterNotNullValues

/**
 * Takes parsed schema as an input and produces types that are prepared for code generation. More specifically:
 * - it finds out which functions and resources reference particular types (recursively)
 * - it finds out which types will be used as inputs / outputs
 * - it generates synthetic types from resources (input properties) and functions (inputs and outputs)
 */
object IntermediateRepresentationGenerator {

    fun getIntermediateRepresentation(schema: ParsedSchema): IntermediateRepresentation {
        val referenceFinder = ReferenceFinder(schema)
        val context = Context(schema, referenceFinder)

        val types = createTypes(context)
        val typeMap = types
            .associateBy { TypeKey.from(it.metadata) }
            .transformKeys { it.withLowercaseName() }

        return IntermediateRepresentation(
            types = types,
            resources = createResources(typeMap, context),
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
            createTypes(schema.functions, UsageKind(Root, Function, Input)) { it.inputs },
            createTypes(schema.functions, UsageKind(Root, Function, Output)) { it.outputs },
            createTypes(schema.resources, UsageKind(Root, Resource, Input)) {
                ObjectProperty(
                    properties = it.inputProperties,
                    description = it.description,
                    deprecationMessage = it.deprecationMessage,
                )
            },
        )

        val regularTypes = listOf(
            createTypes(schema.types) { it },
        )

        return (syntheticTypes + regularTypes).flatten()
    }

    private fun createResources(types: Map<TypeKey, RootType>, context: Context): List<ResourceType> {
        return context.schema.resources.map { (typeName, resource) ->
            val resultFields = resource.properties.map { (propertyName, property) ->
                val outputFieldsUsageKind = UsageKind(Nested, Resource, Output)
                val isRequired = resource.required.contains(propertyName)
                val reference = resolveNestedTypeReference(context, property, outputFieldsUsageKind)
                Field(propertyName.value, OutputWrappedField(reference), isRequired, kDoc = getKDoc(property))
            }

            val pulumiName = PulumiName.from(typeName, context.namingConfiguration)
            val inputUsageKind = UsageKind(Root, Resource, Input)
            val argumentType =
                findTypeAsReference<ReferencedComplexType>(
                    types,
                    TypeKey.from(pulumiName, inputUsageKind),
                )
            ResourceType(pulumiName, argumentType, resultFields, getKDoc(resource))
        }
    }

    private fun createFunctions(types: Map<TypeKey, RootType>, context: Context): List<FunctionType> {
        return context.schema.functions.map { (typeName, function) ->
            val pulumiName = PulumiName.from(typeName, context.namingConfiguration)

            val inputUsageKind = UsageKind(Root, Function, Input)
            val argumentType = findTypeOrEmptyComplexType(
                types,
                TypeKey.from(pulumiName, inputUsageKind),
                getKDoc(function),
            )

            val outputUsageKind = UsageKind(Root, Function, Output)
            val resultType = findTypeAsReference<ReferencedRootType>(types, TypeKey.from(pulumiName, outputUsageKind))

            FunctionType(pulumiName, argumentType, resultType, getKDoc(function))
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
            if (allUsagesForTypeName.isEmpty()) {
                println("$typeName references were empty for $typeName (${rootType.javaClass})")
            }
            allUsagesForTypeName
        }

        val pulumiName = PulumiName.from(typeName, context.namingConfiguration)
        return usages.map { usage ->
            when (rootType) {
                is ObjectProperty -> ComplexType(
                    TypeMetadata(pulumiName, usage, getKDoc(rootType), NormalClass),
                    createComplexTypeFields(rootType, context, usage),
                )

                is StringEnumProperty -> EnumType(
                    TypeMetadata(pulumiName, usage, getKDoc(rootType), EnumClass),
                    rootType.enum.map { it.name ?: it.value },
                )
            }
        }
    }

    private fun createComplexTypeFields(property: ObjectProperty, context: Context, usageKind: UsageKind) =
        property.properties
            .map { (name, value) ->
                name.value to TypeAndOptionality(
                    resolveNestedTypeReference(context, value, usageKind.toNested()),
                    property.required.contains(name),
                    getKDoc(value),
                )
            }
            .toMap()

    private fun resolveNestedTypeReference(context: Context, property: Property, usageKind: UsageKind): ReferencedType {
        require(usageKind.depth != Root) { "Root properties are not supported here (usageKind was $usageKind)" }

        return when (property) {
            is ReferenceProperty -> resolveSingleTypeReference(context, property, usageKind)
            is GenericTypeProperty -> mapGenericTypes(property) { resolveNestedTypeReference(context, it, usageKind) }
            is PrimitiveProperty -> mapPrimitiveTypes(property)
            is ObjectProperty, is StringEnumProperty -> error("Nesting not supported for ${property.javaClass}")
        }
    }

    private fun resolveSingleTypeReference(
        context: Context,
        property: ReferenceProperty,
        usageKind: UsageKind,
    ): ReferencedType {
        if (property.isAssetOrArchive()) {
            return AssetOrArchiveType
        } else if (property.isArchive()) {
            return ArchiveType
        } else if (property.isAny()) {
            return AnyType
        }

        val referencedTypeName = property.referencedTypeName
        val pulumiName = PulumiName.from(referencedTypeName, context.namingConfiguration)
        return when (context.referenceFinder.resolve(referencedTypeName)) {
            is ObjectProperty -> ReferencedComplexType(
                TypeMetadata(pulumiName, usageKind, getKDoc(property)),
            )

            is StringEnumProperty -> ReferencedEnumType(
                TypeMetadata(pulumiName, usageKind, getKDoc(property), EnumClass),
            )

            null -> {
                println("Not found type for $referencedTypeName, defaulting to Any")
                return AnyType
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
            is OneOfProperty -> EitherType(innerTypeMapper(property.oneOf[0]), innerTypeMapper(property.oneOf[1]))
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

    private data class Context(
        val schema: ParsedSchema,
        val referenceFinder: ReferenceFinder,
        val namingConfiguration: PulumiNamingConfiguration =
            PulumiNamingConfiguration.create(
                schema.providerName,
                schema.meta?.moduleFormat,
                schema.language?.java?.basePackage,
                schema.language?.java?.packages,
            ),
    )

    private data class TypeKey(val name: PulumiName, val usageKind: UsageKind) {

        fun withLowercaseName() =
            copy(
                name = with(name) {
                    PulumiName(
                        packageProviderName.lowercase(),
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
