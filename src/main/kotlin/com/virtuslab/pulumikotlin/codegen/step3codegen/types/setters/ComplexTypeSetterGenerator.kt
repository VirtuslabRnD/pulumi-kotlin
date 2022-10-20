package com.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedComplexType
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.BuilderSettingCodeBlock
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderLambda
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderPattern
import com.virtuslab.pulumikotlin.codegen.step3codegen.NormalField

object ComplexTypeSetterGenerator : SetterGenerator {
    override fun generate(setter: Setter): Iterable<FunSpec> {
        val typedField = setter.fieldType as? NormalField<*> ?: return emptyList()
        val type = typedField.type as? ReferencedComplexType ?: return emptyList()

        val builderTypeName = type.toBuilderTypeName()

        return listOf(
            builderPattern(
                setter.name,
                builderLambda(builderTypeName),
                setter.kDoc,
                BuilderSettingCodeBlock
                    .create("%T().applySuspend{ argument() }.build()", builderTypeName)
                    .withMappingCode(typedField.mappingCode),
            ),
        )
    }
}
