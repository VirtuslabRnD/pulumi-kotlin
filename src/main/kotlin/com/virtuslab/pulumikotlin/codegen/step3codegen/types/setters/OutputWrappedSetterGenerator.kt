package com.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addDocsToBuilderMethod
import com.virtuslab.pulumikotlin.codegen.step3codegen.OutputWrappedField
import com.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

object OutputWrappedSetterGenerator : SetterGenerator {
    override fun generate(setter: Setter, typeNameClashResolver: TypeNameClashResolver): Iterable<FunSpec> {
        val outputWrappedField = setter.fieldType as? OutputWrappedField<ReferencedType> ?: return emptyList()

        val type = if (outputWrappedField.type is OptionalType) {
            outputWrappedField.type.innerType
        } else {
            outputWrappedField.type
        }

        return listOf(
            FunSpec
                .builder(setter.name)
                .addModifiers(SUSPEND)
                .addParameter("value", typeNameClashResolver.toTypeName(OutputWrappedField(type), Kotlin))
                .addCode("this.%N = value", setter.name)
                .addDocsToBuilderMethod(setter.kDoc, "value")
                .build(),
        )
    }
}
