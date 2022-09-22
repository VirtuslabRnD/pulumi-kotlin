package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.virtuslab.pulumikotlin.codegen.expressions.Assignment
import com.virtuslab.pulumikotlin.codegen.expressions.Code
import com.virtuslab.pulumikotlin.codegen.expressions.ConstructObjectExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpressionBuilder
import com.virtuslab.pulumikotlin.codegen.expressions.Expression
import com.virtuslab.pulumikotlin.codegen.expressions.FunctionExpression
import com.virtuslab.pulumikotlin.codegen.expressions.Return
import com.virtuslab.pulumikotlin.codegen.expressions.addCode
import com.virtuslab.pulumikotlin.codegen.expressions.call0
import com.virtuslab.pulumikotlin.codegen.expressions.call1
import com.virtuslab.pulumikotlin.codegen.expressions.callLet
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
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.ConvertibleToJava
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Type
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.utils.letIf
import java.util.Random
import kotlin.streams.asSequence

typealias MappingCode = (Expression) -> Expression

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
    override fun toTypeName(): ParameterizedTypeName {
        return MoreTypes.Java.Pulumi.Output(type.toTypeName())
    }
}

data class Field<T : Type>(
    val name: String,
    val fieldType: FieldType<T>,
    val required: Boolean,
    val overloads: List<FieldType<*>>,
)

