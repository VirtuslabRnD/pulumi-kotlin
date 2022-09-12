package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.virtuslab.pulumikotlin.codegen.expressions.*
import com.virtuslab.pulumikotlin.codegen.step2intermediate.*
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetTypes.builderLambda
import com.virtuslab.pulumikotlin.codegen.utils.letIf

object FunctionGenerator {
    fun generateFunctions(functions: List<FunctionType>): List<FileSpec> {
        val namingFlags = NamingFlags(InputOrOutput.Output, UseCharacteristic.ResourceRoot, LanguageType.Kotlin)
        val files = functions
            .groupBy { it.name.namespace }
            .flatMap { (namespace, types) ->
                val firstType = types.first() ?: return@flatMap emptyList()
                val name = firstType.name

                val objectSpecBuilder = TypeSpec.objectBuilder(name.toFunctionGroupObjectName(namingFlags))

                val functionSpecs = types.flatMap { generateFunctionSpec(it) }
                functionSpecs.forEach {
                    objectSpecBuilder.addFunction(it)
                }

                val fileSpec = FileSpec
                    .builder(
                        name.toFunctionGroupObjectPackage(namingFlags),
                        name.toFunctionGroupObjectName(namingFlags) + ".kt"
                    )
                    .addType(objectSpecBuilder.build())
                    .addImport("kotlinx.coroutines.future", "await")
                    .build()

                listOf(fileSpec)
            }

        return files
    }

    private fun callAwaitAndDoTheMapping(functionType: FunctionType, argument: Expression): Return {
        val javaNamingFlags = NamingFlags(InputOrOutput.Input, UseCharacteristic.FunctionRoot, LanguageType.Java)

        val toKotlin = functionType.outputType.toTypeName().nestedClass("Companion").member("toKotlin")
        val javaMethodGetName = ClassName(
            functionType.name.toFunctionGroupObjectPackage(javaNamingFlags),
            functionType.name.toFunctionGroupObjectName(javaNamingFlags)
        ).member(functionType.name.toFunctionName(javaNamingFlags))


        return Return(toKotlin(javaMethodGetName(argument.call0("toJava")).call0("await")))
    }

    private fun generateFunctionSpec(functionType: FunctionType): List<FunSpec> {

        val spec = FunSpec.builder(functionType.name.name)
            .addParameter("argument", functionType.argsType.toTypeName())
            .addModifiers(KModifier.SUSPEND)
            .returns(functionType.outputType.toTypeName())
            .addCode(
                callAwaitAndDoTheMapping(functionType, CustomExpression("argument"))
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
                        val returnCode = callAwaitAndDoTheMapping(functionType, assignment.reference())

                        it.addCode(
                            GroupedCode(listOf(
                                assignment,
                                returnCode
                            ))
                        )
                    }
                    .build()
            }

        val spec3 = (functionType.argsType as? ComplexType)?.let { args ->
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

                    val allCode = GroupedCode(listOf(
                        builderAssignment,
                        callArgument,
                        builtArgumentAssignment,
                        returnArgument
                    ))

                    builder.addCode(allCode)
                }
                .build()
        }

        return listOfNotNull(spec, spec2, spec3)
    }
}
