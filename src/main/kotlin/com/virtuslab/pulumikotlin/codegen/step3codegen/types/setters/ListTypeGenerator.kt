package com.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.KModifier.VARARG
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedComplexType
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.BuilderSettingCodeBlock
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addDocsToBuilderMethod
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderLambda
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderPattern
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.listOfLambdas
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.mappingCodeBlock
import com.virtuslab.pulumikotlin.codegen.step3codegen.NormalField

object ListTypeGenerator : SetterGenerator {
    override fun generate(setter: Setter): Iterable<FunSpec> {
        val normalField = setter.fieldType as? NormalField<*> ?: return emptyList()
        val type = normalField.type as? ListType ?: return emptyList()

        val innerType = type.innerType

        val name = setter.name
        val kDoc = setter.kDoc

        val builderPattern = when (innerType) {
            is ReferencedComplexType -> {
                val commonCodeBlock = BuilderSettingCodeBlock
                    .create(
                        "argument.toList().map { %T().applySuspend{ it() }.build() }",
                        innerType.toBuilderTypeName(),
                    )
                    .withMappingCode(normalField.mappingCode)

                listOf(
                    builderPattern(name, listOfLambdas(innerType), kDoc, commonCodeBlock),
                    builderPattern(
                        name,
                        builderLambda(innerType),
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
                .addModifiers(SUSPEND)
                .addParameter("values", innerType.toTypeName(), VARARG)
                .addCode(mappingCodeBlock(normalField, required = false, name, "values.toList()"))
                .addDocsToBuilderMethod(kDoc, "values")
                .build(),
        )

        return builderPattern + justValuesPassedAsVarargArguments
    }
}
