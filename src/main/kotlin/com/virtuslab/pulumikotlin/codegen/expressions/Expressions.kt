package com.virtuslab.pulumikotlin.codegen.expressions

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName


sealed class Code

sealed class Expression {
    abstract fun toCodeBlock(): CustomCodeBlock
}

data class FunctionExpression(val argumentNames: List<String>, val expression: Expression) : Expression() {
    companion object {
        fun create(arguments: Int, mapper: (List<String>) -> Expression): FunctionExpression {
            val args = (0 until arguments)
                .map { "args${it}" }

            val expression = CustomExpressionBuilder.start("{") + args.joinToString(", ") + " -> " + mapper(args) + "}"

            return FunctionExpression(args, expression.build())
        }
    }

    override fun toCodeBlock(): CustomCodeBlock {
        return expression.toCodeBlock()
    }
}

data class ConstructObjectExpression(val typeName: TypeName, val fields: Map<String, Expression>) : Expression() {
    override fun toCodeBlock(): CustomCodeBlock {
        val fieldsBuilder = fields
            .map { (name, value) ->
                CustomExpressionBuilder.start(name) + "=" + value
            }
            .reduce { left, right -> left + "," + right }

        val builder = CustomExpressionBuilder.start("%T", typeName) + "(" + fieldsBuilder + ")"
        return builder.build().toCodeBlock()
    }
}

data class CustomCodeBlock(val text: String, val args: List<Any>) {
    fun toKotlinPoetCodeBlock(): CodeBlock {
        return CodeBlock.of(text, *args.toTypedArray())

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

data class Assignment(val to: String, val expression: Expression)

operator fun MemberName.invoke(vararg expression: Expression): Expression {
    val builder = CustomExpressionBuilder.start("%M", this) + "(" + expression.map { it.toCodeBlock() }.merge(",") + ")"
    return builder.build()
}

