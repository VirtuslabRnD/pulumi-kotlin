package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.virtuslab.pulumikotlin.codegen.expressions.Assignment
import com.virtuslab.pulumikotlin.codegen.expressions.ConstructObjectExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import com.virtuslab.pulumikotlin.codegen.expressions.Expression
import com.virtuslab.pulumikotlin.codegen.expressions.GroupedCode
import com.virtuslab.pulumikotlin.codegen.expressions.Return
import com.virtuslab.pulumikotlin.codegen.expressions.addCode
import com.virtuslab.pulumikotlin.codegen.expressions.call0
import com.virtuslab.pulumikotlin.codegen.expressions.invoke
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.FunctionType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.NamingFlags
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic
import com.virtuslab.pulumikotlin.codegen.utils.letIf

private fun callAwaitAndDoTheMapping(functionType: FunctionType, argument: Expression?): Return {
    val javaNamingFlags = NamingFlags(InputOrOutput.Input, UseCharacteristic.FunctionRoot, LanguageType.Java)

    val toKotlin = functionType.outputType.toTypeName().nestedClass("Companion").member("toKotlin")
    val javaMethodGetName = ClassName(
        functionType.name.toFunctionGroupObjectPackage(javaNamingFlags),
        functionType.name.toFunctionGroupObjectName(javaNamingFlags),
    ).member(functionType.name.toFunctionName(javaNamingFlags))

    val calledJavaMethod = if (argument == null) {
        javaMethodGetName()
    } else {
        javaMethodGetName(argument.call0("toJava"))
    }
    return Return(toKotlin(calledJavaMethod.call0("await")))
}
fun generateFunctionSpec(functionType: FunctionType): List<FunSpec> {
    val hasAnyArguments = (functionType.argsType as? ComplexType)?.fields?.isNotEmpty() ?: true

    val basicFunSpec = FunSpec.builder(functionType.name.name)
        .letIf(hasAnyArguments) {
            it.addParameter("argument", functionType.argsType.toTypeName())
        }
        .addModifiers(KModifier.SUSPEND)
        .returns(functionType.outputType.toTypeName())
        .let {
            val argumentExpression = if (hasAnyArguments) { CustomExpression("argument") } else { null }
            it.addCode(callAwaitAndDoTheMapping(functionType, argumentExpression))
        }
        .build()

    if (!hasAnyArguments) {
        return listOf(basicFunSpec)
    }

    val separateArgumentsOverloadFunSpec = (functionType.argsType as? ComplexType)
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
                    },
                )
                .addModifiers(KModifier.SUSPEND)
                .returns(functionType.outputType.toTypeName())
                .let {
                    val assignment = Assignment(
                        "argument",
                        ConstructObjectExpression(
                            functionType.argsType.toTypeName(),
                            parameters.map { (name, _) -> name to CustomExpression(name) }.toMap(),
                        ),
                    )
                    val returnCode = callAwaitAndDoTheMapping(functionType, assignment.reference())

                    it.addCode(
                        GroupedCode(
                            listOf(
                                assignment,
                                returnCode,
                            ),
                        ),
                    )
                }
                .build()
        }

    val typeSafeBuilderOverloadFunSpec = (functionType.argsType as? ComplexType)?.let { args ->
        FunSpec.builder(functionType.name.name)
            .addParameter("argument", builderLambda(args.toBuilderTypeName()))
            .addModifiers(KModifier.SUSPEND)
            .returns(functionType.outputType.toTypeName())
            .let { builder ->

                val builderAssignment =
                    Assignment("builder", ConstructObjectExpression(args.toBuilderTypeName(), emptyMap()))
                val callArgument = builderAssignment.reference().call0("argument")
                val builtArgumentAssignment = Assignment("builtArgument", CustomExpression("builder").call0("build"))
                val returnArgument = callAwaitAndDoTheMapping(functionType, builtArgumentAssignment.reference())

                val allCode = GroupedCode(
                    listOf(
                        builderAssignment,
                        callArgument,
                        builtArgumentAssignment,
                        returnArgument,
                    ),
                )

                builder.addCode(allCode)
            }
            .build()
    }

    return listOfNotNull(basicFunSpec, separateArgumentsOverloadFunSpec, typeSafeBuilderOverloadFunSpec)
}

fun generateFunctions(functions: List<FunctionType>): List<FileSpec> {
    val namingFlags = NamingFlags(InputOrOutput.Output, UseCharacteristic.ResourceRoot, LanguageType.Kotlin)
    val files = functions
        .groupBy { it.name.namespace }
        .flatMap { (_, types) ->
            val firstType = types.first()
            val name = firstType.name

            val objectSpecBuilder = TypeSpec.objectBuilder(name.toFunctionGroupObjectName(namingFlags))

            val functionSpecs = types.flatMap { generateFunctionSpec(it) }
            functionSpecs.forEach {
                objectSpecBuilder.addFunction(it)
            }

            val fileSpec = FileSpec
                .builder(
                    name.toFunctionGroupObjectPackage(namingFlags),
                    name.toFunctionGroupObjectName(namingFlags),
                )
                .addType(objectSpecBuilder.build())
                .addImport("kotlinx.coroutines.future", "await")
                .build()

            listOf(fileSpec)
        }

    return files
}