fun toKotlinExpression(expression: Expression, type: Type): Expression {
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

fun toKotlinExpressionBase(name: String): Expression {
    return CustomExpression("javaType.%N().toKotlin()!!", KeywordsEscaper.escape(name))
}

fun toKotlinFunction(typeMetadata: TypeMetadata, fields: List<Field<*>>): FunSpec {
    val arguments = fields.associate { field ->
        val type = field.fieldType.type

        val baseE = toKotlinExpressionBase(field.name)

        val secondPart =
            baseE.call1("applyValue", FunctionExpression.create(1, { args -> toKotlinExpression(args.get(0), type) }))

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

fun toJavaFunction(typeMetadata: TypeMetadata, fields: List<Field<*>>): FunSpec {
    val codeBlocks = fields.map { field ->
        val block = CodeBlock.of(
            ".%N(%N)",
            KeywordsEscaper.escape(field.name),
            field.name,
        )
        val toJavaBlock = CodeBlock.of(".%N(%N?.%N())", KeywordsEscaper.escape(field.name), field.name, "toJava")
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
            codeBlocks.forEach { block -> addCode(block) }
        }
        .addCode(CodeBlock.of(".build()"))
        .build()
}

data class GenerationOptions(
    val shouldGenerateBuilders: Boolean = true,
    val implementToJava: Boolean = true,
    val implementToKotlin: Boolean = true,
)

fun generateTypeWithNiceBuilders(
    typeMetadata: TypeMetadata,
    fields: List<Field<*>>,
    options: GenerationOptions = GenerationOptions(),
): FileSpec {
    val names = typeMetadata.names(LanguageType.Kotlin)

    val fileSpec = FileSpec.builder(
        names.packageName,
        names.className,
    )

    val argsClassName = ClassName(names.packageName, names.className)

    val dslTag = ClassName("com.pulumi.kotlin", "PulumiTagMarker")

    val classB = TypeSpec.classBuilder(argsClassName)
        .letIf(options.implementToJava) {
            val convertibleToJava = ConvertibleToJava(typeMetadata.names(LanguageType.Java).kotlinPoetClassName)
            it.addSuperinterface(convertibleToJava)
        }
        .addModifiers(KModifier.DATA)

    val constructor = FunSpec.constructorBuilder()

    fields.forEach { field ->
        val isRequired = field.required
        val typeName = field.fieldType.toTypeName().copy(nullable = !isRequired)
        classB.addProperty(
            PropertySpec.builder(field.name, typeName)
                .initializer(field.name)
                .build(),
        )

        constructor.addParameter(
            ParameterSpec.builder(field.name, typeName)
                .letIf(!isRequired) {
                    it.defaultValue("%L", null)
                }
                .build(),
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
                .build(),
        )
    }
    val argsClass = classB.build()

    val argsBuilderClassName = ClassName(names.packageName, names.builderClassName)

    val arguments = fields.associate {
        val requiredPart = if (it.required) "!!" else ""
        it.name to CustomExpressionBuilder.start("%N$requiredPart", it.name).build()
    }

    val argsBuilderClass = TypeSpec
        .classBuilder(argsBuilderClassName)
        .addAnnotation(dslTag)
        .addProperties(
            fields.map {
                PropertySpec
                    .builder(it.name, it.fieldType.toTypeName().copy(nullable = true))
                    .initializer("null")
                    .mutable(true)
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            },
        )
        .addFunctions(
            fields.flatMap { field ->
                generateFunctionsForInput(field)
            },
        )
        .addFunction(
            FunSpec.builder("build")
                .returns(argsClassName)
                .addCode(Return(ConstructObjectExpression(argsClassName, arguments)))
                .build(),
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

fun CodeBlock.Builder.add(code: Code): CodeBlock.Builder {
    return this.add(code.toCodeBlock().toKotlinPoetCodeBlock())
}

private fun mappingCodeBlock(
    field: NormalField<*>,
    required: Boolean,
    name: String,
    code: String,
    vararg args: Any?,
): CodeBlock {
    val expression = CustomExpression("toBeMapped").callLet(!required) { arg -> field.mappingCode(arg) }
    return CodeBlock.builder()
        .addStatement("val toBeMapped = $code", *args)
        .add(Assignment("mapped", expression))
        .addStatement("")
        .addStatement("this.%N = mapped", name)
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
                .add(Assignment("mapped", mc(CustomExpression("toBeMapped"))))
                .addStatement("")
                .addStatement("this.%N = mapped", fieldToSetName)
                .build()
        } else {
            CodeBlock.builder()
                .addStatement("this.%N = $code", fieldToSetName, *args.toTypedArray())
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
    parameterModifiers: List<KModifier> = emptyList(),
): FunSpec {
    return FunSpec
        .builder(name)
        .addModifiers(SUSPEND)
        .addParameter(
            "argument",
            parameterType,
            parameterModifiers,
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
            BuilderSettingCodeBlock
                .create("%T().applySuspend{ argument() }.build()", builderTypeName)
                .withMappingCode(field.mappingCode),
        ),
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
                    innerType.toBuilderTypeName(),
                )
                .withMappingCode(field.mappingCode)

            listOf(
                builderPattern(name, listOfLambdas(innerType), commonCodeBlock),
                builderPattern(name, builderLambda(innerType), commonCodeBlock, parameterModifiers = listOf(VARARG)),
            )
        }

        else -> emptyList()
    }

    val justValuesPassedAsVarargArguments = listOf(
        FunSpec
            .builder(name)
            .addModifiers(SUSPEND)
            .addParameter("values", innerType.toTypeName(), VARARG)
            .addCode(mappingCodeBlock(field, false, name, "values.toList()"))
            .build(),
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
                    rightInnerType.toBuilderTypeName(),
                )
                .withMappingCode(field.mappingCode)

            listOf(
                builderPattern(
                    name,
                    MoreTypes.Kotlin.Pair(leftInnerType.toTypeName(), builderLambda(rightInnerType)),
                    commonCodeBlock,
                    parameterModifiers = listOf(VARARG),
                ),
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
                VARARG,
            )
            .addCode(mappingCodeBlock(field, false, name, "values.toMap()"))
            .build(),
    )

    return builderPattern + justValuesPassedAsVarargArguments
}

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
                    .addParameter("value", fieldType.toTypeName().copy(nullable = !required))
                    .addCode(mappingCodeBlock(fieldType, required, name, "value"))
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
                .addParameter("value", fieldType.toTypeName().copy(nullable = !required))
                .addCode("this.%N = value", name)
                .build(),
        )
    }

    return functions
}
