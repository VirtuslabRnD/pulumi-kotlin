package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.FunctionsMap
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.ParsedSchema
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.ResourcesMap
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.IntegerProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ObjectProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyName
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ReferenceProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.SpecificationReference
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.StringProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.TypesMap
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import com.virtuslab.pulumikotlin.namingConfigurationWithSlashInModuleFormat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.Random
import kotlin.streams.asSequence
import kotlin.test.assertEquals

internal class IntermediateRepresentationGeneratorTest {

    @Test
    fun `types used in resource outputs (even if nested) are properly recognized and converted to ComplexType`() {
        val typeName1 = "provider:namespace/type:type"
        val typeName2 = "provider:namespace/type:type2"
        val types = mapOf(
            typeName1 to ObjectProperty(
                properties = mapOf(
                    PropertyName("referencedType2") to ReferenceProperty(
                        ref = SpecificationReference(ref(typeName2)),
                    ),
                    PropertyName("int") to IntegerProperty(),
                ),
            ),
            typeName2 to ObjectProperty(
                properties = mapOf(
                    PropertyName("int") to IntegerProperty(),
                ),
            ),
        )
        val resources = someResourceWithOutputReferences(typeName1)

        val ir = getIntermediateRepresentation(
            providerName = "provider",
            types = types,
            resources = resources,
        )
        val irTypes = ir.types

        assertContainsComplexTypeThat(
            irTypes,
            nameIs = "type",
            fieldsAre = setOf("int", "referencedType2"),
            usageKindIs = UsageKind(Nested, Resource, Output),
        )

        assertContainsComplexTypeThat(
            irTypes,
            nameIs = "type2",
            fieldsAre = setOf("int"),
            usageKindIs = UsageKind(Nested, Resource, Output),
        )
    }

    @Test
    fun `types used in resource inputs are properly recognized and converted to ComplexType`() {
        val typeName = "provider:namespace/type:type"
        val types = mapOf(
            typeName to ObjectProperty(
                properties = mapOf(
                    PropertyName("int") to IntegerProperty(),
                ),
            ),
        )
        val resources = someResourceWithInputReferences(typeName)

        val ir = getIntermediateRepresentation(
            providerName = "provider",
            types = types,
            resources = resources,
        )
        val irTypes = ir.types

        assertContainsComplexTypeThat(
            irTypes,
            nameIs = "type",
            fieldsAre = setOf("int"),
            usageKindIs = UsageKind(Nested, Resource, Input),
        )
    }

    @Test
    fun `types used in multiple places (input and output) are re-created`() {
        val typeName = "provider:namespace/type:type"
        val types = mapOf(
            typeName to ObjectProperty(
                properties = mapOf(
                    PropertyName("someInt") to IntegerProperty(),
                ),
            ),
        )
        val resources = someResourceWithReferences(
            referencedInputTypeNames = listOf(typeName),
            referencedOutputTypeNames = listOf(typeName),
        )

        val ir = getIntermediateRepresentation(
            providerName = "provider",
            types = types,
            resources = resources,
        )
        val irTypes = ir.types

        assertContainsComplexTypeThat(
            irTypes,
            nameIs = "type",
            fieldsAre = setOf("someInt"),
            usageKindIs = UsageKind(Nested, Resource, Input),
        )

        assertContainsComplexTypeThat(
            irTypes,
            nameIs = "type",
            fieldsAre = setOf("someInt"),
            usageKindIs = UsageKind(Nested, Resource, Output),
        )
    }

    @Test
    fun `types used in multiple places (functions and resource) are re-created`() {
        val typeName = "provider:namespace/type:type"
        val types = mapOf(
            typeName to ObjectProperty(
                properties = mapOf(
                    PropertyName("someInt") to IntegerProperty(),
                ),
            ),
        )
        val resources = someResourceWithReferences(
            referencedInputTypeNames = listOf(typeName),
        )
        val functions = someFunctionsWithReferences(
            referencedInputTypeNames = listOf(typeName),
        )

        val ir = getIntermediateRepresentation(
            providerName = "provider",
            types = types,
            resources = resources,
            functions = functions,
        )
        val irTypes = ir.types

        assertContainsComplexTypeThat(
            irTypes,
            nameIs = "type",
            fieldsAre = setOf("someInt"),
            usageKindIs = UsageKind(Nested, Resource, Input),
        )

        assertContainsComplexTypeThat(
            irTypes,
            nameIs = "type",
            fieldsAre = setOf("someInt"),
            usageKindIs = UsageKind(Nested, Function, Input),
        )
    }

