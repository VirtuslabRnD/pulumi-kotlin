package com.virtuslab.pulumikotlin.codegen.expressions

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName


sealed class Code {
    abstract fun toCodeBlock(): CustomCodeBlock

}

data class Return(val expression: Expression): Code() {
    override fun toCodeBlock(): CustomCodeBlock {
        return CustomCodeBlock("return " + expression.toCodeBlock().text, expression.toCodeBlock().args)
    }
}

sealed class Expression: Code() {
}

fun Expression.pairWith(expression: Expression) = this.call1("to", expression)
fun Expression.field(name: String): Expression = (CustomExpressionBuilder.start() + this + "." + name).build()

fun Expression.call0(name: String, optional: Boolean = false): Expression {
    val optionalString = if(optional) { "?" } else { "" }

    return (CustomExpressionBuilder.start() + this + "${optionalString}." + name + "()").build()
}

fun Expression.callMap(optional: Boolean = false, expressionMapper: (Expression) -> Expression): Expression {
    return call1("map", FunctionExpression.create(1, { expr -> expressionMapper(expr.get(0)) }), optional = optional)
}

fun Expression.callLet(optional: Boolean = false, expressionMapper: (Expression) -> Expression): Expression {
    return call1("let", FunctionExpression.create(1, { expr -> expressionMapper(expr.get(0)) }), optional = optional)
}

fun Expression.callApplyValue(optional: Boolean = false, expressionMapper: (Expression) -> Expression): Expression {
    return call1("applyValue", FunctionExpression.create(1, { expr -> expressionMapper(expr.get(0)) }), optional = optional)
}

fun Expression.call1(name: String, expression: Expression, optional: Boolean = false): Expression {
    val optionalString = if(optional) { "?" } else { "" }
    return (CustomExpressionBuilder.start() + this + "${optionalString}." + name + "(" + expression + ")").build()
}

data class NullsafeApply(val expression: Expression, val mapper: (Expression) -> Expression): Expression() {
    override fun toCodeBlock(): CustomCodeBlock {
        return (CustomExpressionBuilder.start() + expression + ".let {" + mapper(CustomExpression("it")) + "}").build().toCodeBlock()
    }


}

data class FunctionExpression(val argumentNames: List<String>, val expression: Expression) : Expression() {
    companion object {
        fun create(arguments: Int, mapper: (List<Expression>) -> Expression): FunctionExpression {
            val argNames = (0 until arguments)
                .map { "args${it}" }

            val argExprs = argNames.map { CustomExpression(it) }

            val expression = CustomExpressionBuilder.start("{") + argNames.joinToString(", ") + " -> " + mapper(argExprs) + "}"

            return FunctionExpression(argNames, expression.build())
        }
    }

    override fun toCodeBlock(): CustomCodeBlock {
        return expression.toCodeBlock()
    }
}

fun FunSpec.Builder.addCode(code: Code): FunSpec.Builder {
    return this.addCode(code.toCodeBlock().toKotlinPoetCodeBlock())
}

data class ConstructObjectExpression(val typeName: TypeName, val fields: Map<String, Expression>) : Expression() {
    override fun toCodeBlock(): CustomCodeBlock {
        val fieldsBuilder = fields
            .map { (name, value) ->
                CustomExpressionBuilder.start(name) + "=" + value
            }
            .reduceOrNull { left, right -> left + "," + right }
            ?: CustomExpressionBuilder.start()

        val builder = CustomExpressionBuilder.start("%T", typeName) + "(" + fieldsBuilder + ")"
        return builder.build().toCodeBlock()
    }
}

data class CustomCodeBlock(val text: String, val args: List<Any>) {
    fun toKotlinPoetCodeBlock(): CodeBlock {
        try {
            return CodeBlock.of(text, *args.toTypedArray())
        } catch(e: Exception) {
            println("Exception debug info: text: ${text}")
            println("Exception debug info: args: ${args}")
            throw e
        }
    }

}

data class CustomExpressionBuilder(val text: String, val args: List<Any>) {

    operator fun plus(string: String): CustomExpressionBuilder {
        return copy(text = this.text + string)
    }

    operator fun plus(expression: Expression): CustomExpressionBuilder {
        return copy(text = this.text + expression.toCodeBlock().text, args = this.args + expression.toCodeBlock().args)
    }

    operator fun plus(code: CustomCodeBlock): CustomExpressionBuilder {
        return copy(text = this.text + code.text, args = this.args + code.args)
    }

    operator fun plus(code: CustomExpressionBuilder): CustomExpressionBuilder {
        return copy(text = this.text + code.text, args = this.args + code.args)
    }

    fun build(): CustomExpression {
        return CustomExpression(this.text, this.args)
    }

    companion object {
        fun start(): CustomExpressionBuilder {
            return CustomExpressionBuilder("", emptyList())
        }

        fun start(text: String, vararg args: Any): CustomExpressionBuilder {
            return CustomExpressionBuilder(text, args.toList())
        }
    }
}

data class CustomExpression(val text: String, val args: List<Any>) : Expression() {
    constructor(text: String, vararg args: Any) : this(text, args.toList())

    operator fun invoke(text: String, vararg args: Any): CustomExpression {
        return copy(text = this.text + text, args = this.args + args)
    }

    operator fun invoke(expression: CustomExpression): CustomExpression {
        return copy(text = this.text + expression.text, args = this.args + expression.args)
    }

    override fun toCodeBlock(): CustomCodeBlock {
        return try {
            CustomCodeBlock(text, args)
        } catch (e: Exception) {
            println("Text" + text)
            println("Args" + args)
            throw e
        }
    }
}

fun Iterable<CustomCodeBlock>.merge(separator: String): CustomCodeBlock {
    return CustomCodeBlock(
        map { it.text }.joinToString(separator),
        flatMap { it.args }
    )
}

data class Assignment(val to: String, val expression: Expression): Code() {
    override fun toCodeBlock(): CustomCodeBlock {
        return CustomCodeBlock("val %N = " + expression.toCodeBlock().text, listOf(to) + expression.toCodeBlock().args)
    }
}

operator fun MemberName.invoke(vararg expression: Expression): Expression {
    val builder = CustomExpressionBuilder.start("%M", this) + "(" + expression.map { it.toCodeBlock() }.merge(",") + ")"
    return builder.build()
}

