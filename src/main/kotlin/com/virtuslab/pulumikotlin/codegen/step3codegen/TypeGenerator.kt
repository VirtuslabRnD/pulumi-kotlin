package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.virtuslab.pulumikotlin.codegen.expressions.*
import com.virtuslab.pulumikotlin.codegen.step2intermediate.*
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetTypes.builderLambda
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetTypes.listOfLambdas
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
    val overloads: List<FieldType<*>>
)

object TypeGenerator {
    data class Options(
        val shouldGenerateBuilders: Boolean = true,
        val implementToJava: Boolean = true,
        val implementToKotlin: Boolean = true
    )

    fun generateTypes(
        typeMetadata: TypeMetadata,
        fields: List<Field<*>>,
        options: Options = Options()
    ): FileSpec {

        val names = typeMetadata.names(LanguageType.Kotlin)

        val fileSpec = FileSpec.builder(
            names.packageName, names.className + ".kt"
        )

        val argsClassName = ClassName(names.packageName, names.className)

        val dslTag = ClassName("com.pulumi.kotlin", "PulumiTagMarker")

        val classB = TypeSpec.classBuilder(argsClassName)
            .letIf(options.implementToJava) {
                it.addSuperinterface(MoreTypes.Kotlin.Pulumi.ConvertibleToJava(typeMetadata.names(LanguageType.Java).kotlinPoetClassName))
            }
            .addModifiers(DATA)

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
            val requiredPart = if (it.required) {
                "!!"
            } else {
                ""
            }
            "${it.name} = ${it.name}${requiredPart}"
        }.joinToString(", ")

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

    private fun CodeBlock.Builder.add(code: Code): CodeBlock.Builder {
        return this.add(code.toCodeBlock().toKotlinPoetCodeBlock())
    }

    private fun mappingCodeBlock(field: NormalField<*>, required: Boolean, name: String, code: String, vararg args: Any?): CodeBlock {
        val expression = CustomExpression("toBeMapped").callLet(!required) { arg -> field.mappingCode(arg) }
        return CodeBlock.builder()
            .addStatement("val toBeMapped = $code", *args)
            .add(Assignment("mapped", expression))
            .addStatement("")
            .addStatement("this.${name} = mapped")
            .build()
    }

    private fun builderPattern(
        name: String,
        parameterType: TypeName,
        codeBlock: BuilderSettingCodeBlock,
        parameterModifiers: List<KModifier> = emptyList()
    ): FunSpec {
        return FunSpec
            .builder(name)
            .addModifiers(SUSPEND)
            .addParameter(
                "argument",
                parameterType,
                parameterModifiers
            )
            .addCode(codeBlock.toCodeBlock(name))
            .build()
    }

    private fun preventJvmPlatformNameClash(funSpec: FunSpec): FunSpec {
        val randomJvmNameAnnotation = AnnotationSpec
            .builder(JvmName::class)
            .addMember("%S", randomStringWith16Characters())

        return funSpec
            .toBuilder()
            .addAnnotation(randomJvmNameAnnotation.build())
            .build()
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
                KotlinPoetTypes.builderLambda(builderTypeName),
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
                .addCode(mappingCodeBlock(field, false, name, "values.toList()"))
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
                .addCode(mappingCodeBlock(field, false, name, "values.toMap()"))
                .build()
        )

        return builderPattern + justValuesPassedAsVarargArguments
    }

    private fun generateFunctionsForInput(field: Field<*>): List<FunSpec> {
        val functionsForDefaultField = generateFunctionsForInput2(field.name, field.required, field.fieldType)

        val functionsForOverloads = field.overloads.flatMap {
            generateFunctionsForInput2(field.name, field.required, it)
        }

        val allFunctions = functionsForDefaultField + functionsForOverloads

        return allFunctions.map { preventJvmPlatformNameClash(it) }
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
                    .addCode("this.${name} = value")
                    .build()
            )
        }

        return functions
    }

    private fun toKotlinExpression(expression: Expression, type: Type): Expression {
        return when (val type = type) {
            AnyType -> expression
            is ComplexType -> type.toTypeName().member("toKotlin")(expression)
            is EnumType -> type.toTypeName().member("toKotlin")(expression)
            is EitherType -> expression
            is ListType -> expression.callMap { args -> toKotlinExpression(args, type.innerType) }

            is MapType -> expression
                .callMap {
                        args -> args.field("key").call1("to", toKotlinExpression(args.field("value"), type.secondType))
                }
                .call0("toMap")

            is PrimitiveType -> expression
        }
    }

    private fun toKotlinExpressionBase(name: String): Expression {
        return CustomExpression("javaType.%N().toKotlin()!!", KeywordsEscaper.escape(name))
    }

    private fun toKotlinFunction(typeMetadata: TypeMetadata, fields: List<Field<*>>): FunSpec {

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

    private fun toJavaFunction(typeMetadata: TypeMetadata, fields: List<Field<*>>): FunSpec {

        val codeBlocks = fields.map { field ->
            val block = CodeBlock.of(
                ".%N(%N)", KeywordsEscaper.escape(field.name), field.name
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
                codeBlocks.forEach { block ->
                    addCode(block)
                }
            }
            .addCode(CodeBlock.of(".build()"))
            .build()
    }

    private data class BuilderSettingCodeBlock(val mappingCode: MappingCode? = null, val code: String, val args: List<Any?>) {

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
}