    @Test
    fun `resource and synthetic types for the resource (from input properties) are generated`() {
        val typeName = "provider:namespace/type:type"
        val types = mapOf(
            typeName to ObjectProperty(
                properties = mapOf(
                    PropertyName("someInt") to IntegerProperty(),
                ),
            ),
        )
        val resourceName = "provider:namespace/resource:resource"
        val resources = mapOf(
            resourceName to SchemaModel.Resource(
                description = "any",
                inputProperties = mapOf(
                    PropertyName("someInt") to IntegerProperty(),
                    PropertyName("referencedType") to ReferenceProperty(
                        ref = SpecificationReference(ref(typeName)),
                    ),
                ),
            ),
        )

        val namingConfiguration = namingConfigurationWithSlashInModuleFormat("provider")
        val ir = getIntermediateRepresentation(
            providerName = "provider",
            types = types,
            resources = resources,
        )
        val irTypes = ir.types
        val irResources = ir.resources

        assertContainsComplexTypeThat(
            irTypes,
            nameIs = "resource",
            fieldsAre = setOf("someInt", "referencedType"),
            usageKindIs = UsageKind(Root, Resource, Input),
        )

        assertEquals(1, irResources.size)
        assertEquals(PulumiName.from(resourceName, namingConfiguration), irResources.first().name)
        assertEquals(irTypes.findComplexTypeThat(isNamed = "resource")!!.toReference(), irResources.first().argsType)
    }

    @Test
    fun `function and synthetic types for the function are generated`() {
        val typeName = "provider:namespace/type:type"
        val types = mapOf(
            typeName to ObjectProperty(
                properties = mapOf(
                    PropertyName("someInt") to IntegerProperty(),
                ),
            ),
        )
        val functionName = "provider:namespace/function:function"
        val functions = mapOf(
            functionName to SchemaModel.Function(
                description = "any",
                inputs = ObjectProperty(
                    properties = mapOf(
                        PropertyName("someInt") to IntegerProperty(),
                        PropertyName("referencedType") to ReferenceProperty(
                            ref = SpecificationReference(ref(typeName)),
                        ),
                    ),
                ),
                outputs = ObjectProperty(
                    properties = mapOf(
                        PropertyName("someInt2") to IntegerProperty(),
                        PropertyName("referencedType2") to ReferenceProperty(
                            ref = SpecificationReference(ref(typeName)),
                        ),
                    ),
                ),
            ),
        )

        val namingConfiguration = namingConfigurationWithSlashInModuleFormat("provider")
        val ir = getIntermediateRepresentation(
            providerName = "provider",
            types = types,
            functions = functions,
        )
        val irTypes = ir.types

        assertContainsComplexTypeThat(
            irTypes,
            nameIs = "function",
            fieldsAre = setOf("someInt", "referencedType"),
            usageKindIs = UsageKind(Root, Function, Input),
        )

        assertContainsComplexTypeThat(
            irTypes,
            nameIs = "function",
            fieldsAre = setOf("someInt2", "referencedType2"),
            usageKindIs = UsageKind(Root, Function, Output),
        )

        val irFunctions = ir.functions

        assertEquals(1, irFunctions.size)
        assertEquals(PulumiName.from(functionName, namingConfiguration), irFunctions.first().name)
        assertEquals(
            irTypes.findComplexTypeThat(isNamed = "function", hasUsageOfKind = UsageKind(Root, Function, Input))!!,
            irFunctions.first().argsType,
        )
        assertEquals(
            irTypes
                .findComplexTypeThat(isNamed = "function", hasUsageOfKind = UsageKind(Root, Function, Output))!!
                .toReference(),
            irFunctions.first().outputType,
        )
    }

    @Test
    fun `types reference resolution is case insensitive`() {
        val typeName = "provider:namespace/type:type"
        val types = mapOf(
            typeName to ObjectProperty(
                properties = mapOf(
                    PropertyName("someInt") to IntegerProperty(),
                ),
            ),
        )
        val functionName = "provider:namespace/function:function"
        val functions = mapOf(
            functionName to SchemaModel.Function(
                description = "any",
                outputs = ObjectProperty(
                    properties = mapOf(
                        PropertyName("someInt") to IntegerProperty(),
                        PropertyName("referencedType") to ReferenceProperty(
                            ref = SpecificationReference(ref("provider:Namespace/Type:Type")),
                        ),
                    ),
                ),
            ),
        )

        val ir = getIntermediateRepresentation(
            providerName = "provider",
            types = types,
            functions = functions,
        )
        val irTypes = ir.types

        assertContainsComplexTypeThat(
            irTypes,
            nameIs = "type",
            fieldsAre = setOf("someInt"),
            usageKindIs = UsageKind(Nested, Function, Output),
        )
    }

