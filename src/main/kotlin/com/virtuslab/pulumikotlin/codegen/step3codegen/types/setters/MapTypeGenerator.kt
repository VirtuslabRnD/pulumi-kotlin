package com.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.VARARG
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.pairClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedComplexType
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.BuilderSettingCodeBlock
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addDocsToBuilderMethod
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderLambda
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderPattern
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.mappingCodeBlock
import com.virtuslab.pulumikotlin.codegen.step3codegen.NormalField

object MapTypeGenerator : SetterGenerator {
    override fun generate(setter: Setter): Iterable<FunSpec> {
        val normalField = setter.fieldType as? NormalField<*> ?: return emptyList()
        val type = normalField.type as? MapType ?: return emptyList()

        val leftInnerType = type.firstType
        val rightInnerType = type.secondType

        val name = setter.name
        val kDoc = setter.kDoc

        val builderPattern = when (rightInnerType) {
            is ReferencedComplexType -> {
                val commonCodeBlock = BuilderSettingCodeBlock
                    .create(
                        "argument.toList().map { (left, right) -> left to %T().applySuspend{ right() }.build() }",
                        rightInnerType.toBuilderTypeName(),
                    )
                    .withMappingCode(normalField.mappingCode)

                listOf(
                    builderPattern(
                        name,
                        pairClass(leftInnerType.toTypeName(), builderLambda(rightInnerType)),
                        kDoc,
                        commonCodeBlock,
                        parameterModifiers = listOf(VARARG),
                    ),
                )
            }

            else -> emptyList()
        }

        val justValuesPassedAsVarargArguments = listOf(
            FunSpec
                .builder(name)
                .addParameter(
                    "values",
                    pairClass(leftInnerType.toTypeName(), rightInnerType.toTypeName()),
                    VARARG,
                )
                .addCode(mappingCodeBlock(normalField, required = false, name, "values.toMap()"))
                .addDocsToBuilderMethod(kDoc, "values")
                .build(),
        )

        return builderPattern + justValuesPassedAsVarargArguments
    }
}
