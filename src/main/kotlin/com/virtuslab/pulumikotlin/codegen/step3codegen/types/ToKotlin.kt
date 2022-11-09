package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.virtuslab.pulumikotlin.codegen.expressions.ConstructObjectExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpressionBuilder
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
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.JsonType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.NameGeneration
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedEnumType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.StringType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

private const val TO_KOTLIN_FUNCTION_NAME = "toKotlin"

private const val JAVA_TYPE_PARAMETER_NAME = "javaType"

object ToKotlin {
    fun toKotlinFunction(
        typeMetadata: TypeMetadata,
        kotlinNames: NameGeneration,
        fields: List<Field<*>>,
        typeNameClashResolver: TypeNameClashResolver,
    ): FunSpec {
        val arguments = fields.associate { field ->
            val type = field.fieldType.type

            val baseE = toKotlinExpressionBase(field.toJavaName(escape = true), required = field.required)

            val secondPart =
                baseE.call1(
                    "applyValue",
                    FunctionExpression.create(1) { args ->
                        toKotlinExpression(
                            typeMetadata,
                            args.first(),
                            type,
                            typeNameClashResolver,
                            required = field.required,
                        )
                    },
                    optional = !field.required,
                )

            field.toKotlinName() to secondPart
        }

        val kotlinArgsClass = ClassName(kotlinNames.packageName, kotlinNames.className)

        val objectCreation = Return(ConstructObjectExpression(kotlinArgsClass, arguments))

        return FunSpec.builder(TO_KOTLIN_FUNCTION_NAME)
            .returns(kotlinArgsClass)
            .addParameter(prepareToJavaParameterSpec(typeMetadata, typeNameClashResolver))
            .addCode(objectCreation)
            .build()
    }

    fun toKotlinEnumFunction(typeMetadata: TypeMetadata, typeNameClashResolver: TypeNameClashResolver): FunSpec {
        val kotlinClass = typeMetadata.names(Kotlin).kotlinPoetClassName

        return FunSpec.builder(TO_KOTLIN_FUNCTION_NAME)
            .addParameter(prepareToJavaParameterSpec(typeMetadata, typeNameClashResolver))
            .returns(kotlinClass)
            .addStatement("return %T.valueOf(%L.name)", kotlinClass, JAVA_TYPE_PARAMETER_NAME)
            .build()
    }

    private fun toKotlinExpression(
        typeMetadata: TypeMetadata,
        expression: Expression,
        type: ReferencedType,
        typeNameClashResolver: TypeNameClashResolver,
        required: Boolean,
    ): Expression {
        return when (type) {
            AnyType -> expression
            is ReferencedComplexType -> typeNameClashResolver.kotlinNames(type.metadata)
                .kotlinPoetClassName
                .member(TO_KOTLIN_FUNCTION_NAME)(expression)

            is ReferencedEnumType -> typeNameClashResolver.kotlinNames(type.metadata)
                .kotlinPoetClassName
                .member(TO_KOTLIN_FUNCTION_NAME)(expression)

            is EitherType -> {
                val direction = typeMetadata.usageKind.direction
                val firstType = type.firstType
                val secondType = type.secondType
                if (direction == Output && firstType is ReferencedEnumType && secondType is StringType) {
                    (CustomExpressionBuilder.start() + "Either.ofRight(" + expression + ")").build()
                } else if (direction == Output && firstType is StringType && secondType is ReferencedEnumType) {
                    (CustomExpressionBuilder.start() + "Either.ofLeft(" + expression + ")").build()
                } else {
                    expression.callTransform(
                        optional = !required,
                        expressionMapperLeft = { args ->
                            toKotlinExpression(
                                typeMetadata,
                                args,
                                firstType,
                                typeNameClashResolver,
                                required = true,
                            )
                        },
                        expressionMapperRight = { args ->
                            toKotlinExpression(
                                typeMetadata,
                                args,
                                secondType,
                                typeNameClashResolver,
                                required = true,
                            )
                        },
                    )
                }
            }

            is ListType -> expression.callMap { args ->
                toKotlinExpression(
                    typeMetadata,
                    args,
                    type.innerType,
                    typeNameClashResolver,
                    required = true,
                )
            }

            is MapType ->
                expression
                    .callMap { args ->
                        args.field("key")
                            .call1(
                                "to",
                                toKotlinExpression(
                                    typeMetadata,
                                    args.field("value"),
                                    type.valueType,
                                    typeNameClashResolver,
                                    required = true,
                                ),
                                optional = false,
                            )
                    }
                    .call0("toMap")

            is PrimitiveType -> expression
            is AssetOrArchiveType, is ArchiveType, is JsonType -> expression
        }
    }

    private fun toKotlinExpressionBase(name: String, required: Boolean): Expression {
        return CustomExpression(
            "%L.%N().%L()" + (if (required) "!!" else ""),
            JAVA_TYPE_PARAMETER_NAME,
            name,
            TO_KOTLIN_FUNCTION_NAME,
        )
    }

    private fun prepareToJavaParameterSpec(
        typeMetadata: TypeMetadata,
        typeNameClashResolver: TypeNameClashResolver,
    ): ParameterSpec {
        val javaClass = typeNameClashResolver.javaNames(typeMetadata).kotlinPoetClassName
        return ParameterSpec.builder(JAVA_TYPE_PARAMETER_NAME, javaClass).build()
    }
}
