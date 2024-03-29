package org.virtuslab.pulumikotlin.codegen.step3codegen

import com.google.gson.GsonBuilder
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import kotlinx.serialization.json.Json
import org.virtuslab.pulumikotlin.codegen.expressions.ConstructObjectExpression
import org.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import org.virtuslab.pulumikotlin.codegen.expressions.CustomExpressionBuilder
import org.virtuslab.pulumikotlin.codegen.expressions.Expression
import org.virtuslab.pulumikotlin.codegen.expressions.Return
import org.virtuslab.pulumikotlin.codegen.expressions.addCode
import org.virtuslab.pulumikotlin.codegen.expressions.call0
import org.virtuslab.pulumikotlin.codegen.expressions.call1
import org.virtuslab.pulumikotlin.codegen.expressions.callApplyValue
import org.virtuslab.pulumikotlin.codegen.expressions.callLet
import org.virtuslab.pulumikotlin.codegen.expressions.callMap
import org.virtuslab.pulumikotlin.codegen.expressions.callTransform
import org.virtuslab.pulumikotlin.codegen.expressions.field
import org.virtuslab.pulumikotlin.codegen.expressions.invoke
import org.virtuslab.pulumikotlin.codegen.expressions.ofLeft
import org.virtuslab.pulumikotlin.codegen.expressions.ofRight
import org.virtuslab.pulumikotlin.codegen.expressions.pairWith
import org.virtuslab.pulumikotlin.codegen.step2intermediate.AnyType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ArchiveType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.AssetOrArchiveType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import org.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.JsonType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedEnumType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedRootType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.StringType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import org.virtuslab.pulumikotlin.codegen.utils.letIf

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
            .letIf(fields.isEmpty()) {
                it.addAnnotation(
                    AnnotationSpec.builder(Suppress::class)
                        .addMember("\"UNUSED_PARAMETER\"")
                        .build(),
                )
            }
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
            is ReferencedRootType -> toKotlinReferencedRootExpression(expression, type, typeNameClashResolver)
            is EitherType -> toKotlinEitherExpression(direction, isType, expression, type, typeNameClashResolver)
            is ListType -> toKotlinListExpression(direction, isType, expression, type, typeNameClashResolver)
            is MapType -> toKotlinMapExpression(direction, isType, expression, type, typeNameClashResolver)
            is JsonType -> toKotlinJsonExpression(expression)
            is OptionalType -> toKotlinOptionalExpression(direction, isType, expression, type, typeNameClashResolver)
            is AnyType, is AssetOrArchiveType, is ArchiveType -> expression
            is PrimitiveType -> expression
        }
    }

    private fun toKotlinReferencedRootExpression(
        expression: Expression,
        type: ReferencedRootType,
        typeNameClashResolver: TypeNameClashResolver,
    ) = expression.callLet { argument ->
        typeNameClashResolver.toTypeName(type, languageType = Kotlin)
            .nestedClass("Companion")
            .member(TO_KOTLIN_FUNCTION_NAME)(argument)
    }

    private fun toKotlinEitherExpression(
        direction: Direction,
        isType: Boolean,
        expression: Expression,
        type: EitherType,
        typeNameClashResolver: TypeNameClashResolver,
    ): Expression {
        val firstType = type.firstType
        val secondType = type.secondType
        return if (direction == Output && firstType is ReferencedEnumType && secondType is StringType) {
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

    private fun toKotlinListExpression(
        direction: Direction,
        isType: Boolean,
        expression: Expression,
        type: ListType,
        typeNameClashResolver: TypeNameClashResolver,
    ) = expression.callMap { args ->
        toKotlinExpression(
            direction,
            isType,
            args,
            type.innerType,
            typeNameClashResolver,
        )
    }

    private fun toKotlinMapExpression(
        direction: Direction,
        isType: Boolean,
        expression: Expression,
        type: MapType,
        typeNameClashResolver: TypeNameClashResolver,
    ) = expression.callMap { argument ->
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

    private fun toKotlinJsonExpression(expression: Expression) = (
        CustomExpressionBuilder.start(
            "%T.parseToJsonElement(%T().serializeNulls().create().toJson(",
            Json::class,
            GsonBuilder::class,
        )
            .plus(expression)
            .plus("))")
        )
        .build()

    private fun toKotlinOptionalExpression(
        direction: Direction,
        isType: Boolean,
        expression: Expression,
        type: OptionalType,
        typeNameClashResolver: TypeNameClashResolver,
    ) = if (isType && (type.innerType is ListType || type.innerType is MapType)) {
        toKotlinExpression(
            direction,
            isType,
            expression,
            type.innerType,
            typeNameClashResolver,
        )
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
