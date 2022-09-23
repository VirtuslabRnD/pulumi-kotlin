package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.virtuslab.pulumikotlin.codegen.expressions.ConstructObjectExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import com.virtuslab.pulumikotlin.codegen.expressions.Expression
import com.virtuslab.pulumikotlin.codegen.expressions.FunctionExpression
import com.virtuslab.pulumikotlin.codegen.expressions.Return
import com.virtuslab.pulumikotlin.codegen.expressions.addCode
import com.virtuslab.pulumikotlin.codegen.expressions.call0
import com.virtuslab.pulumikotlin.codegen.expressions.call1
import com.virtuslab.pulumikotlin.codegen.expressions.callMap
import com.virtuslab.pulumikotlin.codegen.expressions.field
import com.virtuslab.pulumikotlin.codegen.expressions.invoke
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AnyType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EnumType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Type
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.KeywordsEscaper

object ToKotlin {
    fun toKotlinFunction(typeMetadata: TypeMetadata, fields: List<Field<*>>): FunSpec {
        val arguments = fields.associate { field ->
            val type = field.fieldType.type

            val baseE = toKotlinExpressionBase(field.name)

            val secondPart =
                baseE.call1(
                    "applyValue",
                    FunctionExpression.create(1, { args -> toKotlinExpression(args.get(0), type) }),
                )

            field.name to secondPart
        }

        val names = typeMetadata.names(LanguageType.Kotlin)
        val kotlinArgsClass = ClassName(names.packageName, names.className)

        val javaNames = typeMetadata.names(LanguageType.Java)
        val javaArgsClass = ClassName(javaNames.packageName, javaNames.className)

        val objectCreation = Return(ConstructObjectExpression(kotlinArgsClass, arguments))

        return FunSpec.builder("toKotlin")
            .returns(kotlinArgsClass)
            .addParameter("javaType", javaArgsClass)
            .addCode(objectCreation)
            .build()
    }

    private fun toKotlinExpression(expression: Expression, type: Type): Expression {
        return when (val type = type) {
            AnyType -> expression
            is ComplexType -> type.toTypeName().member("toKotlin")(expression)
            is EnumType -> type.toTypeName().member("toKotlin")(expression)
            is EitherType -> expression
            is ListType -> expression.callMap { args -> toKotlinExpression(args, type.innerType) }

            is MapType ->
                expression
                    .callMap { args ->
                        args.field("key").call1("to", toKotlinExpression(args.field("value"), type.secondType))
                    }
                    .call0("toMap")

            is PrimitiveType -> expression
        }
    }

    private fun toKotlinExpressionBase(name: String): Expression {
        return CustomExpression("javaType.%N().toKotlin()!!", KeywordsEscaper.escape(name))
    }
}
