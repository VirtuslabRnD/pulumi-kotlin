package org.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.pulumi.core.Output
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Java.Pulumi.outputClass
import org.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addDocsToBuilderMethod
import org.virtuslab.pulumikotlin.codegen.step3codegen.OutputWrappedField
import org.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

object OutputWrappedSetterGenerator : SetterGenerator {
    override fun generate(setter: Setter, typeNameClashResolver: TypeNameClashResolver): Iterable<FunSpec> {
        val outputWrappedField = setter.fieldType as? OutputWrappedField<ReferencedType> ?: return emptyList()

        val type = if (outputWrappedField.type is OptionalType) {
            outputWrappedField.type.innerType
        } else {
            outputWrappedField.type
        }

        val additionalListSetters = when (type) {
            is ListType -> {
                listOf(
                    createVarargOutputSetter(setter, typeNameClashResolver, type),
                    createOutputListSetter(setter, typeNameClashResolver, type),
                )
            }

            else -> emptyList()
        }

        val basicSetter = listOf(
            FunSpec
                .builder(setter.name)
                .addModifiers(SUSPEND)
                .addParameter("value", typeNameClashResolver.toTypeName(OutputWrappedField(type), Kotlin))
                .addCode("this.%N = value", setter.name)
                .addDocsToBuilderMethod(setter.kDoc, "value")
                .build(),
        )

        return basicSetter + additionalListSetters
    }

    private fun createOutputListSetter(
        setter: Setter,
        typeNameClashResolver: TypeNameClashResolver,
        innerType: ListType,
    ) = FunSpec
        .builder(setter.name)
        .addModifiers(SUSPEND)
        .addParameter(
            "values",
            List::class.asClassName().parameterizedBy(
                outputClass().parameterizedBy(
                    typeNameClashResolver.toTypeName(
                        innerType.innerType,
                        Kotlin,
                    ),
                ),
            ),
        )
        .addCode("this.%N = %T.all(values)", setter.name, Output::class)
        .addDocsToBuilderMethod(setter.kDoc, "values")
        .build()

    private fun createVarargOutputSetter(
        setter: Setter,
        typeNameClashResolver: TypeNameClashResolver,
        innerType: ListType,
    ) = FunSpec
        .builder(setter.name)
        .addModifiers(SUSPEND)
        .addParameter(
            "values",
            outputClass().parameterizedBy(
                typeNameClashResolver.toTypeName(
                    innerType.innerType,
                    Kotlin,
                ),
            ),
            VARARG,
        )
        .addCode("this.%N = %T.all(values.asList())", setter.name, Output::class)
        .build()
}
