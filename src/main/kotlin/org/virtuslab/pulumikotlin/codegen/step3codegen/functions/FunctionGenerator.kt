package org.virtuslab.pulumikotlin.codegen.step3codegen.functions

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import org.virtuslab.pulumikotlin.codegen.expressions.Assignment
import org.virtuslab.pulumikotlin.codegen.expressions.ConstructObjectExpression
import org.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import org.virtuslab.pulumikotlin.codegen.expressions.Expression
import org.virtuslab.pulumikotlin.codegen.expressions.GroupedCode
import org.virtuslab.pulumikotlin.codegen.expressions.Return
import org.virtuslab.pulumikotlin.codegen.expressions.addCode
import org.virtuslab.pulumikotlin.codegen.expressions.call0
import org.virtuslab.pulumikotlin.codegen.expressions.invoke
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import org.virtuslab.pulumikotlin.codegen.step2intermediate.FunctionType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.coroutinesFutureAwaitExtensionMethod
import org.virtuslab.pulumikotlin.codegen.step2intermediate.NamingFlags
import org.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetExtensions.addImport
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addStandardSuppressAnnotations
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderLambda
import org.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver
import org.virtuslab.pulumikotlin.codegen.step3codegen.addDeprecationWarningIfAvailable
import org.virtuslab.pulumikotlin.codegen.step3codegen.addDocs
import org.virtuslab.pulumikotlin.codegen.utils.letIf

object FunctionGenerator {
    fun generateFunctions(functions: List<FunctionType>, typeNameClashResolver: TypeNameClashResolver): List<FileSpec> {
        val namingFlags = NamingFlags(Root, Function, Output, Kotlin)
        val files = functions
            .groupBy { it.name.namespace }
            .map { (_, types) ->
                val firstType = types.first()
                val name = firstType.name

                val objectSpecBuilder = TypeSpec.objectBuilder(name.toFunctionGroupObjectName(namingFlags))

                val functionSpecs = types.flatMap { generateFunctionSpec(it, typeNameClashResolver) }
                functionSpecs.forEach {
                    objectSpecBuilder.addFunction(it)
                }

                val fileSpec = FileSpec
                    .builder(
                        name.toFunctionGroupObjectPackage(namingFlags),
                        name.toFunctionGroupObjectName(namingFlags),
                    )
                    .addType(objectSpecBuilder.build())
                    .addImport(coroutinesFutureAwaitExtensionMethod())
                    .addStandardSuppressAnnotations()
                    .build()

                fileSpec
            }

        return files
    }

    private fun callAwaitAndDoTheMapping(
        functionType: FunctionType,
        argument: Expression?,
        typeNameClashResolver: TypeNameClashResolver,
    ): Return {
        val javaNamingFlags = NamingFlags(Root, Function, Input, Java)

        val toKotlin = typeNameClashResolver.kotlinNames(functionType.outputType.metadata)
            .kotlinPoetClassName
            .nestedClass("Companion")
            .member("toKotlin")
        val javaMethodGetName = ClassName(
            functionType.name.toFunctionGroupObjectPackage(javaNamingFlags),
            functionType.name.toFunctionGroupObjectName(javaNamingFlags),
        )
            .member(functionType.name.toFunctionName(javaNamingFlags))

        val calledJavaMethod = if (argument == null) {
            javaMethodGetName()
        } else {
            javaMethodGetName(argument.call0("toJava"))
        }
        return Return(toKotlin(calledJavaMethod.call0("await")))
    }

