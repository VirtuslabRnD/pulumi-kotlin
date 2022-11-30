package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.google.gson.JsonParser
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpressionBuilder
import com.virtuslab.pulumikotlin.codegen.expressions.Expression
import com.virtuslab.pulumikotlin.codegen.expressions.call0
import com.virtuslab.pulumikotlin.codegen.expressions.callApplyValue
import com.virtuslab.pulumikotlin.codegen.expressions.callLet
import com.virtuslab.pulumikotlin.codegen.expressions.callMap
import com.virtuslab.pulumikotlin.codegen.expressions.callTransform
import com.virtuslab.pulumikotlin.codegen.expressions.field
import com.virtuslab.pulumikotlin.codegen.expressions.pairWith
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AnyType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AssetOrArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.JsonType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedRootType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private const val TO_JAVA_FUNCTION_NAME = "toJava"

private const val ENUM_JAVA_PROPERTY_NAME = "javaValue"

object ToJava {

    fun typeFunction(
        fields: List<Field<*>>,
        typeNameClashResolver: TypeNameClashResolver,
        typeMetadata: TypeMetadata,
    ): FunSpec {
        val usageKind = typeMetadata.usageKind
        val javaClass = typeNameClashResolver.javaNames(typeMetadata).kotlinPoetClassName

        return FunSpec.builder(TO_JAVA_FUNCTION_NAME)
            .returns(javaClass)
            .addModifiers(KModifier.OVERRIDE)
            .addCode(CodeBlock.of("return %T.%M()", javaClass, javaClass.member("builder")))
            .apply {
                fields.map {
                    val expression =
                        if (usageKind.direction == Direction.Input && usageKind.subject == Subject.Function) {
                            CustomExpression("%N", it.toKotlinName())
                                .callLet(it.fieldType.type is OptionalType) { args ->
                                    toJavaExpression(
                                        args,
                                        it.fieldType.type,
                                        typeNameClashResolver,
                                        usageKind.depth,
                                    )
                                }
                        } else {
                            CustomExpression("%N", it.toKotlinName())
                                .callApplyValue(it.fieldType.type is OptionalType) { args ->
                                    toJavaExpression(
                                        args,
                                        it.fieldType.type,
                                        typeNameClashResolver,
                                        usageKind.depth,
                                    )
                                }
                        }
                    val codeBlock = CodeBlock.builder()
                        .add("\n.%N(", it.toJavaName())
                        .add(expression.toCodeBlock().toKotlinPoetCodeBlock())
                        .add(")")
                        .build()
                    addCode(codeBlock)
                }
            }
            .addCode(CodeBlock.of(".build()"))
            .build()
    }

    private fun toJavaExpression(
        expression: Expression,
        type: ReferencedType,
        typeNameClashResolver: TypeNameClashResolver,
        depth: Depth,
    ): Expression {
        return when (type) {
            is AnyType, ArchiveType, AssetOrArchiveType -> expression
            is EitherType -> {
                val firstType = type.firstType
                val secondType = type.secondType
                expression.callTransform(
                    expressionMapperLeft = { args ->
                        toJavaExpression(
                            args,
                            firstType,
                            typeNameClashResolver,
                            depth,
                        )
                    },
                    expressionMapperRight = { args ->
                        toJavaExpression(
                            args,
                            secondType,
                            typeNameClashResolver,
                            depth,
                        )
                    },
                )
            }

            is JsonType -> (
                CustomExpressionBuilder.start(
                    "%T.parseString(%T.encodeToString(%T.serializer(),",
                    JsonParser::class,
                    Json::class,
                    JsonElement::class,
                ) +
                    expression +
                    "))"
                )
                .build()

            is ListType -> expression.callMap {
                toJavaExpression(
                    it,
                    type.innerType,
                    typeNameClashResolver,
                    depth,
                )
            }

            is MapType -> expression.callMap { argument ->
                argument.field("key")
                    .pairWith(
                        toJavaExpression(
                            argument.field("value"),
                            type.valueType,
                            typeNameClashResolver,
                            depth,
                        ),
                    )
            }
                .call0("toMap")

            is OptionalType -> toJavaExpression(
                expression,
                type.innerType,
                typeNameClashResolver,
                depth,
            )

            is PrimitiveType -> expression
            is ReferencedRootType -> expression.callLet { argument ->
                (CustomExpressionBuilder.start() + argument + CustomExpression(".%N()", TO_JAVA_FUNCTION_NAME)).build()
            }
        }
    }

    fun enumFunction(javaClass: ClassName) =
        FunSpec.builder(TO_JAVA_FUNCTION_NAME)
            .addModifiers(KModifier.OVERRIDE)
            .returns(javaClass)
            .addStatement("return $ENUM_JAVA_PROPERTY_NAME")
            .build()
}
