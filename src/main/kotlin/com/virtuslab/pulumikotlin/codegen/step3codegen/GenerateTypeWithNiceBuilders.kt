package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.virtuslab.pulumikotlin.codegen.step2intermediate.*
import com.virtuslab.pulumikotlin.codegen.utils.letIf
import java.util.Random
import kotlin.streams.asSequence

typealias MappingCode = (from: String, to: String) -> CodeBlock

sealed class FieldType<T : Type> {
    abstract fun toTypeName(): TypeName

    abstract val type: T
}

data class NormalField<T : Type>(override val type: T, val mappingCode: MappingCode) : FieldType<T>() {
    override fun toTypeName(): TypeName {
        return type.toTypeName()
    }
}

data class OutputWrappedField<T : Type>(override val type: T) : FieldType<T>() {
    override fun toTypeName(): TypeName {
        return MoreTypes.Java.Pulumi.Output(type.toTypeName())
    }
}

sealed class Code

data class Expression(val text: String, val args: List<Any>) {
    constructor(text: String, vararg args: Any) : this(text, args.toList())

    operator fun invoke(text: String, vararg args: Any): Expression {
        return copy(text = this.text + text, args = this.args + args)
    }

    operator fun invoke(expression: Expression): Expression {
        return copy(text = this.text + expression.text, args = this.args + expression.args)
    }

    fun toCodeBlock(): CodeBlock {
        return try {
            CodeBlock.of(text, *args.toTypedArray())
        } catch (e: Exception) {
            println("Text" + text)
            println("Args" + args)
            throw e
        }
    }
}

data class Assignment(val to: String, val expression: Expression)

operator fun MemberName.invoke(vararg expression: Expression): Expression {
    return Expression(
        "%M(" + expression.joinToString(", ", transform = { it.text }) + ")",
        listOf(this) + expression.flatMap { it.args }
    )
}

fun Expression.`fun`(name: String, expression: Expression): Expression {
    return Expression(".${name}(")(expression)(")")
}

fun Expression.invokeOneArgLikeMap(name: String, expression: Expression, opt: Boolean = false): Expression {
    val optional = if(opt) { "?" } else { "" }
    return this(Expression("${optional}.${name}{ arg -> ")(expression)("}"))
}

fun Expression.`invokeZeroArgs`(name: String, opt: Boolean = false): Expression {
    val optional = if(opt) { "?" } else { "" }
    return this(Expression("${optional}.${name}()"))
}


data class Field<T : Type>(
    val name: String,
    val fieldType: FieldType<T>,
    val required: Boolean,
    val overloads: List<FieldType<*>>
)

fun toKotlinIfOptional(field: Field<*>): String {
    return if (!field.required) {
        ".toKotlin()"
    } else {
        ""
    }
}

fun ToKotlinIfOptional(expression: Expression, required: Boolean): Expression {
    return if (!required) {
        expression(".toKotlin()")
    } else {
        expression
    }
}

fun toKotlinExpression(expression: Expression, type: Type): Expression {
    return when (val type = type) {
        AnyType -> expression
        is ComplexType -> type.toTypeName().member("toKotlin")(expression)
        is EnumType -> type.toTypeName().member("toKotlin")(expression)
        is EitherType -> expression
        is ListType -> expression.invokeOneArgLikeMap("map", toKotlinExpression(Expression("arg"), type.innerType))
        is MapType -> expression.invokeOneArgLikeMap(
            "map",
            Expression("arg.key")(" to ")(toKotlinExpression(Expression("arg.value"), type.secondType))
        ).invokeZeroArgs("toMap")

        is PrimitiveType -> expression
    }
}

fun toKotlinExpressionBase(name: String): Expression {
    return Expression("javaType.%N().toKotlin()!!", KeywordsEscaper.escape(name))
}

