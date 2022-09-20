package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.virtuslab.pulumikotlin.codegen.expressions.Assignment
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import com.virtuslab.pulumikotlin.codegen.expressions.Expression
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.TypesGenerator.add

typealias MappingCode = (Expression) -> Expression

object KotlinPoetPatterns {
    fun listOfLambdas(innerType: TypeName): TypeName =
        LIST.parameterizedBy(builderLambda(innerType))

    fun listOfLambdas(innerType: ComplexType): TypeName =
        listOfLambdas(innerType.toBuilderTypeName())

    fun builderLambda(innerType: ComplexType): TypeName =
        builderLambda(innerType.toBuilderTypeName())

    fun builderLambda(innerType: TypeName): TypeName =
        LambdaTypeName
            .get(receiver = innerType, returnType = UNIT)
            .copy(suspending = true)

    data class BuilderSettingCodeBlock(val mappingCode: MappingCode? = null, val code: String, val args: List<Any?>) {
        fun withMappingCode(mappingCode: MappingCode): BuilderSettingCodeBlock {
            return copy(mappingCode = mappingCode)
        }

        fun toCodeBlock(fieldToSetName: String): CodeBlock {
            val mc = mappingCode
            return if (mc != null) {
                CodeBlock.builder()
                    .addStatement("val toBeMapped = $code", *args.toTypedArray())
                    .add(Assignment("mapped", mc(CustomExpression("toBeMapped"))))
                    .addStatement("")
                    .addStatement("this.$fieldToSetName = mapped")
                    .build()
            } else {
                CodeBlock.builder()
                    .addStatement("this.$fieldToSetName = $code", *args.toTypedArray())
                    .build()
            }
        }

        companion object {
            fun create(code: String, vararg args: Any?) = BuilderSettingCodeBlock(null, code, args.toList())
        }
    }
}