    enum class CircularReferenceCase(
        val types: Map<String, ObjectProperty>,
        val complexTypeNameToFields: Map<String, Set<String>>,
    ) {
        SelfReferencedTypes(
            types = mapOf(
                "provider:namespace/type:type" to ObjectProperty(
                    properties = mapOf(
                        PropertyName("referencedType")
                            to ReferenceProperty(ref = SpecificationReference("#/types/provider:namespace/type:type")),
                    ),
                ),
            ),
            complexTypeNameToFields = mapOf(
                "type" to setOf("referencedType"),
            ),
        ),
        IndirectReferencedTypes(
            types = mapOf(
                "provider:namespace/type:type1" to ObjectProperty(
                    properties = mapOf(
                        PropertyName("referencedType2")
                            to ReferenceProperty(ref = SpecificationReference("#/types/provider:namespace/type:type2")),
                    ),
                ),
                "provider:namespace/type:type2" to ObjectProperty(
                    properties = mapOf(
                        PropertyName("referencedType1")
                            to ReferenceProperty(ref = SpecificationReference("#/types/provider:namespace/type:type1")),
                    ),
                ),
            ),
            complexTypeNameToFields = mapOf(
                "type1" to setOf("referencedType2"),
                "type2" to setOf("referencedType1"),
            ),
        ),
        TransitiveReferencedTypes(
            types = mapOf(
                "provider:namespace/type:type1" to ObjectProperty(
                    properties = mapOf(
                        PropertyName("referencedType2")
                            to ReferenceProperty(ref = SpecificationReference("#/types/provider:namespace/type:type2")),
                    ),
                ),
                "provider:namespace/type:type2" to ObjectProperty(
                    properties = mapOf(
                        PropertyName("referencedType3")
                            to ReferenceProperty(ref = SpecificationReference("#/types/provider:namespace/type:type3")),
                    ),
                ),
                "provider:namespace/type:type3" to ObjectProperty(
                    properties = mapOf(
                        PropertyName("referencedType1")
                            to ReferenceProperty(ref = SpecificationReference("#/types/provider:namespace/type:type1")),
                    ),
                ),
            ),
            complexTypeNameToFields = mapOf(
                "type1" to setOf("referencedType2"),
                "type2" to setOf("referencedType3"),
                "type3" to setOf("referencedType1"),
            ),
        ),
        DiamondReferencedTypes(
            types = mapOf(
                "provider:namespace/type:type1" to ObjectProperty(
                    properties = mapOf(
                        PropertyName("referencedType2")
                            to ReferenceProperty(ref = SpecificationReference("#/types/provider:namespace/type:type2")),
                        PropertyName("referencedType3")
                            to ReferenceProperty(ref = SpecificationReference("#/types/provider:namespace/type:type3")),
                    ),
                ),
                "provider:namespace/type:type2" to ObjectProperty(
                    properties = mapOf(
                        PropertyName("referencedType4")
                            to ReferenceProperty(ref = SpecificationReference("#/types/provider:namespace/type:type4")),
                    ),
                ),
                "provider:namespace/type:type3" to ObjectProperty(
                    properties = mapOf(
                        PropertyName("referencedType4")
                            to ReferenceProperty(ref = SpecificationReference("#/types/provider:namespace/type:type4")),
                    ),
                ),
                "provider:namespace/type:type4" to ObjectProperty(),
            ),
            complexTypeNameToFields = mapOf(
                "type1" to setOf("referencedType2", "referencedType3"),
                "type2" to setOf("referencedType4"),
                "type3" to setOf("referencedType4"),
                "type4" to setOf(),
            ),
        ),
    }

    @ParameterizedTest
    @EnumSource(CircularReferenceCase::class)
    fun `circular referenced types in resource inputs are properly recognized and converted to ComplexType`(
        case: CircularReferenceCase,
    ) {
        // given
        // token = provider : module : name
        val providerName = case.types.keys.first().split(":").first()
        val resources = someResourceWithInputReferences(case.types.keys.first())

        // when
        val ir = getIntermediateRepresentation(providerName = providerName, types = case.types, resources = resources)

        // then
        val assertions = case.complexTypeNameToFields.map { (name, fields) ->
            Executable {
                assertContainsComplexTypeThat(
                    ir.types,
                    nameIs = name,
                    fieldsAre = fields,
                    usageKindIs = UsageKind(Nested, Resource, Input),
                )
            }
        }

        assertAll(assertions)
    }

