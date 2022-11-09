package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PulumiName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UsageKind
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class TypeNameClashResolverTest {

    @Test
    fun `throws exception when trying to create nested function input that clashes with explicit type in Kotlin`() {
        val explicitType = ComplexType(
            createMetadata("FunctionNamePlainArgs", Nested, Function, Input),
            emptyMap(),
        )

        val typeNameClashResolver = TypeNameClashResolver(listOf(explicitType))

        val syntheticTypeMetadata = createMetadata("FunctionName", Root, Function, Input)

        assertEquals(
            typeNameClashResolver.kotlinNames(explicitType.metadata).kotlinPoetClassName.canonicalName,
            "some.package.kotlin.inputs.FunctionNamePlainArgs",
        )
        assertThrows<IllegalStateException>(getErrorMessage(Function, Input, Kotlin)) {
            typeNameClashResolver.kotlinNames(syntheticTypeMetadata).kotlinPoetClassName
        }
    }

    @Test
    fun `throws exception when trying to create nested function input that clashes with explicit type in Java`() {
        val explicitType = ComplexType(
            createMetadata("FunctionNamePlainArgs", Nested, Function, Input),
            emptyMap(),
        )

        val typeNameClashResolver = TypeNameClashResolver(listOf(explicitType))

        val syntheticTypeMetadata = createMetadata("FunctionName", Root, Function, Input)

        assertEquals(
            typeNameClashResolver.javaNames(explicitType.metadata).kotlinPoetClassName.canonicalName,
            "some.package.inputs.FunctionNamePlainArgs",
        )
        assertThrows<IllegalStateException>(getErrorMessage(Function, Input, Java)) {
            typeNameClashResolver.javaNames(syntheticTypeMetadata).kotlinPoetClassName
        }
    }

    @Test
    fun `uses alternative suffix to create nested function output that clashes with explicit type in Kotlin`() {
        val explicitType = ComplexType(
            createMetadata("FunctionNameResult", Nested, Function, Output),
            emptyMap(),
        )

        val typeNameClashResolver = TypeNameClashResolver(listOf(explicitType))

        val syntheticTypeMetadata = createMetadata("FunctionName", Root, Function, Output)

        assertEquals(
            "some.package.kotlin.outputs.FunctionNameResult",
            typeNameClashResolver.kotlinNames(explicitType.metadata).kotlinPoetClassName.canonicalName,
        )
        assertEquals(
            "some.package.kotlin.outputs.FunctionNameInvokeResult",
            typeNameClashResolver.kotlinNames(syntheticTypeMetadata).kotlinPoetClassName.canonicalName,
        )
    }

    @Test
    fun `uses alternative suffix to create nested function output that clashes with explicit type in Java`() {
        val explicitType = ComplexType(
            createMetadata("FunctionNameResult", Nested, Function, Output),
            emptyMap(),
        )

        val typeNameClashResolver = TypeNameClashResolver(listOf(explicitType))

        val syntheticTypeMetadata = createMetadata("FunctionName", Root, Function, Output)

        assertEquals(
            "some.package.outputs.FunctionNameResult",
            typeNameClashResolver.javaNames(explicitType.metadata).kotlinPoetClassName.canonicalName,
        )
        assertEquals(
            "some.package.outputs.FunctionNameInvokeResult",
            typeNameClashResolver.javaNames(syntheticTypeMetadata).kotlinPoetClassName.canonicalName,
        )
    }

    @Test
    fun `creates nested resource input whose name clashes with explicit type in Kotlin (different package)`() {
        val explicitType = ComplexType(
            createMetadata("ResourceName", Nested, Resource, Input),
            emptyMap(),
        )

        val typeNameClashResolver = TypeNameClashResolver(listOf(explicitType))

        val typeMetadata = createMetadata("ResourceName", Root, Resource, Input)

        assertEquals(
            "some.package.kotlin.inputs.ResourceNameArgs",
            typeNameClashResolver.kotlinNames(explicitType.metadata).kotlinPoetClassName.canonicalName,
        )
        assertEquals(
            "some.package.kotlin.ResourceNameArgs",
            typeNameClashResolver.kotlinNames(typeMetadata).kotlinPoetClassName.canonicalName,
        )
    }

    @Test
    fun `creates nested resource input whose name clashes with explicit type in Java (different package)`() {
        val explicitType = ComplexType(
            createMetadata("ResourceName", Nested, Resource, Input),
            emptyMap(),
        )

        val typeNameClashResolver = TypeNameClashResolver(listOf(explicitType))

        val typeMetadata = createMetadata("ResourceName", Root, Resource, Input)

        assertEquals(
            "some.package.inputs.ResourceNameArgs",
            typeNameClashResolver.javaNames(explicitType.metadata).kotlinPoetClassName.canonicalName,
        )
        assertEquals(
            "some.package.ResourceNameArgs",
            typeNameClashResolver.javaNames(typeMetadata).kotlinPoetClassName.canonicalName,
        )
    }

    @Test
    fun `throws exception when trying to create nested resource output that clashes with explicit type in Kotlin`() {
        val explicitType = ComplexType(
            createMetadata("ResourceName", Nested, Resource, Output),
            emptyMap(),
        )
        val typeNameClashResolver = TypeNameClashResolver(listOf(explicitType))

        val typeMetadata = createMetadata("ResourceName", Root, Resource, Output)

        assertEquals(
            "some.package.kotlin.outputs.ResourceName",
            typeNameClashResolver.kotlinNames(explicitType.metadata).kotlinPoetClassName.canonicalName,
        )
        assertThrows<IllegalStateException>(getErrorMessage(Resource, Output, Kotlin)) {
            typeNameClashResolver.kotlinNames(typeMetadata).kotlinPoetClassName
        }
    }

    @Test
    fun `throws exception when trying to create nested resource output that clashes with explicit type in Java`() {
        val explicitType = ComplexType(
            createMetadata("ResourceName", Nested, Resource, Output),
            emptyMap(),
        )
        val typeNameClashResolver = TypeNameClashResolver(listOf(explicitType))

        val typeMetadata = createMetadata("ResourceName", Root, Resource, Output)

        assertEquals(
            "some.package.outputs.ResourceName",
            typeNameClashResolver.javaNames(explicitType.metadata).kotlinPoetClassName.canonicalName,
        )
        assertThrows<IllegalStateException>(getErrorMessage(Resource, Output, Java)) {
            typeNameClashResolver.javaNames(typeMetadata).kotlinPoetClassName
        }
    }

    private fun createMetadata(name: String, depth: Depth, subject: Subject, direction: Direction) = TypeMetadata(
        PulumiName("provider", listOf("some", "package"), name),
        UsageKind(depth, subject, direction),
        KDoc(null, null),
    )

    private fun getErrorMessage(subject: Subject, direction: Direction, languageType: LanguageType) =
        "No name suffix configured to deal with naming conflict. " +
            "Name: ${subject}Name. " +
            "Naming flags: NamingFlags(" +
            "depth=Root, " +
            "subject=$subject, " +
            "direction=$direction, " +
            "language=$languageType, " +
            "generatedClass=NormalClass, " +
            "useAlternativeName=true" +
            ")"
}