fun toKotlinFunction(typeMetadata: TypeMetadata, fields: List<Field<*>>): FunSpec {

    val codeBlocks = fields.map { field ->
        val type = field.fieldType.type

        val baseE = toKotlinExpressionBase(field.name)
        val firstPart = Expression("%N = ", field.name)

        val secondPart = baseE.invokeOneArgLikeMap("applyValue", toKotlinExpression(Expression("arg"), type))

        firstPart(secondPart).toCodeBlock()
    }

    val names = typeMetadata.names(LanguageType.Kotlin)
    val kotlinArgsClass = ClassName(names.packageName, names.className)

    val javaNames = typeMetadata.names(LanguageType.Java)
    val javaArgsClass = ClassName(javaNames.packageName, javaNames.className)

    return FunSpec.builder("toKotlin")
        .returns(kotlinArgsClass)
        .addParameter("javaType", javaArgsClass)
        .addCode(CodeBlock.of("return %T(", kotlinArgsClass))
        .apply {
            codeBlocks.forEach { block ->
                addCode(block)
                addCode(",")
            }
        }
        .addCode(CodeBlock.of(")"))
        .build()
}

fun toJavaFunction(typeMetadata: TypeMetadata, fields: List<Field<*>>): FunSpec {

    val codeBlocks = fields.map { field ->
        val block = CodeBlock.of(
            ".%N(%N)", KeywordsEscaper.escape(field.name), field.name
        )
        val toJavaBlock = CodeBlock.of(".%N(%N.%N())", KeywordsEscaper.escape(field.name), field.name, "toJava")
        when (val type = field.fieldType.type) {
            AnyType -> block
            is PrimitiveType -> block
            is ComplexType -> toJavaBlock
            is EnumType -> toJavaBlock
            is EitherType -> toJavaBlock
            is ListType -> toJavaBlock
            is MapType -> toJavaBlock
        }

    }

    val names = typeMetadata.names(LanguageType.Java)
    val javaArgsClass = ClassName(names.packageName, names.className)

    return FunSpec.builder("toJava")
        .returns(javaArgsClass)
        .addModifiers(OVERRIDE)
        .addCode(CodeBlock.of("return %T.%M()", javaArgsClass, javaArgsClass.member("builder")))
        .apply {
            codeBlocks.forEach { block ->
                addCode(block)
            }
        }
        .addCode(CodeBlock.of(".build()"))
        .build()
}

data class GenerationOptions(
    val shouldGenerateBuilders: Boolean = true,
    val implementToJava: Boolean = true,
    val implementToKotlin: Boolean = true
)

fun generateTypeWithNiceBuilders(
    typeMetadata: TypeMetadata,
    fields: List<Field<*>>,
    options: GenerationOptions = GenerationOptions()
): FileSpec {

    val names = typeMetadata.names(LanguageType.Kotlin)

    val fileSpec = FileSpec.builder(
        names.packageName, names.className + ".kt"
    )

    val argsClassName = ClassName(names.packageName, names.className)

    val classB = TypeSpec.classBuilder(argsClassName)
        .letIf(options.implementToJava) {
            it.addSuperinterface(MoreTypes.Kotlin.Pulumi.ConvertibleToJava(typeMetadata.names(LanguageType.Java).kotlinPoetClassName))
        }
        .addModifiers(KModifier.DATA)

    val constructor = FunSpec.constructorBuilder()

    fields.forEach { field ->
        val isRequired = field.required
        val typeName = field.fieldType.toTypeName().copy(nullable = !isRequired)
        classB
            .addProperty(
                PropertySpec.builder(field.name, typeName)
                    .initializer(field.name)
                    .build()
            )

        constructor
            .addParameter(
                ParameterSpec.builder(field.name, typeName)
                    .let {
                        if (!isRequired) {
                            it.defaultValue("%L", null)
                        } else {
                            it
                        }
                    }
                    .build()
            )
    }

    classB.primaryConstructor(constructor.build())

    classB.letIf(options.implementToJava) {
        it.addFunction(toJavaFunction(typeMetadata, fields))
    }
    classB.letIf(options.implementToKotlin) {
        it.addType(
            TypeSpec.companionObjectBuilder()
                .addFunction(toKotlinFunction(typeMetadata, fields))
                .build()
        )
    }
    val argsClass = classB.build()

    val argsBuilderClassName = ClassName(names.packageName, names.builderClassName)

    val argNames = fields.map {
        "${it.name} = ${it.name}!!"
    }.joinToString(", ")

    val argsBuilderClass = TypeSpec
        .classBuilder(argsBuilderClassName)
//        .addAnnotation(dslTag)
        .addProperties(
            fields.map {
                PropertySpec
                    .builder(it.name, it.fieldType.toTypeName().copy(nullable = true))
                    .initializer("null")
                    .mutable(true)
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            }
        )
        .addFunctions(
            fields.flatMap { field ->
                generateFunctionsForInput(field)
            }
        )
        .addFunction(
            FunSpec.builder("build")
                .returns(argsClassName)
                .addCode("return %T(${argNames})", argsClassName)
                .build()
        )
        .build()

    fileSpec
        .addImport("com.pulumi.kotlin", "applySuspend")
        .addImport("com.pulumi.kotlin", "applyValue")
        .addImport("com.pulumi.kotlin", "toJava")
        .addImport("com.pulumi.kotlin", "toKotlin")
        .let {
            if (options.shouldGenerateBuilders) {
                it.addType(argsBuilderClass)
            } else {
                it
            }
        }
        .addType(argsClass)

    return fileSpec.build()
}