    private fun someResourceWithReferences(
        referencedInputTypeNames: List<String> = emptyList(),
        referencedOutputTypeNames: List<String> = emptyList(),
    ): Map<String, SchemaModel.Resource> {
        fun generateField(name: String) =
            PropertyName("referenced$name") to ReferenceProperty(ref = SpecificationReference(ref(name)))

        val referencedInputTypes = referencedInputTypeNames.associate { generateField(it) }
        val referencedOutputTypes = referencedOutputTypeNames.associate { generateField(it) }

        return mapOf(
            randomResourceName() to SchemaModel.Resource(
                inputProperties = mapOf(PropertyName("someInt") to IntegerProperty()) + referencedInputTypes,
                properties = mapOf(PropertyName("someString") to StringProperty()) + referencedOutputTypes,
                description = "any",
            ),
        )
    }

    private fun someFunctionsWithReferences(
        referencedInputTypeNames: List<String> = emptyList(),
        referencedOutputTypeNames: List<String> = emptyList(),
    ): Map<String, SchemaModel.Function> {
        fun generateField(name: String) =
            PropertyName("referenced$name") to ReferenceProperty(ref = SpecificationReference(ref(name)))

        val referencedInputTypes = referencedInputTypeNames.associate { generateField(it) }
        val referencedOutputTypes = referencedOutputTypeNames.associate { generateField(it) }

        return mapOf(
            randomFunctionName() to SchemaModel.Function(
                inputs = ObjectProperty(
                    properties = mapOf(PropertyName("someInt") to IntegerProperty()) + referencedInputTypes,
                ),
                outputs = ObjectProperty(
                    properties = mapOf(PropertyName("someString") to StringProperty()) + referencedOutputTypes,
                ),
                description = "any",
            ),
        )
    }

    private fun Iterable<Type>.findComplexTypeThat(
        isNamed: String? = null,
        hasSameFieldsAs: Set<String>? = null,
        hasUsageOfKind: UsageKind? = null,
    ): ComplexType? {
        fun <T> equalsIfNotNull(arg: T?, to: T) = arg == null || arg == to

        return this
            .filterIsInstance<ComplexType>()
            .find { type ->
                equalsIfNotNull(isNamed, type.metadata.pulumiName.name) &&
                    equalsIfNotNull(hasSameFieldsAs, type.fields.keys) &&
                    equalsIfNotNull(hasUsageOfKind, type.metadata.usageKind)
            }
    }

    private fun assertContainsComplexTypeThat(
        types: List<RootType>,
        nameIs: String? = null,
        fieldsAre: Set<String>? = null,
        usageKindIs: UsageKind? = null,
    ) {
        val foundType = types.findComplexTypeThat(nameIs, fieldsAre, usageKindIs)

        assertNotNull(foundType)
    }

    @Suppress("LongParameterList") // these parameters are required to create PulumiNamingConfiguration
    private fun getIntermediateRepresentation(
        providerName: String,
        types: TypesMap = emptyMap(),
        functions: FunctionsMap = emptyMap(),
        resources: ResourcesMap = emptyMap(),
        meta: SchemaModel.Metadata? = getMetaWithSlashInModuleFormat(),
        language: SchemaModel.PackageLanguage? = null,
    ) = IntermediateRepresentationGenerator.getIntermediateRepresentation(
        ParsedSchema(
            providerName = providerName,
            types = types,
            resources = resources,
            functions = functions,
            meta = meta,
            language = language,
        ),
    )

    private fun getMetaWithSlashInModuleFormat() = SchemaModel.Metadata("(.*)(?:/[^/]*)")

    private fun someResourceWithInputReferences(vararg referencedInputTypeNames: String) =
        someResourceWithReferences(referencedInputTypeNames = referencedInputTypeNames.toList())

    private fun someResourceWithOutputReferences(vararg referencedOutputTypeNames: String) =
        someResourceWithReferences(referencedOutputTypeNames = referencedOutputTypeNames.toList())

    private fun ref(typeName: String): String {
        return "#/types/$typeName"
    }

    private fun randomResourceName(): String {
        val randomSuffix = randomString()
        return "provider:namespace/resourcer$randomSuffix:resource$randomSuffix"
    }

    private fun randomFunctionName(): String {
        val randomSuffix = randomString()
        return "provider:namespace/getSomething$randomSuffix:getSomething$randomSuffix"
    }

    private fun randomString(length: Int = 6) = randomCharacterSequence().take(length).joinToString("")

    private fun randomCharacterSequence() =
        Random().ints('a'.code, 'z'.code).asSequence().map { it.toChar() }
}
