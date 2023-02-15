package org.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.KModifier.VARARG
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedComplexType
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.BuilderSettingCodeBlock
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addDocsToBuilderMethod
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderLambda
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderPattern
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.listOfLambdas
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.mappingCodeBlock
import org.virtuslab.pulumikotlin.codegen.step3codegen.NormalField
import org.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

object ListTypeSetterGenerator : SetterGenerator {
    override fun generate(setter: Setter, typeNameClashResolver: TypeNameClashResolver): Iterable<FunSpec> {
        val normalField = setter.fieldType as? NormalField<*> ?: return emptyList()

        val type = if (normalField.type is ListType) {
            normalField.type
        } else if (normalField.type is OptionalType && normalField.type.innerType is ListType) {
            normalField.type.innerType
        } else {
            return emptyList()
        }

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

                val singleValueCodeBlock = BuilderSettingCodeBlock
                    .create(
                        "listOf(%T().applySuspend { argument() }.build())",
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
                    builderPattern(
                        name,
                        builderLambda(innerType),
                        kDoc,
                        singleValueCodeBlock,
                    ),
                )
            }

            else -> emptyList()
        }

        val justValuesPassedAsVarargArguments = listOf(
            FunSpec
                .builder(name)
                .addModifiers(SUSPEND)
                .addParameter("values", typeNameClashResolver.toTypeName(innerType, LanguageType.Kotlin), VARARG)
                .addCode(mappingCodeBlock(normalField, required = true, name, "values.toList()"))
                .addDocsToBuilderMethod(kDoc, "values")
                .build(),
        )

        return builderPattern + justValuesPassedAsVarargArguments
    }
}
