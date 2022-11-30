package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.google.gson.GsonBuilder
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.virtuslab.pulumikotlin.codegen.expressions.ConstructObjectExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpressionBuilder
import com.virtuslab.pulumikotlin.codegen.expressions.Expression
import com.virtuslab.pulumikotlin.codegen.expressions.Return
import com.virtuslab.pulumikotlin.codegen.expressions.addCode
import com.virtuslab.pulumikotlin.codegen.expressions.call0
import com.virtuslab.pulumikotlin.codegen.expressions.call1
import com.virtuslab.pulumikotlin.codegen.expressions.callApplyValue
import com.virtuslab.pulumikotlin.codegen.expressions.callLet
import com.virtuslab.pulumikotlin.codegen.expressions.callMap
import com.virtuslab.pulumikotlin.codegen.expressions.callTransform
import com.virtuslab.pulumikotlin.codegen.expressions.field
import com.virtuslab.pulumikotlin.codegen.expressions.invoke
import com.virtuslab.pulumikotlin.codegen.expressions.ofLeft
import com.virtuslab.pulumikotlin.codegen.expressions.ofRight
import com.virtuslab.pulumikotlin.codegen.expressions.pairWith
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AnyType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AssetOrArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.JsonType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedEnumType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedRootType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.StringType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import kotlinx.serialization.json.Json

private const val TO_KOTLIN_FUNCTION_NAME = "toKotlin"

private const val JAVA_TYPE_PARAMETER_NAME = "javaType"
private const val JAVA_RESOURCE_PROPERTY_NAME = "javaResource"

object ToKotlin {

    fun resourceFunction(
        field: Field<*>,
        typeNameClashResolver: TypeNameClashResolver,
    ): FunSpec {
        return FunSpec.getterBuilder()
            .addCode(
                Return(
                    CustomExpression("%N.%N()", JAVA_RESOURCE_PROPERTY_NAME, field.toJavaName())
                        .callApplyValue { arg ->
                            toKotlinExpression(
                                Output,
                                false,
                                arg,
                                field.fieldType.type,
                                typeNameClashResolver,
                            )
                        },
                ),
            )
            .build()
    }

    fun typeFunction(
        typeMetadata: TypeMetadata,
        fields: List<Field<*>>,
        typeNameClashResolver: TypeNameClashResolver,
    ): FunSpec {
        val arguments = fields.associate { field ->
            val expression = if (typeMetadata.usageKind.direction == Input) {
                CustomExpression("%N.%N()", JAVA_TYPE_PARAMETER_NAME, field.toJavaName())
                    .callApplyValue { args ->
                        toKotlinExpression(
                            typeMetadata.usageKind.direction,
                            true,
                            args,
                            field.fieldType.type,
                            typeNameClashResolver,
                        )
                    }
            } else {
                toKotlinExpression(
                    typeMetadata.usageKind.direction,
                    true,
                    CustomExpression("%N.%N()", JAVA_TYPE_PARAMETER_NAME, field.toJavaName()),
                    field.fieldType.type,
                    typeNameClashResolver,
                )
            }

            field.toKotlinName() to expression
        }

        val kotlinClass = typeNameClashResolver.kotlinNames(typeMetadata).kotlinPoetClassName
        val javaClass = typeNameClashResolver.javaNames(typeMetadata).kotlinPoetClassName

        val objectCreation = Return(ConstructObjectExpression(kotlinClass, arguments))

        return FunSpec.builder(TO_KOTLIN_FUNCTION_NAME)
            .returns(kotlinClass)
            .addParameter(ParameterSpec.builder(JAVA_TYPE_PARAMETER_NAME, javaClass).build())
            .addCode(objectCreation)
            .build()
    }

    fun enumFunction(javaClass: ClassName, kotlinClass: ClassName): FunSpec {
        return FunSpec.builder(TO_KOTLIN_FUNCTION_NAME)
            .addParameter(ParameterSpec.builder(JAVA_TYPE_PARAMETER_NAME, javaClass).build())
            .returns(kotlinClass)
            .addStatement("return %T.values().first·{·it.javaValue·==·%L·}", kotlinClass, JAVA_TYPE_PARAMETER_NAME)
            .build()
    }

    private fun toKotlinExpression(
        direction: Direction,
        isType: Boolean,
        expression: Expression,
        type: ReferencedType,
        typeNameClashResolver: TypeNameClashResolver,
    ): Expression {
        return when (type) {
            is AnyType -> expression
            is ReferencedRootType -> expression.callLet { argument ->
                typeNameClashResolver.toTypeName(type, languageType = Kotlin)
                    .nestedClass("Companion")
                    .member(TO_KOTLIN_FUNCTION_NAME)(argument)
            }

            is EitherType -> {
                val firstType = type.firstType
                val secondType = type.secondType
                if (direction == Output && firstType is ReferencedEnumType && secondType is StringType) {
                    expression.ofRight(
                        typeNameClashResolver.toTypeName(firstType, Kotlin),
                        typeNameClashResolver.toTypeName(secondType, Kotlin),
                    )
                } else if (direction == Output && firstType is StringType && secondType is ReferencedEnumType) {
                    expression.ofLeft(
                        typeNameClashResolver.toTypeName(firstType, Kotlin),
                        typeNameClashResolver.toTypeName(secondType, Kotlin),
                    )
                } else {
                    expression.callTransform(
                        expressionMapperLeft = { args ->
                            toKotlinExpression(
                                direction,
                                isType,
                                args,
                                firstType,
                                typeNameClashResolver,
                            )
                        },
                        expressionMapperRight = { args ->
                            toKotlinExpression(
                                direction,
                                isType,
                                args,
                                secondType,
                                typeNameClashResolver,
                            )
                        },
                    )
                }
            }

            is ListType -> expression.callMap { args ->
                toKotlinExpression(
                    direction,
                    isType,
                    args,
                    type.innerType,
                    typeNameClashResolver,
                )
            }

            is MapType -> expression.callMap { argument ->
                argument.field("key")
                    .pairWith(
                        toKotlinExpression(
                            direction,
                            isType,
                            argument.field("value"),
                            type.valueType,
                            typeNameClashResolver,
                        ),
                    )
            }
                .call0("toMap")

            is PrimitiveType -> expression
            is AssetOrArchiveType, is ArchiveType -> expression
            is JsonType -> (
                CustomExpressionBuilder.start(
                    "%T.parseToJsonElement(%T().serializeNulls().create().toJson(",
                    Json::class,
                    GsonBuilder::class,
                ) +
                    expression +
                    "))"
                )
                .build()

            is OptionalType -> {
                if (isType && (type.innerType is ListType || type.innerType is MapType)) {
                    toKotlinExpression(
                        direction,
                        isType,
                        expression,
                        type.innerType,
                        typeNameClashResolver,
                    )
                } else if (type.innerType is PrimitiveType) {
                    toKotlinExpression(
                        direction,
                        isType,
                        expression,
                        type.innerType,
                        typeNameClashResolver,
                    )
                        .call1("orElse", CustomExpression("null"))
                } else {
                    expression.callMap { args ->
                        toKotlinExpression(
                            direction,
                            isType,
                            args,
                            type.innerType,
                            typeNameClashResolver,
                        )
                    }
                        .call1("orElse", CustomExpression("null"))
                }
            }
        }
    }
}