fun mappingCodeBlock(mappingCode: MappingCode, name: String, code: String, vararg args: Any?): CodeBlock {
    return CodeBlock.builder()
        .addStatement("val toBeMapped = $code", *args)
        .add(mappingCode("toBeMapped", "mapped"))
        .addStatement("")
        .addStatement("this.${name} = mapped")
        .build()
}

fun listOfLambdas(innerType: TypeName): TypeName {
    return LIST.parameterizedBy(builderLambda(innerType))
}

fun listOfLambdas(innerType: ComplexType): TypeName {
    return listOfLambdas(innerType.toBuilderTypeName())
}

fun builderLambda(innerType: ComplexType): TypeName = builderLambda(innerType.toBuilderTypeName())
fun builderLambda(innerType: TypeName): TypeName {
    return LambdaTypeName
        .get(receiver = innerType, returnType = UNIT)
        .copy(suspending = true)
}

data class BuilderSettingCodeBlock(val mappingCode: MappingCode? = null, val code: String, val args: List<Any?>) {

    fun withMappingCode(mappingCode: MappingCode): BuilderSettingCodeBlock {
        return copy(mappingCode = mappingCode)
    }

    fun toCodeBlock(fieldToSetName: String): CodeBlock {
        val mc = mappingCode
        return if (mc != null) {
            CodeBlock.builder()
                .addStatement("val toBeMapped = $code", *args.toTypedArray())
                .add(mc("toBeMapped", "mapped"))
                .addStatement("")
                .addStatement("this.${fieldToSetName} = mapped")
                .build()
        } else {
            CodeBlock.builder()
                .addStatement("this.${fieldToSetName} = $code", *args.toTypedArray())
                .build()
        }
    }

    companion object {
        fun create(code: String, vararg args: Any?) = BuilderSettingCodeBlock(null, code, args.toList())
    }
}

fun builderPattern(
    name: String,
    parameterType: TypeName,
    codeBlock: BuilderSettingCodeBlock,
    parameterModifiers: List<KModifier> = emptyList()
): FunSpec {
    return FunSpec
        .builder(name)
        .addModifiers(SUSPEND)
//        .preventJvmPlatformNameClash()
        .addParameter(
            "argument",
            parameterType,
            parameterModifiers
        )
        .addCode(codeBlock.toCodeBlock(name))
        .build()
}

private fun FunSpec.Builder.preventJvmPlatformNameClash(): FunSpec.Builder {
    return addAnnotation(AnnotationSpec.builder(JvmName::class).addMember("%S", randomStringWith16Characters()).build())
}

private fun randomStringWith16Characters() =
    Random().ints('a'.code, 'z'.code).asSequence().map { it.toChar() }.take(16).joinToString("")

private fun specialMethodsForComplexType(
    name: String,
    field: NormalField<ComplexType>,
): List<FunSpec> {
    val builderTypeName = field.type.toBuilderTypeName()
    return listOf(
        builderPattern(
            name,
            builderLambda(builderTypeName),
            BuilderSettingCodeBlock.create("%T().applySuspend{ argument() }.build()", builderTypeName)
                .withMappingCode(field.mappingCode),
        )
    )
}

