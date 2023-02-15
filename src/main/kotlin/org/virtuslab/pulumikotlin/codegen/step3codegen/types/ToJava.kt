package org.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.google.gson.JsonParser
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName.Companion.member
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import org.virtuslab.pulumikotlin.codegen.expressions.CustomExpressionBuilder
import org.virtuslab.pulumikotlin.codegen.expressions.Expression
import org.virtuslab.pulumikotlin.codegen.expressions.call0
import org.virtuslab.pulumikotlin.codegen.expressions.callApplyValue
import org.virtuslab.pulumikotlin.codegen.expressions.callLet
import org.virtuslab.pulumikotlin.codegen.expressions.callMap
import org.virtuslab.pulumikotlin.codegen.expressions.callTransform
import org.virtuslab.pulumikotlin.codegen.expressions.field
import org.virtuslab.pulumikotlin.codegen.expressions.pairWith
import org.virtuslab.pulumikotlin.codegen.step2intermediate.AnyType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ArchiveType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.AssetOrArchiveType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction
import org.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.JsonType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedRootType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject
import org.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import org.virtuslab.pulumikotlin.codegen.step3codegen.Field
import org.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

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
                                    )
                                }
                        } else {
                            CustomExpression("%N", it.toKotlinName())
                                .callApplyValue(it.fieldType.type is OptionalType) { args ->
                                    toJavaExpression(
                                        args,
                                        it.fieldType.type,
                                        typeNameClashResolver,
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
    ): Expression {
        return when (type) {
            is ReferencedRootType -> toJavaReferencedRootExpression(expression)
            is EitherType -> toJavaEitherExpression(expression, type, typeNameClashResolver)
            is ListType -> toJavaListExpression(expression, type, typeNameClashResolver)
            is MapType -> toJavaMapExpression(expression, type, typeNameClashResolver)
            is JsonType -> toJavaJsonExpression(expression)
            is OptionalType -> toJavaOptionalExpression(expression, type, typeNameClashResolver)
            is AnyType, is ArchiveType, is AssetOrArchiveType -> expression
            is PrimitiveType -> expression
        }
    }

    private fun toJavaReferencedRootExpression(expression: Expression) =
        expression.callLet { argument ->
            (CustomExpressionBuilder.start() + argument + CustomExpression(".%N()", TO_JAVA_FUNCTION_NAME)).build()
        }

    private fun toJavaEitherExpression(
        expression: Expression,
        type: EitherType,
        typeNameClashResolver: TypeNameClashResolver,
    ): Expression {
        val firstType = type.firstType
        val secondType = type.secondType
        return expression.callTransform(
            expressionMapperLeft = { args ->
                toJavaExpression(
                    args,
                    firstType,
                    typeNameClashResolver,
                )
            },
            expressionMapperRight = { args ->
                toJavaExpression(
                    args,
                    secondType,
                    typeNameClashResolver,
                )
            },
        )
    }

    private fun toJavaListExpression(
        expression: Expression,
        type: ListType,
        typeNameClashResolver: TypeNameClashResolver,
    ) = expression.callMap {
        toJavaExpression(
            it,
            type.innerType,
            typeNameClashResolver,
        )
    }

    private fun toJavaMapExpression(
        expression: Expression,
        type: MapType,
        typeNameClashResolver: TypeNameClashResolver,
    ) = expression.callMap { argument ->
        argument.field("key")
            .pairWith(
                toJavaExpression(
                    argument.field("value"),
                    type.valueType,
                    typeNameClashResolver,
                ),
            )
    }
        .call0("toMap")

    private fun toJavaJsonExpression(expression: Expression) = (
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

    private fun toJavaOptionalExpression(
        expression: Expression,
        type: OptionalType,
        typeNameClashResolver: TypeNameClashResolver,
    ) = toJavaExpression(
        expression,
        type.innerType,
        typeNameClashResolver,
    )

    fun enumFunction(javaClass: ClassName) =
        FunSpec.builder(TO_JAVA_FUNCTION_NAME)
            .addModifiers(KModifier.OVERRIDE)
            .returns(javaClass)
            .addStatement("return $ENUM_JAVA_PROPERTY_NAME")
            .build()
}
