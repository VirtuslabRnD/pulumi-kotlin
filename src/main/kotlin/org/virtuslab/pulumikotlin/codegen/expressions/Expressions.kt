package org.virtuslab.pulumikotlin.codegen.expressions

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

sealed class Code {
    abstract fun toCodeBlock(): CustomCodeBlock
}

data class Return(val expression: Expression) : Code() {
    override fun toCodeBlock(): CustomCodeBlock {
        return CustomCodeBlock("return·" + expression.toCodeBlock().text, expression.toCodeBlock().args)
    }
}

sealed class Expression : Code()

fun Expression.pairWith(expression: Expression) = this.call1("to", expression)
fun Expression.field(name: String): Expression = (CustomExpressionBuilder.start() + this + "." + name).build()

fun Expression.call0(name: String, optional: Boolean = false): Expression {
    val optionalString = if (optional) "?" else ""

    return (CustomExpressionBuilder.start() + this + "$optionalString." + name + "()").build()
}

fun Expression.callMap(optional: Boolean = false, expressionMapper: (Expression) -> Expression): Expression {
    return call1("map", FunctionExpression.create(1) { expr -> expressionMapper(expr.first()) }, optional = optional)
}

fun Expression.callTransform(
    optional: Boolean = false,
    expressionMapperLeft: (Expression) -> Expression,
    expressionMapperRight: (Expression) -> Expression,
): Expression {
    return callWithNArgumentExpressions(
        "transform",
        optional = optional,
        FunctionExpression.create(1) { expr -> expressionMapperLeft(expr.first()) },
        FunctionExpression.create(1) { expr -> expressionMapperRight(expr.first()) },
    )
}

fun Expression.callLet(optional: Boolean = false, expressionMapper: (Expression) -> Expression): Expression {
    return call1("let", FunctionExpression.create(1) { expr -> expressionMapper(expr.first()) }, optional = optional)
}

fun Expression.callApplyValue(optional: Boolean = false, expressionMapper: (Expression) -> Expression): Expression {
    return call1(
        "applyValue",
        FunctionExpression.create(1) { expr -> expressionMapper(expr.first()) },
        optional = optional,
    )
}

fun Expression.call1(name: String, expression: Expression, optional: Boolean = false): Expression {
    val optionalString = if (optional) "?" else ""
    return (CustomExpressionBuilder.start() + this + "$optionalString." + name + "(" + expression + ")").build()
}

fun Expression.callWithNArgumentExpressions(
    functionNameToCall: String,
    optional: Boolean = false,
    vararg expressions: Expression,
): Expression {
    val optionalString = if (optional) "?" else ""
    val joinedExpression = expressions.map { it.toCodeBlock() }
        .map { it.toKotlinPoetCodeBlock() }
        .joinToString(separator = ", ")
    return (
        CustomExpressionBuilder.start() +
            this + "$optionalString." + functionNameToCall + "(" + joinedExpression + ")"
        ).build()
}

fun Expression.ofLeft(firstType: TypeName, secondType: ClassName) =
    (CustomExpressionBuilder.start("Either.ofLeft<%T, %T>(", firstType, secondType) + this + ")").build()

fun Expression.ofRight(firstType: ClassName, secondType: TypeName) =
    (CustomExpressionBuilder.start("Either.ofRight<%T, %T>(", firstType, secondType) + this + ")").build()

data class FunctionExpression(val argumentNames: List<String>, val expression: Expression) : Expression() {
    override fun toCodeBlock(): CustomCodeBlock {
        return expression.toCodeBlock()
    }

    companion object {
        fun create(arguments: Int, mapper: (List<Expression>) -> Expression): FunctionExpression {
            val argNames = (0 until arguments)
                .map { "args$it" }

            val argExprs = argNames.map { CustomExpression(it) }

            val expression =
                CustomExpressionBuilder.start("{") + argNames.joinToString(", ") + " -> " + mapper(argExprs) + "}"

            return FunctionExpression(argNames, expression.build())
        }
    }
}

fun FunSpec.Builder.addCode(code: Code): FunSpec.Builder {
    return this.addCode(code.toCodeBlock().toKotlinPoetCodeBlock())
}

data class ConstructObjectExpression(val typeName: TypeName, val fields: Map<String, Expression>) : Expression() {
    override fun toCodeBlock(): CustomCodeBlock {
        val fieldsBuilder = fields
            .map { (name, value) ->
                CustomExpressionBuilder.start("%N", name) + "=" + value
            }
            .reduceOrNull { left, right -> left + ",\n" + right }
            ?: CustomExpressionBuilder.start()

        val builder = CustomExpressionBuilder.start("%T", typeName) + "(\n" + fieldsBuilder + "\n)"
        return builder.build().toCodeBlock()
    }
}

data class CustomCodeBlock(val text: String, val args: List<Any>) {
    @SuppressWarnings("TooGenericExceptionCaught")
    fun toKotlinPoetCodeBlock(): CodeBlock {
        try {
            return CodeBlock.of(text, *args.toTypedArray())
        } catch (e: Exception) {
            throw CodeBlockCreationException(text, args, e)
        }
    }
}

class CodeBlockCreationException(text: String, args: List<Any>, cause: java.lang.Exception) :
    RuntimeException(
        "Failed to create Kotlin Poet code block.\n" +
            "Text: $text\n" +
            "Args: $args",
        cause,
    )

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
        return CustomCodeBlock(text, args)
    }
}

fun Iterable<CustomCodeBlock>.merge(separator: String): CustomCodeBlock {
    return CustomCodeBlock(
        joinToString(separator) { it.text },
        flatMap { it.args },
    )
}

data class Assignment(val to: String, val expression: Expression) : Code() {
    override fun toCodeBlock(): CustomCodeBlock {
        return CustomCodeBlock("val %N = " + expression.toCodeBlock().text, listOf(to) + expression.toCodeBlock().args)
    }

    fun reference(): VariableReference = VariableReference(to)
}

data class GroupedCode(val list: List<Code>) : Code() {
    override fun toCodeBlock(): CustomCodeBlock {
        val blocks = list.map { it.toCodeBlock() }

        val text = blocks.joinToString("\n") { it.text }
        val args = blocks.flatMap { it.args }

        return CustomCodeBlock(text, args)
    }
}

data class VariableReference(val name: String) : Expression() {
    override fun toCodeBlock(): CustomCodeBlock {
        return CustomCodeBlock("%N", listOf(name))
    }
}

operator fun MemberName.invoke(vararg expression: Expression): Expression {
    val builder = CustomExpressionBuilder.start("%M", this) + "(" + expression.map { it.toCodeBlock() }.merge(",") + ")"
    return builder.build()
}

fun CodeBlock.Builder.add(code: Code): CodeBlock.Builder =
    add(code.toCodeBlock().toKotlinPoetCodeBlock())
