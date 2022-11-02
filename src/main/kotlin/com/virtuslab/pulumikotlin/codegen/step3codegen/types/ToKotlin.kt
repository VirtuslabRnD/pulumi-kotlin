package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.virtuslab.pulumikotlin.codegen.expressions.ConstructObjectExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import com.virtuslab.pulumikotlin.codegen.expressions.Expression
import com.virtuslab.pulumikotlin.codegen.expressions.FunctionExpression
import com.virtuslab.pulumikotlin.codegen.expressions.Return
import com.virtuslab.pulumikotlin.codegen.expressions.addCode
import com.virtuslab.pulumikotlin.codegen.expressions.call0
import com.virtuslab.pulumikotlin.codegen.expressions.call1
import com.virtuslab.pulumikotlin.codegen.expressions.callMap
import com.virtuslab.pulumikotlin.codegen.expressions.callTransform
import com.virtuslab.pulumikotlin.codegen.expressions.field
import com.virtuslab.pulumikotlin.codegen.expressions.invoke
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AnyType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AssetOrArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.NameGeneration
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedEnumType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.KeywordsEscaper
import com.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

private const val TO_KOTLIN_FUNCTION_NAME = "toKotlin"

private const val JAVA_TYPE_PARAMETER_NAME = "javaType"

object ToKotlin {
    fun toKotlinFunction(
        typeMetadata: TypeMetadata,
        useAlternativeName: Boolean,
        kotlinNames: NameGeneration,
        fields: List<Field<*>>,
        typeNameClashResolver: TypeNameClashResolver,
    ): FunSpec {
        val arguments = fields.associate { field ->
            val type = field.fieldType.type

            val baseE = toKotlinExpressionBase(field.name)

            val secondPart =
                baseE.call1(
                    "applyValue",
                    FunctionExpression.create(1) { args ->
                        toKotlinExpression(
                            args.first(),
                            type,
                            typeNameClashResolver,
                        )
                    },
                )

            field.name to secondPart
        }

        val kotlinArgsClass = ClassName(kotlinNames.packageName, kotlinNames.className)

        val objectCreation = Return(ConstructObjectExpression(kotlinArgsClass, arguments))

        return FunSpec.builder(TO_KOTLIN_FUNCTION_NAME)
            .returns(kotlinArgsClass)
            .addParameter(prepareToJavaParameterSpec(typeMetadata, useAlternativeName))
            .addCode(objectCreation)
            .build()
    }

    fun toKotlinEnumFunction(typeMetadata: TypeMetadata, useAlternativeName: Boolean): FunSpec {
        val kotlinClass = typeMetadata.names(Kotlin).kotlinPoetClassName

        return FunSpec.builder(TO_KOTLIN_FUNCTION_NAME)
            .addParameter(prepareToJavaParameterSpec(typeMetadata, useAlternativeName))
            .returns(kotlinClass)
            .addStatement("return %T.valueOf(%L.name)", kotlinClass, JAVA_TYPE_PARAMETER_NAME)
            .build()
    }

    private fun toKotlinExpression(
        expression: Expression,
        type: ReferencedType,
        typeNameClashResolver: TypeNameClashResolver,
    ): Expression {
        return when (type) {
            AnyType -> expression
            is ReferencedComplexType -> typeNameClashResolver.kotlinNames(type.metadata)
                .kotlinPoetClassName
                .member(TO_KOTLIN_FUNCTION_NAME)(expression)

            is ReferencedEnumType -> typeNameClashResolver.kotlinNames(type.metadata)
                .kotlinPoetClassName
                .member(TO_KOTLIN_FUNCTION_NAME)(expression)

            is EitherType -> expression.callTransform(
                expressionMapperLeft = { args -> toKotlinExpression(args, type.firstType, typeNameClashResolver) },
                expressionMapperRight = { args -> toKotlinExpression(args, type.secondType, typeNameClashResolver) },
            )

            is ListType -> expression.callMap { args ->
                toKotlinExpression(
                    args,
                    type.innerType,
                    typeNameClashResolver,
                )
            }

            is MapType ->
                expression
                    .callMap { args ->
                        args.field("key")
                            .call1("to", toKotlinExpression(args.field("value"), type.valueType, typeNameClashResolver))
                    }
                    .call0("toMap")

            is PrimitiveType -> expression
            is AssetOrArchiveType, is ArchiveType -> expression
        }
    }

    private fun toKotlinExpressionBase(name: String): Expression {
        return CustomExpression(
            "%L.%N().%L()!!",
            JAVA_TYPE_PARAMETER_NAME,
            KeywordsEscaper.escape(name),
            TO_KOTLIN_FUNCTION_NAME,
        )
    }

    private fun prepareToJavaParameterSpec(typeMetadata: TypeMetadata, useAlternativeName: Boolean): ParameterSpec {
        val javaClass = typeMetadata.names(Java, useAlternativeName).kotlinPoetClassName
        return ParameterSpec.builder(JAVA_TYPE_PARAMETER_NAME, javaClass).build()
    }
}
