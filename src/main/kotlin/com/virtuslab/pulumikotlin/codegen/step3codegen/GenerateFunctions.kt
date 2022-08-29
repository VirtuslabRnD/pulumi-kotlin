package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.virtuslab.pulumikotlin.codegen.expressions.*
import com.virtuslab.pulumikotlin.codegen.step2intermediate.*
import com.virtuslab.pulumikotlin.codegen.utils.letIf


fun generateFunctionSpec(functionType: FunctionType): List<FunSpec> {
    fun namingFlags(io: InputOrOutput = InputOrOutput.Input) =
        NamingFlags(io, UseCharacteristic.FunctionRoot, LanguageType.Java)

    val spec = FunSpec.builder(functionType.name.name)
        .addParameter("argument", functionType.argsType.toTypeName())
        .addModifiers(KModifier.SUSPEND)
        .returns(functionType.outputType.toTypeName())
        .addCode(
            CodeBlock.of(
                "return %M(%M(argument.toJava()).await())",
                functionType.outputType.toTypeName().nestedClass("Companion").member("toKotlin"),
                ClassName(
                    functionType.name.toFunctionGroupObjectPackage(namingFlags()),
                    functionType.name.toFunctionGroupObjectName(namingFlags())
                ).member(functionType.name.toFunctionName(namingFlags()))
            )
        )
        .build()

    val spec2 = (functionType.argsType as? ComplexType)
        ?.fields
        ?.let { parameters ->
            FunSpec.builder(functionType.name.name)
                .addParameters(
                    parameters.map { (name, type) ->
                        ParameterSpec.builder(name, type.type.toTypeName().copy(nullable = !type.required))
                            .letIf(!type.required) {
                                it
                                    .defaultValue("null")
                            }
                            .build()
                    }
                )
                .addModifiers(KModifier.SUSPEND)
                .returns(functionType.outputType.toTypeName())
                .let {
                    val assignment = Assignment("argument", ConstructObjectExpression(functionType.argsType.toTypeName(),
                        parameters.map { (name, _) -> name to CustomExpression(name) }.toMap()
                    ))

                    it.addCode(assignment.toCodeBlock().toKotlinPoetCodeBlock())

                    it.addCode("\n")

                    it.addCode(
                        CodeBlock.of(
                            "return %M(%M(argument.toJava()).await())",
                            functionType.outputType.toTypeName().nestedClass("Companion").member("toKotlin"),
                            ClassName(
                                functionType.name.toFunctionGroupObjectPackage(namingFlags()),
                                functionType.name.toFunctionGroupObjectName(namingFlags())
                            ).member(functionType.name.toFunctionName(namingFlags()))
                        )
                    )
                }
                .build()


        }

        return listOfNotNull(
            spec,
            spec2
        )
}

fun generateFunctions(functions: List<FunctionType>): List<FileSpec> {
    val namingFlags = NamingFlags(InputOrOutput.Output, UseCharacteristic.ResourceRoot, LanguageType.Kotlin)
    val files = functions
        .groupBy { it.name.namespace }
        .flatMap { (namespace, types) ->
            val firstType = types.first() ?: return@flatMap emptyList()
            val name = firstType.name
            val fileSpecBuilder = FileSpec
                .builder(
                    name.toFunctionGroupObjectPackage(namingFlags),
                    name.toFunctionGroupObjectName(namingFlags) + ".kt"
                )
                .addImport("kotlinx.coroutines.future", "await")

            val functionSpecs = types.flatMap { generateFunctionSpec(it) }
            functionSpecs.forEach {
                fileSpecBuilder.addFunction(it)
            }

            listOf(fileSpecBuilder.build())
        }

    return files
}
