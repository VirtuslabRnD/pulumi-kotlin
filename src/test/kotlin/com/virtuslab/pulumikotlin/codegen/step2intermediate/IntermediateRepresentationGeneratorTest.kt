package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Function
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.FunctionsMap
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.ParsedSchema
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Resources
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Resources.IntegerProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Resources.ObjectProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Resources.PropertyName
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Resources.ReferredProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Resources.SpecificationReference
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Resources.StringProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.ResourcesMap
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.TypesMap
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic.FunctionNested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic.FunctionRoot
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic.ResourceNested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic.ResourceRoot
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
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
                    PropertyName("referencedType2") to ReferredProperty(`$ref` = SpecificationReference(ref(typeName2))),
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

        val ir = getIntermediateRepresentation(types = types, resources = resources)
        val irTypes = ir.types

        assertContainsComplexTypeThat(
            irTypes,
            isNamed = "type",
            hasSameFieldsAs = setOf("int", "referencedType2"),
            hasUsageCharacteristicOf = ResourceNested,
            willBeUsedAs = Output,
        )

        assertContainsComplexTypeThat(
            irTypes,
            isNamed = "type2",
            hasSameFieldsAs = setOf("int"),
            hasUsageCharacteristicOf = ResourceNested,
            willBeUsedAs = Output,
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

        val ir = getIntermediateRepresentation(types = types, resources = resources)
        val irTypes = ir.types

        assertContainsComplexTypeThat(
            irTypes,
            isNamed = "type",
            hasSameFieldsAs = setOf("int"),
            hasUsageCharacteristicOf = ResourceNested,
            willBeUsedAs = Input,
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

        val ir = getIntermediateRepresentation(types = types, resources = resources)
        val irTypes = ir.types

        assertContainsComplexTypeThat(
            irTypes,
            isNamed = "type",
            hasSameFieldsAs = setOf("someInt"),
            hasUsageCharacteristicOf = ResourceNested,
            willBeUsedAs = Input,
        )

        assertContainsComplexTypeThat(
            irTypes,
            isNamed = "type",
            hasSameFieldsAs = setOf("someInt"),
            hasUsageCharacteristicOf = ResourceNested,
            willBeUsedAs = Output,
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

        val ir = getIntermediateRepresentation(types = types, resources = resources, functions = functions)
        val irTypes = ir.types

        assertContainsComplexTypeThat(
            irTypes,
            isNamed = "type",
            hasSameFieldsAs = setOf("someInt"),
            hasUsageCharacteristicOf = ResourceNested,
            willBeUsedAs = Input,
        )

        assertContainsComplexTypeThat(
            irTypes,
            isNamed = "type",
            hasSameFieldsAs = setOf("someInt"),
            hasUsageCharacteristicOf = FunctionNested,
            willBeUsedAs = Input,
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
            resourceName to Resources.Resource(
                description = "any",
                inputProperties = mapOf(
                    PropertyName("someInt") to IntegerProperty(),
                    PropertyName("referencedType") to ReferredProperty(
                        `$ref` = SpecificationReference(ref(typeName)),
                    ),
                ),
            ),
        )

        val ir = getIntermediateRepresentation(types = types, resources = resources)
        val irTypes = ir.types
        val irResources = ir.resources

        assertContainsComplexTypeThat(
            irTypes,
            isNamed = "resource",
            hasSameFieldsAs = setOf("someInt", "referencedType"),
            willBeUsedAs = Input,
            hasUsageCharacteristicOf = ResourceRoot,
        )

        assertEquals(1, irResources.size)
        assertEquals(PulumiName.from(resourceName), irResources.first().name)
        assertEquals(irTypes.findComplexTypeThat(isNamed = "resource")!!, irResources.first().argsType)
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
        val resourceName = "provider:namespace/function:function"
        val functions = mapOf(
            resourceName to Function(
                description = "any",
                inputs = ObjectProperty(
                    properties = mapOf(
                        PropertyName("someInt") to IntegerProperty(),
                        PropertyName("referencedType") to ReferredProperty(
                            `$ref` = SpecificationReference(ref(typeName)),
                        ),
                    ),
                ),
                outputs = ObjectProperty(
                    properties = mapOf(
                        PropertyName("someInt2") to IntegerProperty(),
                        PropertyName("referencedType2") to ReferredProperty(
                            `$ref` = SpecificationReference(ref(typeName)),
                        ),
                    ),
                ),
            ),
        )

        val ir = getIntermediateRepresentation(types = types, functions = functions)
        val irTypes = ir.types

        assertContainsComplexTypeThat(
            irTypes,
            isNamed = "function",
            hasSameFieldsAs = setOf("someInt", "referencedType"),
            willBeUsedAs = Input,
            hasUsageCharacteristicOf = FunctionRoot,
        )

        assertContainsComplexTypeThat(
            irTypes,
            isNamed = "function",
            hasSameFieldsAs = setOf("someInt2", "referencedType2"),
            willBeUsedAs = Output,
            hasUsageCharacteristicOf = FunctionRoot,
        )

        val irFunctions = ir.functions

        assertEquals(1, irFunctions.size)
        assertEquals(PulumiName.from(resourceName), irFunctions.first().name)
        assertEquals(
            irTypes.findComplexTypeThat(isNamed = "function", willBeUsedAs = Input)!!,
            irFunctions.first().argsType,
        )
        assertEquals(
            irTypes.findComplexTypeThat(isNamed = "function", willBeUsedAs = Output)!!,
            irFunctions.first().outputType,
        )
    }

    private fun someResourceWithReferences(
        referencedInputTypeNames: List<String> = emptyList(),
        referencedOutputTypeNames: List<String> = emptyList(),
    ): Map<String, Resources.Resource> {
        fun generateField(name: String) =
            PropertyName("referenced$name") to ReferredProperty(`$ref` = SpecificationReference(ref(name)))

        val referencedInputTypes = referencedInputTypeNames.associate { generateField(it) }
        val referencedOutputTypes = referencedOutputTypeNames.associate { generateField(it) }

        return mapOf(
            randomResourceName() to Resources.Resource(
                inputProperties = mapOf(PropertyName("someInt") to IntegerProperty()) + referencedInputTypes,
                properties = mapOf(PropertyName("someString") to StringProperty()) + referencedOutputTypes,
                description = "any",
            ),
        )
    }

    private fun someFunctionsWithReferences(
        referencedInputTypeNames: List<String> = emptyList(),
        referencedOutputTypeNames: List<String> = emptyList(),
    ): Map<String, Function> {
        fun generateField(name: String) =
            PropertyName("referenced$name") to ReferredProperty(`$ref` = SpecificationReference(ref(name)))

        val referencedInputTypes = referencedInputTypeNames.associate { generateField(it) }
        val referencedOutputTypes = referencedOutputTypeNames.associate { generateField(it) }

        return mapOf(
            randomFunctionName() to Function(
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
        willBeUsedAs: InputOrOutput? = null,
        hasUsageCharacteristicOf: UseCharacteristic? = null,
    ): ComplexType? {
        fun <T> equalsIfNotNull(arg: T?, to: T) = arg == null || arg == to

        return this
            .filterIsInstance<ComplexType>()
            .find { type ->
                equalsIfNotNull(isNamed, type.metadata.pulumiName.name) &&
                    equalsIfNotNull(hasSameFieldsAs, type.fields.keys) &&
                    equalsIfNotNull(willBeUsedAs, type.metadata.inputOrOutput) &&
                    equalsIfNotNull(hasUsageCharacteristicOf, type.metadata.useCharacteristic)
            }
    }

    private fun assertContainsComplexTypeThat(
        types: List<AutonomousType>,
        isNamed: String? = null,
        hasSameFieldsAs: Set<String>? = null,
        willBeUsedAs: InputOrOutput? = null,
        hasUsageCharacteristicOf: UseCharacteristic? = null,
    ) {
        val foundType = types.findComplexTypeThat(isNamed, hasSameFieldsAs, willBeUsedAs, hasUsageCharacteristicOf)

        assertNotNull(foundType)
    }

    private fun getIntermediateRepresentation(
        types: TypesMap = emptyMap(),
        functions: FunctionsMap = emptyMap(),
        resources: ResourcesMap = emptyMap(),
    ) = IntermediateRepresentationGenerator.getIntermediateRepresentation(
        ParsedSchema(types = types, resources = resources, functions = functions),
    )

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
