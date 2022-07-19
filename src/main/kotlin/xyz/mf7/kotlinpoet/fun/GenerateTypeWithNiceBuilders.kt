package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

data class FieldOverload(
    val type: TypeName,
    val mappingCode: (from: String, to: String) -> CodeBlock
)

data class Field(val name: String, val type: TypeName, val required: Boolean, val overloads: List<FieldOverload>)

fun generateTypeWithNiceBuilders(
    fileName: String,
    packageName: String,
    mainClassName: String,
    builderClassName: String,

    builderMethodName: String,
    builderPropertyToSet: String,
    receiverClassNameForBuilderMethod: ClassName,

    fields: List<Field>
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
        val typeName = field.type.copy(nullable = !isRequired)
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
                    .builder(it.name, it.type.copy(nullable = !it.required))
                    .initializer("null")
                    .mutable(true)
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            }
        )
        .addFunctions(
            fields.flatMap { field ->
                generateFunctionsForInput2(field)
            }
        )
        .addFunction(
            FunSpec.builder("build")
                .returns(argsClassName)
                .addCode("return %T(${argNames})", argsClassName)
                .build()
        )
        .build()

    val builderMethod = FunSpec
        .builder(builderMethodName)
        .receiver(receiverClassNameForBuilderMethod)
        .addModifiers(KModifier.SUSPEND)
        .addParameter(
            "block", LambdaTypeName.get(
                argsBuilderClassName,
                returnType = UNIT
            ).copy(suspending = true)
        )
        .addStatement("val builder = %T()", argsBuilderClassName)
        .addStatement("block(builder)")
        .addStatement("this.%N = builder.build()", builderPropertyToSet)
        .build()

    fileSpec
        .addType(argsBuilderClass)
        .addType(argsClass)
        .addFunction(builderMethod)

    return fileSpec.build()
}

private fun generateFunctionsForInput2(field: Field): List<FunSpec> {
    val fieldType = field.type

    return buildList {
        add(
            FunSpec
                .builder(field.name)
                .addParameter("value", fieldType.copy(nullable = !field.required))
                .addCode("this.${field.name} = value")
                .build()
        )
        field.overloads.forEach {
            add(
                FunSpec
                    .builder(field.name)
                    .addParameter("value", it.type.copy(nullable = !field.required))
                    .addCode("if(%N != null) {", "value")
                    .addCode(it.mappingCode("value", "mapped"))
                    .addCode("\n")
                    .addCode("this.${field.name} = mapped")
                    .addCode("}")
                    .addCode("else {")
                    .addCode("this.${field.name} = null")
                    .addCode("}")
                    .build()
            )
        }
        if (fieldType is ParameterizedTypeName) {
            val typeArguments = fieldType.typeArguments
            when (fieldType.rawType) {
                LIST ->
                    add(
                        FunSpec
                            .builder(field.name)
                            .addParameter("values", typeArguments[0], KModifier.VARARG)
                            .addCode(
                                "this.${field.name} = values.toList()",
                            )
                            .build()
                    )
                MAP ->
                    add(
                        FunSpec
                            .builder(field.name)
                            .addParameter(
                                "values",
                                ClassName("kotlin", "Pair").parameterizedBy(
                                    typeArguments[0],
                                    typeArguments[1]
                                ),
                                KModifier.VARARG
                            )
                            .addCode(
                                "this.${field.name} = values.toList().toMap()"
                            )
                            .build()
                    )
            }
        }
        field.overloads.forEach {
            val fieldType = it.type
            if (fieldType is ParameterizedTypeName) {
                val typeArguments = fieldType.typeArguments
                when (fieldType.rawType) {
                    LIST ->
                        add(
                            FunSpec
                                .builder(field.name)
                                .addParameter("values", typeArguments[0], KModifier.VARARG)
                                .addCode("val list = values.toList()",)
                                .addCode(it.mappingCode("list", "mapped"))
                                .addCode("this.${field.name} = mapped")
                                .build()
                        )
                    MAP ->
                        add(
                            FunSpec
                                .builder(field.name)
                                .addParameter(
                                    "values",
                                    ClassName("kotlin", "Pair").parameterizedBy(
                                        typeArguments[0],
                                        typeArguments[1]
                                    ),
                                    KModifier.VARARG
                                )
                                .addCode("val list = values.toList().toMap()",)
                                .addCode(it.mappingCode("list", "mapped"))
                                .addCode("this.${field.name} = mapped")
                                .build()
                        )
                }
            }
        }
    }
}
//
//fun generateBuilderWrapperFunction(): FunSpec {
//
//}