private fun specialMethodsForList(
    name: String,
    field: NormalField<ListType>,
): List<FunSpec> {
    val innerType = field.type.innerType
    val builderPattern = when (innerType) {
        is ComplexType -> {
            val commonCodeBlock = BuilderSettingCodeBlock
                .create(
                    "argument.toList().map { %T().applySuspend{ it() }.build() }",
                    innerType.toBuilderTypeName()
                )
                .withMappingCode(field.mappingCode)

            listOf(
                builderPattern(name, listOfLambdas(innerType), commonCodeBlock),
                builderPattern(name, builderLambda(innerType), commonCodeBlock, parameterModifiers = listOf(VARARG))
            )
        }

        else -> emptyList()
    }

    val justValuesPassedAsVarargArguments = listOf(
        FunSpec
            .builder(name)
            .addModifiers(SUSPEND)
            .addParameter("values", innerType.toTypeName(), VARARG)
            .addCode(mappingCodeBlock(field.mappingCode, name, "values.toList()"))
            .build()
    )

    return builderPattern + justValuesPassedAsVarargArguments
}

private fun specialMethodsForMap(
    name: String,
    field: NormalField<MapType>,
): List<FunSpec> {
    val leftInnerType = field.type.firstType
    val rightInnerType = field.type.secondType

    val builderPattern = when (rightInnerType) {
        is ComplexType -> {
            val commonCodeBlock = BuilderSettingCodeBlock
                .create(
                    "argument.toList().map { (left, right) -> left to %T().applySuspend{ right() }.build() }",
                    rightInnerType.toBuilderTypeName()
                )
                .withMappingCode(field.mappingCode)

            listOf(
                builderPattern(
                    name,
                    MoreTypes.Kotlin.Pair(leftInnerType.toTypeName(), builderLambda(rightInnerType)),
                    commonCodeBlock,
                    parameterModifiers = listOf(VARARG)
                )
            )
        }

        else -> emptyList()
    }

    val justValuesPassedAsVarargArguments = listOf(
        FunSpec
            .builder(name)
            .addParameter(
                "values",
                MoreTypes.Kotlin.Pair(leftInnerType.toTypeName(), rightInnerType.toTypeName()),
                VARARG
            )
            .addCode(mappingCodeBlock(field.mappingCode, name, "values.toMap()"))
            .build()
    )

    return builderPattern + justValuesPassedAsVarargArguments
}

//fun whatever(): FunSpec {
//    return FunSpec
//        .builder(field.name)
//        .addParameter("value", it.type.copy(nullable = !field.required))
//        .addCode("if(%N != null) {", "value")
//        .addCode(it.mappingCode("value", "mapped"))
//        .addCode("\n")
//        .addCode("this.${field.name} = mapped")
//        .addCode("}")
//        .addCode("else {")
//        .addCode("this.${field.name} = null")
//        .addCode("}")
//        .build()
//}

private fun generateFunctionsForInput(field: Field<*>): List<FunSpec> {
    val functionsForDefaultField = generateFunctionsForInput2(field.name, field.required, field.fieldType)

    val functionsForOverloads = field.overloads.flatMap {
        generateFunctionsForInput2(field.name, field.required, it)
    }

    val allFunctions = functionsForDefaultField + functionsForOverloads

    return allFunctions.map {
        it
            .toBuilder()
            .preventJvmPlatformNameClash()
            .build()
    }
}

private fun generateFunctionsForInput2(name: String, required: Boolean, fieldType: FieldType<*>): List<FunSpec> {
    val functions = when (fieldType) {
        is NormalField -> {
            val basicFunction =
                FunSpec
                    .builder(name)
                    .addModifiers(SUSPEND)
//                    .preventJvmPlatformNameClash()
                    .addParameter("value", fieldType.toTypeName().copy(nullable = !required))
                    .addCode(mappingCodeBlock(fieldType.mappingCode, name, "value"))
                    .build()

            val otherFunctions = when (fieldType.type) {
                is ComplexType -> specialMethodsForComplexType(name, fieldType as NormalField<ComplexType>)
                is ListType -> specialMethodsForList(name, fieldType as NormalField<ListType>)
                is MapType -> specialMethodsForMap(name, fieldType as NormalField<MapType>)
                is PrimitiveType -> listOf()
                is EitherType -> listOf()
                else -> listOf()
            }

            otherFunctions + basicFunction
        }

        is OutputWrappedField -> listOf(
            FunSpec
                .builder(name)
                .addModifiers(SUSPEND)
//                .preventJvmPlatformNameClash()
                .addParameter("value", fieldType.toTypeName().copy(nullable = !required))
                .addCode("this.${name} = value")
                .build()
        )
    }



    return functions
}
//
//fun generateBuilderWrapperFunction(): FunSpec {
//
//}