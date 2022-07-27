package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

typealias MappingCode = (from: String, to: String) -> CodeBlock

sealed class FieldType<T : Type> {
    abstract fun toTypeName(): TypeName
}

data class NormalField<T : Type>(val type: T, val mappingCode: MappingCode) : FieldType<T>() {
    override fun toTypeName(): TypeName {
        return type.toTypeName()
    }
}

data class OutputWrappedField<T : Type>(val type: T) : FieldType<T>() {
    override fun toTypeName(): TypeName {
        return ClassName("com.pulumi.core", "Output").parameterizedBy(type.toTypeName())
    }
}

data class Field<T : Type>(
    val name: String,
    val fieldType: FieldType<T>,
    val required: Boolean,
    val overloads: List<FieldType<*>>
)

fun generateTypeWithNiceBuilders(
    fileName: String,
    packageName: String,
    mainClassName: String,
    builderClassName: String,

    fields: List<Field<*>>
): FileSpec {
    val fileSpec = FileSpec.builder(
        packageName, fileName
    )

    val argsClassName = ClassName(packageName, mainClassName)

    val classB = TypeSpec.classBuilder(argsClassName)
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
    val argsClass = classB.build()

    val argsBuilderClassName = ClassName(packageName, builderClassName)

    val argNames = fields.map {
        "${it.name} = ${it.name}"
    }.joinToString(", ")

    val argsBuilderClass = TypeSpec
        .classBuilder(argsBuilderClassName)
//        .addAnnotation(dslTag)
        .addProperties(
            fields.map {
                PropertySpec
                    .builder(it.name, it.fieldType.toTypeName().copy(nullable = !it.required))
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
        .addType(argsBuilderClass)
        .addType(argsClass)

    return fileSpec.build()
}

fun mappingCodeBlock(mappingCode: MappingCode, name: String, code: String, vararg args: Any?): CodeBlock {
    return CodeBlock.builder()
        .addStatement("val toBeMapped = $code", *args)
        .add(mappingCode("toBeMapped", "mapped"))
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

data class CodeBlock2(val mappingCode: MappingCode? = null, val code: String, val args: List<Any?>) {

    fun withMappingCode(mappingCode: MappingCode): CodeBlock2 {
        return copy(mappingCode = mappingCode)
    }

    fun toCodeBlock(fieldToSetName: String): CodeBlock {
        val mc = mappingCode
        return if (mc != null) {
            CodeBlock.builder()
                .addStatement("val toBeMapped = $code", *args.toTypedArray())
                .add(mc("toBeMapped", "mapped"))
                .addStatement("this.${fieldToSetName} = mapped")
                .build()
        } else {
            CodeBlock.builder()
                .addStatement("this.${fieldToSetName} = $code", *args.toTypedArray())
                .build()
        }
    }

    companion object {
        fun create(code: String, vararg args: Any?) = CodeBlock2(null, code, args.toList())
    }
}

fun builderPattern(
    name: String,
    parameterType: TypeName,
    codeBlock: CodeBlock2,
    parameterModifiers: List<KModifier> = emptyList()
): FunSpec {
    return FunSpec
        .builder(name)
        .addParameter(
            "argument",
            parameterType,
            parameterModifiers
        )
        .addCode(codeBlock.toCodeBlock(name))
        .build()
}

private fun specialMethodsForComplexType(
    name: String,
    field: NormalField<ComplexType>,
): List<FunSpec> {
    val builderTypeName = field.type.toBuilderTypeName()
    return listOf(
        builderPattern(
            name,
            builderLambda(builderTypeName),
            CodeBlock2.create("%T().apply { argument() }.build()", builderTypeName).withMappingCode(field.mappingCode),
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
            val commonCodeBlock = CodeBlock2
                .create(
                    "argument.toList().map { %T().apply { it() }.build() }",
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
            .addParameter("values", innerType.toTypeName(), VARARG)
            .addCode(mappingCodeBlock(field.mappingCode, name, "values.toList()"))
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

    return functionsForDefaultField + functionsForOverloads
}

private fun generateFunctionsForInput2(name: String, required: Boolean, fieldType: FieldType<*>): List<FunSpec> {
    val specialFunctions = when (fieldType) {
        is NormalField -> {
            when (fieldType.type) {
                is ComplexType -> specialMethodsForComplexType(name, fieldType as NormalField<ComplexType>)
                is ListType -> specialMethodsForList(name, fieldType as NormalField<ListType>)
                is MapType -> listOf()
                is PrimitiveType -> listOf()
            }
        }

        is OutputWrappedField -> listOf()
    }

    val basicFunction =
        FunSpec
            .builder(name)
            .addParameter("value", fieldType.toTypeName().copy(nullable = !required))
            .addCode("this.${name} = value")
            .build()

    return specialFunctions + basicFunction
}
//
//fun generateBuilderWrapperFunction(): FunSpec {
//
//}