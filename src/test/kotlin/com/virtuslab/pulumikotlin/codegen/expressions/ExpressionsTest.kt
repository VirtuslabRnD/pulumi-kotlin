package com.virtuslab.pulumikotlin.codegen.expressions

import com.squareup.kotlinpoet.FunSpec
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes
import org.junit.jupiter.api.Test

class ExpressionsTest {

    @Test
    fun justHavingFun() {
        val code = FunctionExpression.create(2) { args ->
            ConstructObjectExpression(
                MoreTypes.Java.Pulumi.Output(),
                mapOf(
                    "whatever" to args.get(0),
                    "whatever2" to args.get(1).pairWith(args.get(0)).callLet(true, { expr -> expr.pairWith(CustomExpression("%S", "asd")) })
                )
            )
        }.toCodeBlock().toKotlinPoetCodeBlock()

        println(FunSpec.builder("whateer").addCode(code).build().toString())
    }
}