package com.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addDocsToBuilderMethod
import com.virtuslab.pulumikotlin.codegen.step3codegen.OutputWrappedField

object OutputWrappedSetterGenerator : SetterGenerator {
    override fun generate(setter: Setter): Iterable<FunSpec> {
        val outputWrappedField = setter.fieldType as? OutputWrappedField<ReferencedType> ?: return emptyList()

        return listOf(
            FunSpec
                .builder(setter.name)
                .addModifiers(SUSPEND)
                .addParameter("value", outputWrappedField.toTypeName())
                .addCode("this.%N = value", setter.name)
                .addDocsToBuilderMethod(setter.kDoc, "value")
                .build(),
        )
    }
}