    private fun generateFunctionSpec(
        functionType: FunctionType,
        typeNameClashResolver: TypeNameClashResolver,
    ): List<FunSpec> {
        val hasAnyArguments = (functionType.argsType as? ComplexType)?.fields?.isNotEmpty() ?: true

        val functionDocs = functionType.kDoc.description.orEmpty()
        val returnDoc = "@return ${functionType.outputType.metadata.kDoc.description}\n"

        val basicFunSpec = FunSpec.builder(functionType.name.name)
            .letIf(hasAnyArguments) {
                it.addParameter(
                    "argument",
                    typeNameClashResolver.kotlinNames(functionType.argsType.metadata).kotlinPoetClassName,
                )
            }
            .addModifiers(KModifier.SUSPEND)
            .returns(typeNameClashResolver.kotlinNames(functionType.outputType.metadata).kotlinPoetClassName)
            .let {
                val argumentExpression = if (hasAnyArguments) {
                    CustomExpression("argument")
                } else {
                    null
                }
                it.addCode(callAwaitAndDoTheMapping(functionType, argumentExpression, typeNameClashResolver))
            }
            .addDocs(functionDocs, "@param argument ${functionType.argsType.metadata.kDoc.description}", returnDoc)
            .addDeprecationWarningIfAvailable(functionType.kDoc)
            .build()

        if (!hasAnyArguments) {
            return listOf(basicFunSpec)
        }

        val paramDocs = (functionType.argsType as? ComplexType)
            ?.fields
            ?.map { "@param ${it.key} ${it.value.kDoc.description.orEmpty()}" }
            ?.joinToString("\n")
            .orEmpty()
        val separateArgumentsOverloadFunSpec = (functionType.argsType as? ComplexType)
            ?.fields
            ?.let { parameters ->
                FunSpec.builder(functionType.name.name)
                    .addParameters(
                        parameters.map { (name, type) ->
                            ParameterSpec.builder(
                                name,
                                typeNameClashResolver.toTypeName(type.type, Kotlin)
                                    .copy(nullable = type.type is OptionalType),
                            )
                                .letIf(type.type is OptionalType) {
                                    it.defaultValue("null")
                                }
                                .build()
                        },
                    )
                    .addModifiers(KModifier.SUSPEND)
                    .returns(
                        typeNameClashResolver.kotlinNames(functionType.outputType.metadata).kotlinPoetClassName,
                    )
                    .let {
                        val assignment = Assignment(
                            "argument",
                            ConstructObjectExpression(
                                typeNameClashResolver.kotlinNames(functionType.argsType.metadata).kotlinPoetClassName,
                                parameters.map { (name, _) -> name to CustomExpression("%N", name) }.toMap(),
                            ),
                        )
                        val returnCode =
                            callAwaitAndDoTheMapping(functionType, assignment.reference(), typeNameClashResolver)

                        it.addCode(
                            GroupedCode(
                                listOf(
                                    assignment,
                                    returnCode,
                                ),
                            ),
                        )
                    }
            }
            ?.addDocs("@see [${functionType.name.name}].", paramDocs, returnDoc)
            ?.addDeprecationWarningIfAvailable(functionType.kDoc)
            ?.build()

        val typeSafeBuilderOverloadFunSpec = (functionType.argsType as? ComplexType)?.let { args ->
            val argsTypeName = typeNameClashResolver.kotlinNames(args.metadata)
            FunSpec.builder(functionType.name.name)
                .addParameter("argument", builderLambda(args.toBuilderTypeName()))
                .addModifiers(KModifier.SUSPEND)
                .returns(
                    typeNameClashResolver.kotlinNames(functionType.outputType.metadata).kotlinPoetClassName,
                )
                .let { builder ->
                    val builderAssignment = Assignment(
                        "builder",
                        ConstructObjectExpression(args.toBuilderTypeName(), emptyMap()),
                    )
                    val callArgument = builderAssignment.reference().call0("argument")
                    val builtArgumentAssignment = Assignment(
                        "builtArgument",
                        CustomExpression("builder").call0("build"),
                    )
                    val returnArgument = callAwaitAndDoTheMapping(
                        functionType,
                        builtArgumentAssignment.reference(),
                        typeNameClashResolver,
                    )

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
                .addDocs(
                    "@see [${functionType.name.name}].",
                    "@param argument Builder for [${argsTypeName.kotlinPoetClassName}].",
                    returnDoc,
                )
                .addDeprecationWarningIfAvailable(functionType.kDoc)
        }
            ?.build()

        return listOfNotNull(basicFunSpec, separateArgumentsOverloadFunSpec, typeSafeBuilderOverloadFunSpec)
    }

    private fun FunSpec.Builder.addDocs(
        functionDocs: String,
        paramDocs: String,
        returnDocs: String,
    ) = apply {
        addDocs("$functionDocs\n$paramDocs\n$returnDocs")
    }
}
