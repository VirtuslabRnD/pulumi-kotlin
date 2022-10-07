package com.virtuslab.pulumikotlin.codegen.step3codegen.resources

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.virtuslab.pulumikotlin.codegen.expressions.Code
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import com.virtuslab.pulumikotlin.codegen.expressions.Expression
import com.virtuslab.pulumikotlin.codegen.expressions.Return
import com.virtuslab.pulumikotlin.codegen.expressions.call0
import com.virtuslab.pulumikotlin.codegen.expressions.callApplyValue
import com.virtuslab.pulumikotlin.codegen.expressions.callLet
import com.virtuslab.pulumikotlin.codegen.expressions.callMap
import com.virtuslab.pulumikotlin.codegen.expressions.field
import com.virtuslab.pulumikotlin.codegen.expressions.invoke
import com.virtuslab.pulumikotlin.codegen.expressions.pairWith
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AnyType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedRootType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step3codegen.KeywordsEscaper

object ToKotlin {
    fun toKotlinFunctionResource(name: String, type: ReferencedType, optional: Boolean): Code {
        val baseE = toKotlinExpressionBaseResource(name)
        val secondPart =
            baseE.callApplyValue { arg -> toKotlinExpressionResource(arg.call0("toKotlin", optional), type, optional) }

        return Return(secondPart)
    }

    private fun toKotlinExpressionResource(
        expression: Expression,
        type: ReferencedType,
        optional: Boolean = false,
    ): Expression {
        return when (type) {
            AnyType -> expression
            is ReferencedRootType ->
                expression.callLet(optional) { argument ->
                    type.toTypeName().toKotlinMethod()(argument)
                }

            is EitherType -> expression
            is ListType -> expression.callMap(optional) { argument ->
                toKotlinExpressionResource(
                    argument,
                    type.innerType,
                )
            }

            is MapType ->
                expression
                    .callMap(optional) { argument ->
                        argument.field("key")
                            .pairWith(toKotlinExpressionResource(argument.field("value"), type.secondType))
                    }
                    .call0("toMap", optional)

            is PrimitiveType -> expression
        }
    }

    private fun ClassName.toKotlinMethod() = nestedClass("Companion").member("toKotlin")

    private fun toKotlinExpressionBaseResource(name: String): Expression {
        return CustomExpression("javaResource.%N()", KeywordsEscaper.escape(name))
    }
}
